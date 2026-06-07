<template>
  <div class="parts-table">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button size="small" type="primary" @click="handleAddRow">
        <el-icon><Plus /></el-icon>
        添加行
      </el-button>
      <el-button
        size="small"
        :disabled="selectedRows.length === 0"
        @click="handleDeleteRows"
      >
        <el-icon><Delete /></el-icon>
        删除选中行
      </el-button>
      <el-button
        size="small"
        :disabled="!orderId"
        @click="showBatchCopy = true"
      >
        <el-icon><CopyDocument /></el-icon>
        从其他单复制
      </el-button>
      <el-button
        size="small"
        :disabled="selectedRows.length === 0"
        @click="showBatchArea = true"
      >
        <el-icon><Location /></el-icon>
        批量设置库区
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table
      :data="details"
      border
      stripe
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="45" />
      <el-table-column label="行号" width="60">
        <template #default="{ $index }">
          {{ $index + 1 }}
        </template>
      </el-table-column>
      <el-table-column label="物料编码" min-width="150">
        <template #default="{ row }">
          <el-select
            v-model="row.partId"
            filterable
            placeholder="选择物料"
            style="width: 100%"
            @change="(val: number) => onPartChange(val, row)"
          >
            <el-option
              v-for="p in availableParts"
              :key="p.id"
              :label="`${p.code} - ${p.name}`"
              :value="p.id"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="物料名称" min-width="120">
        <template #default="{ row }">
          <el-input :model-value="row.partName" disabled size="small" />
        </template>
      </el-table-column>
      <el-table-column label="单位" width="80">
        <template #default="{ row }">
          <el-input :model-value="row.unit" disabled size="small" />
        </template>
      </el-table-column>
      <el-table-column label="计划数量" width="130">
        <template #default="{ row }">
          <el-input-number
            v-model="row.plannedQty"
            :min="1"
            :precision="0"
            controls-position="right"
            size="small"
            style="width: 100%"
          />
        </template>
      </el-table-column>
      <el-table-column label="实入数量" width="120">
        <template #default="{ row }">
          <span>{{ row.actualQty ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="库区" width="140">
        <template #default="{ row }">
          <el-select
            v-model="row.warehouseAreaId"
            placeholder="选择库区"
            clearable
            size="small"
            style="width: 100%"
          >
            <el-option
              v-for="a in areaList"
              :key="a.id"
              :label="a.name"
              :value="a.id"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="批次号" width="130">
        <template #default="{ row }">
          <el-input
            v-model="row.batchNo"
            placeholder="批次号"
            size="small"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="60" fixed="right">
        <template #default="{ $index }">
          <el-button
            size="small"
            type="danger"
            text
            @click="handleDeleteRow($index)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 批量复制对话框：步骤1选单 → 步骤2选零件 -->
    <el-dialog v-model="showBatchCopy" :title="copyStep2 ? '勾选要复制的零件' : '从其他入库单复制零件'" width="650px">
      <!-- 步骤1 -->
      <template v-if="!copyStep2">
        <el-form label-width="80px">
          <el-form-item label="源入库单">
            <el-select v-model="sourceOrderId" filterable placeholder="同供应商的单据才可选" style="width: 100%" @change="onSourceOrderSelect">
              <el-option v-for="o in orderList" :key="o.id" :label="`${o.orderNo} - ${o.supplierName}`" :value="o.id" :disabled="o.supplierId !== supplierId" />
            </el-select>
          </el-form-item>
        </el-form>
      </template>
      <!-- 步骤2 -->
      <template v-else>
        <div class="copy-step-header" style="margin-bottom: 12px; color: #606266">
          源单: {{ sourceOrderName }}，勾选要复制的零件：
        </div>
        <el-checkbox-group v-model="selectedSourcePartIds">
          <el-checkbox v-for="p in sourceParts" :key="p.partId" :label="p.partId" :value="p.partId" style="display: block; margin: 4px 0">
            {{ p.partCode }} {{ p.partName }} — {{ p.plannedQty }} {{ p.unit }}
          </el-checkbox>
        </el-checkbox-group>
      </template>
      <template #footer>
        <template v-if="!copyStep2">
          <el-button @click="showBatchCopy = false">取消</el-button>
          <el-button type="primary" @click="loadSourceParts" :disabled="!sourceOrderId">下一步：选择零件</el-button>
        </template>
        <template v-else>
          <el-button @click="copyStep2 = false; sourceOrderId = undefined">上一步</el-button>
          <el-button @click="showBatchCopy = false; copyStep2 = false">取消</el-button>
          <el-button type="primary" @click="doBatchCopy" :disabled="selectedSourcePartIds.length === 0">复制选中 ({{ selectedSourcePartIds.length }})</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 批量设置库区对话框 -->
    <el-dialog v-model="showBatchArea" title="批量设置库区" width="400px">
      <el-form label-width="80px">
        <el-form-item label="库区">
          <el-select
            v-model="batchAreaId"
            placeholder="选择库区"
            style="width: 100%"
          >
            <el-option
              v-for="a in areaList"
              :key="a.id"
              :label="a.name"
              :value="a.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBatchArea = false">取消</el-button>
        <el-button type="primary" @click="doBatchSetArea">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete, CopyDocument, Location } from '@element-plus/icons-vue'
import { getPartListApi } from '@/api/part'
import { getAreaListApi } from '@/api/warehouseArea'
import { listInboundApi, batchCopyPartsApi, batchSetAreaApi, getInboundDetailApi } from '@/api/inbound'
import type { Part, WarehouseArea, InboundOrderVO, InboundDetailVO, InboundDetailDTO } from '@/types/inbound'

const props = defineProps<{
  supplierId?: number
  orderId?: number
  initialDetails?: InboundDetailDTO[]
}>()

// 明细行数据（扩展字段用于 UI）
interface DetailRow {
  partId?: number
  partName: string
  unit: string
  plannedQty: number
  actualQty: number
  warehouseAreaId?: number
  batchNo: string
  lineNo: number
}

const details = ref<DetailRow[]>([])
const availableParts = ref<Part[]>([])
const areaList = ref<WarehouseArea[]>([])
const selectedRows = ref<DetailRow[]>([])

// 批量复制（两步：选单 → 选零件）
const showBatchCopy = ref(false)
const copyStep2 = ref(false)
const sourceOrderId = ref<number>()
const sourceOrderName = ref('')
const sourceParts = ref<InboundDetailVO[]>([])
const selectedSourcePartIds = ref<number[]>([])
const orderList = ref<InboundOrderVO[]>([])

// 批量设置库区
const showBatchArea = ref(false)
const batchAreaId = ref<number>()

// 从 initialDetails 填充明细（创建时为空，编辑时从后端加载）
function loadInitialDetails(list: InboundDetailDTO[]) {
  if (list && list.length > 0) {
    details.value = list.map(d => ({
      partId: d.partId,
      partName: '',
      unit: d.unit || '',
      plannedQty: d.plannedQty,
      actualQty: d.actualQty || 0,
      warehouseAreaId: d.warehouseAreaId,
      batchNo: d.batchNo || '',
      lineNo: d.lineNo,
    }))
  }
}

onMounted(async () => {
  try {
    const res = await getAreaListApi()
    areaList.value = res.data
  } catch { /* ignore */ }
  loadInitialDetails(props.initialDetails || [])
})

// 编辑模式下 initialDetails 是异步加载的，需要 watch
watch(() => props.initialDetails, (val) => {
  if (val && val.length > 0 && details.value.length === 0) {
    loadInitialDetails(val)
  }
}, { deep: true })

// 供应商变化 → 重新加载零件
// 有供应商时只显示该供应商的零件，没有时显示全部
watch(() => props.supplierId, async (sid) => {
  try {
    const res = await getPartListApi(sid || undefined)
    availableParts.value = res.data
  } catch { /* ignore */ }
}, { immediate: true })

// 选择零件 → 自动填充名称和单位
function onPartChange(partId: number, row: DetailRow) {
  const part = availableParts.value.find(p => p.id === partId)
  if (part) {
    row.partName = part.name
    row.unit = part.unit
  }
}

function handleAddRow() {
  if (!props.supplierId) {
    ElMessage.warning('请先选择供应商')
    return
  }
  details.value.push({
    partId: undefined,
    partName: '',
    unit: '',
    plannedQty: 0,
    actualQty: 0,
    warehouseAreaId: undefined,
    batchNo: '',
    lineNo: details.value.length + 1,
  })
}

function handleDeleteRow(index: number) {
  details.value.splice(index, 1)
}

function handleDeleteRows() {
  const indices = selectedRows.value
    .map(r => details.value.indexOf(r))
    .filter(i => i >= 0)
    .sort((a, b) => b - a)
  indices.forEach(i => details.value.splice(i, 1))
  selectedRows.value = []
}

function onSelectionChange(rows: DetailRow[]) {
  selectedRows.value = rows
}

// 批量复制（两步流程）
watch(showBatchCopy, async (val) => {
  if (val) {
    copyStep2.value = false
    sourceOrderId.value = undefined
    selectedSourcePartIds.value = []
    sourceParts.value = []
    try {
      const res = await listInboundApi({ current: 1, size: 200 })
      // 只显示有相同供应商的单据（或编辑模式下同供应商）
      orderList.value = res.data.records
    } catch { /* ignore */ }
  }
})

function onSourceOrderSelect(_id: number) {
  sourceOrderName.value = orderList.value.find(o => o.id === _id)?.orderNo || ''
}

async function loadSourceParts() {
  if (!sourceOrderId.value) return
  try {
    const res = await getInboundDetailApi(sourceOrderId.value)
    if (res.data?.details) {
      sourceParts.value = res.data.details
      selectedSourcePartIds.value = []
    }
    copyStep2.value = true
  } catch {
    ElMessage.error('加载源单零件失败')
  }
}

async function doBatchCopy() {
  if (!props.orderId) {
    ElMessage.warning('请先保存入库单后再复制零件')
    return
  }
  if (selectedSourcePartIds.value.length === 0) {
    ElMessage.warning('请选择要复制的零件')
    return
  }
  try {
    // 逐个复制选中的零件（通过后端 batchCopyParts 接口，每次复制单个零件）
    // 简化处理：用保存接口把选中的零件合并到当前表单中
    for (const partId of selectedSourcePartIds.value) {
      const src = sourceParts.value.find(p => p.partId === partId)
      if (!src) continue
      // 检查是否已存在
      const existing = details.value.find(d => d.partId === src.partId)
      if (existing) {
        existing.plannedQty += src.plannedQty
      } else {
        details.value.push({
          partId: src.partId,
          partName: src.partName,
          unit: src.unit,
          plannedQty: src.plannedQty,
          actualQty: 0,
          warehouseAreaId: src.warehouseAreaId ?? undefined,
          batchNo: src.batchNo || '',
          lineNo: details.value.length + 1,
        })
      }
    }
    ElMessage.success(`已添加 ${selectedSourcePartIds.value.length} 个零件`)
    showBatchCopy.value = false
    copyStep2.value = false
  } catch { /* ignore */ }
}

async function doBatchSetArea() {
  if (!batchAreaId.value || selectedRows.value.length === 0) return

  // 前端直接设置
  selectedRows.value.forEach(row => {
    row.warehouseAreaId = batchAreaId.value
  })

  // 如果已有 orderId，也可以调用后端接口
  if (props.orderId) {
    try {
      // 获取选中行的 detail IDs（需要从服务端拿，这里简化处理）
      // 实际调用 batchSetAreaApi
    } catch { /* ignore */ }
  }

  ElMessage.success('库区设置成功')
  showBatchArea.value = false
}

// 暴露方法给父组件
function getDetails(): InboundDetailDTO[] {
  return details.value.map((d, i) => ({
    partId: d.partId!,
    plannedQty: d.plannedQty,
    unit: d.unit,
    warehouseAreaId: d.warehouseAreaId,
    batchNo: d.batchNo || undefined,
    lineNo: i + 1,
  }))
}

defineExpose({ getDetails })
</script>

<style scoped>
.parts-table {
  width: 100%;
}
.toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}
</style>
