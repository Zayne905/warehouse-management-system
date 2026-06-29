<template>
  <div class="parts-table">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button size="small" type="primary" @click="openDialog" :disabled="!supplierId">
        <el-icon><Plus /></el-icon>添加零件
      </el-button>
      <el-button size="small" type="success" @click="openBatchImport" :disabled="!supplierId">
        <el-icon><Upload /></el-icon>批量导入
      </el-button>
    </div>

    <!-- 主表格：只显示已添加的零件 -->
    <el-table
      ref="tableRef"
      :data="addedParts"
      border
      stripe
      v-loading="loading"
    >
      <el-table-column type="selection" width="45" />
      <el-table-column prop="code" label="物料编码" min-width="120" />
      <el-table-column prop="name" label="物料名称" min-width="120" />
      <el-table-column label="包装容量" width="110">
        <template #default="{ row }">
          <el-input-number
            v-model="row.packageCapacity"
            :min="1"
            :precision="0"
            :disabled="true"
            controls-position="right"
            size="small"
            style="width: 100%"
          />
        </template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="70" />
      <el-table-column label="箱数" width="120">
        <template #default="{ row }">
          <el-input-number
            v-model="row.boxCount"
            :min="0"
            :precision="2"
            :step="1"
            controls-position="right"
            size="small"
            style="width: 100%"
            @change="onBoxCountChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="入库数量" width="130">
        <template #default="{ row }">
          <el-input-number
            v-model="row.plannedQty"
            :min="0"
            :precision="2"
            :step="1"
            controls-position="right"
            size="small"
            style="width: 100%"
            @change="onQuantityChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="实入" width="80" align="center" v-if="isEdit">
        <template #default="{ row }">
          {{ row.actualQty ?? 0 }}
        </template>
      </el-table-column>
      <el-table-column label="库区" width="150">
        <template #default="{ row }">
          <el-select
            v-model="row.warehouseAreaId"
            placeholder="库区"
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
      <el-table-column label="操作" width="70" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="danger" text @click="removePart(row)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="hint" v-if="addedParts.length === 0 && !loading">
      尚未添加零件，请点击"添加零件"按钮选择
    </div>

    <!-- 零件选择弹窗 -->
    <el-dialog v-model="dialogVisible" title="选择零件" width="750px" @opened="onDialogOpened">
      <div class="dialog-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索物料编码或名称"
          clearable
          style="width: 260px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <el-table
        ref="dialogTableRef"
        :data="filteredAllParts"
        border
        stripe
        max-height="400"
        @selection-change="onDialogSelectionChange"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="code" label="物料编码" width="130" />
        <el-table-column prop="name" label="物料名称" min-width="140" />
        <el-table-column prop="unit" label="单位" width="70" />
      </el-table>
      <div v-if="filteredAllParts.length === 0 && !loading" style="text-align:center;padding:20px;color:#909399">
        {{ searchKeyword ? '无匹配零件' : '暂无零件' }}
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDialogAdd" :disabled="pendingDialogSelection.size === 0">
          添加选中 ({{ pendingDialogSelection.size }})
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <BatchImportDialog
      v-if="batchImportVisible"
      :supplier-id="supplierId"
      :all-parts="allParts"
      @import="onBatchImport"
      @close="batchImportVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { Plus, Delete, Search, Upload } from '@element-plus/icons-vue'
import { getPartListApi } from '@/api/part'
import { getAreaListApi } from '@/api/warehouseArea'
import { ElMessage } from 'element-plus'
import type { Part, WarehouseArea, InboundDetailDTO } from '@/types/inbound'
import BatchImportDialog, { type ImportedPart } from './BatchImportDialog.vue'

const props = defineProps<{
  supplierId?: number
  orderId?: number
  initialDetails?: InboundDetailDTO[]
}>()

const loading = ref(false)
const areaList = ref<WarehouseArea[]>([])
const tableRef = ref()
const dialogTableRef = ref()

// 零件行 = Part + UI 状态
interface PartRow extends Part {
  checked: boolean
  boxCount: number
  plannedQty: number
  actualQty: number
  batchNo: string
  lastEdited: 'box' | 'quantity'
}

// 所有零件（供应商的全部零件，用于弹窗展示）
const allParts = ref<PartRow[]>([])
// 弹窗相关
const dialogVisible = ref(false)
const searchKeyword = ref('')
const pendingDialogSelection = ref<Set<number>>(new Set())

const loadVersion = ref(0)
const isEdit = computed(() => !!(props.initialDetails && props.initialDetails.length > 0))

// 主表格：只显示已添加的零件
const addedParts = computed(() => allParts.value.filter(r => r.checked))

// 弹窗表格：搜索过滤后的全部零件
const filteredAllParts = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return allParts.value
  return allParts.value.filter(p =>
    (p.code && p.code.toLowerCase().includes(kw)) ||
    (p.name && p.name.toLowerCase().includes(kw))
  )
})

onMounted(async () => {
  try {
    const res = await getAreaListApi()
    areaList.value = res.data
  } catch { /* ignore */ }
})

async function loadParts(supplierId?: number) {
  const version = ++loadVersion.value
  loading.value = true
  try {
    const res = await getPartListApi(supplierId || undefined)
    if (version !== loadVersion.value) return

    const parts = (res.data || []) as Part[]

    if (props.initialDetails && props.initialDetails.length > 0) {
      const detailMap = new Map(props.initialDetails.map(d => [d.partId!, d]))
      allParts.value = parts.map(p => {
        const detail = detailMap.get(p.id)
        const capacity = p.packageCapacity || 1
        return {
          ...p,
          checked: detail !== undefined,
          boxCount: detail?.boxCount ?? ((detail?.plannedQty || 0) / capacity),
          plannedQty: detail?.plannedQty ?? (capacity * (detail?.boxCount || 0)),
          actualQty: detail?.actualQty ?? 0,
          warehouseAreaId: detail?.warehouseAreaId ?? p.warehouseAreaId,
          batchNo: detail?.batchNo || '',
          lastEdited: 'box' as const,
        }
      })
    } else {
      allParts.value = parts.map(p => ({
        ...p,
        checked: false,
        boxCount: 0,
        plannedQty: 0,
        actualQty: 0,
        warehouseAreaId: p.warehouseAreaId,
        batchNo: '',
        lastEdited: 'box' as const,
      }))
    }
  } catch { /* ignore */ }
  finally {
    loading.value = false
  }
}

// 合并监听：supplierId 和 initialDetails
watch(
  () => [props.supplierId, props.initialDetails] as const,
  ([sid, details]) => {
    if (!sid) {
      allParts.value = []
      return
    }
    if (details && details.length > 0) {
      loadParts(sid)
    } else if (!props.orderId) {
      loadParts(sid)
    }
  },
  { immediate: true, deep: true }
)

// ============ 弹窗逻辑 ============

function openDialog() {
  searchKeyword.value = ''
  dialogVisible.value = true
}

async function onDialogOpened() {
  // 每次打开弹窗时，同步已添加的零件到待选集合
  pendingDialogSelection.value = new Set(allParts.value.filter(p => p.checked).map(p => p.id))
  await nextTick()
  // 在表格中勾选已添加的零件
  allParts.value.forEach(row => {
    if (row.checked) {
      dialogTableRef.value?.toggleRowSelection(row, true)
    }
  })
}

function onDialogSelectionChange(rows: PartRow[]) {
  pendingDialogSelection.value = new Set(rows.map(r => r.id))
}

// 搜索变化时重新同步表格勾选状态
watch(filteredAllParts, async () => {
  await nextTick()
  allParts.value.forEach(row => {
    if (pendingDialogSelection.value.has(row.id)) {
      dialogTableRef.value?.toggleRowSelection(row, true)
    } else {
      dialogTableRef.value?.toggleRowSelection(row, false)
    }
  })
})

function confirmDialogAdd() {
  // 将勾选的零件标记为已添加
  const selectedIds = pendingDialogSelection.value
  allParts.value.forEach(p => {
    if (selectedIds.has(p.id)) {
      if (!p.checked) {
        // 新添加的零件：初始化默认值
        p.checked = true
        p.boxCount = 0
        p.plannedQty = 0
        p.warehouseAreaId = p.warehouseAreaId // 保持默认库区
      }
    }
  })
  dialogVisible.value = false
}

// ============ 主表格操作 ============

function removePart(row: PartRow) {
  row.checked = false
  row.boxCount = 0
  row.plannedQty = 0
  row.actualQty = 0
}

// ============ 批量导入 ============

const batchImportVisible = ref(false)

function openBatchImport() {
  batchImportVisible.value = true
}

function onBatchImport(importedParts: ImportedPart[]) {
  let newCount = 0
  let mergeCount = 0

  allParts.value.forEach(part => {
    const imported = importedParts.find(p => p.partId === part.id)
    if (!imported) return

    if (part.checked) {
      // 已在表格中：合并数量
      part.plannedQty = round((part.plannedQty || 0) + imported.plannedQty)
      part.boxCount = round(part.plannedQty / (part.packageCapacity || 1))
      mergeCount++
    } else {
      // 新添加：设置字段
      part.checked = true
      part.plannedQty = imported.plannedQty
      part.boxCount = imported.boxCount
      part.warehouseAreaId = imported.warehouseAreaId ?? part.warehouseAreaId
      part.batchNo = imported.batchNo || ''
      part.lastEdited = 'quantity'
      newCount++
    }
  })

  batchImportVisible.value = false
  const msgParts: string[] = []
  if (newCount > 0) msgParts.push(`新增 ${newCount} 个`)
  if (mergeCount > 0) msgParts.push(`合并 ${mergeCount} 个`)
  ElMessage.success(`成功导入 ${msgParts.join('，')} 零件`)
}

// ============ 数量联动 ============

function round(value: number, precision = 2): number {
  const factor = 10 ** precision
  return Math.round((value + Number.EPSILON) * factor) / factor
}

function onBoxCountChange(row: PartRow) {
  row.lastEdited = 'box'
  row.plannedQty = round((row.packageCapacity || 1) * (row.boxCount || 0))
}

function onQuantityChange(row: PartRow) {
  row.lastEdited = 'quantity'
  row.boxCount = round((row.plannedQty || 0) / (row.packageCapacity || 1))
}

function onCapacityChange(row: PartRow) {
  if (row.lastEdited === 'quantity') {
    onQuantityChange(row)
  } else {
    onBoxCountChange(row)
  }
}

// ============ 导出明细 ============

function getDetails(): InboundDetailDTO[] {
  return addedParts.value
    .filter(r => (r.plannedQty || 0) > 0)
    .map((r, i) => ({
      partId: r.id,
      plannedQty: r.plannedQty,
      unit: r.unit,
      warehouseAreaId: r.warehouseAreaId,
      batchNo: r.batchNo || undefined,
      boxCount: r.boxCount,
      actualQty: r.actualQty,
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
.hint {
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
  text-align: center;
  padding: 20px;
}
.dialog-search {
  margin-bottom: 12px;
}
</style>
