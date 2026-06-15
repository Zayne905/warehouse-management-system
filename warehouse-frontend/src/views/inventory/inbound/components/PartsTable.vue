<template>
  <div class="parts-table">
    <el-table
      ref="tableRef"
      :data="partRows"
      border
      stripe
      v-loading="loading"
      @selection-change="onSelectionChange"
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
            :disabled="!row.checked"
            controls-position="right"
            size="small"
            style="width: 100%"
            @change="onCapacityChange(row)"
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
            :step="0.1"
            :disabled="!row.checked"
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
            :disabled="!row.checked"
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
            :disabled="!row.checked"
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
            :disabled="!row.checked"
            size="small"
          />
        </template>
      </el-table-column>
    </el-table>

    <div class="hint" v-if="partRows.length > 0">
      已勾选 {{ checkedCount }} 个零件
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { getPartListApi } from '@/api/part'
import { getAreaListApi } from '@/api/warehouseArea'
import type { Part, WarehouseArea, InboundDetailDTO } from '@/types/inbound'

const props = defineProps<{
  supplierId?: number
  orderId?: number
  initialDetails?: InboundDetailDTO[]
}>()

const loading = ref(false)
const areaList = ref<WarehouseArea[]>([])
const tableRef = ref()

// 零件行 = Part + UI 状态
interface PartRow extends Part {
  checked: boolean
  boxCount: number
  plannedQty: number
  actualQty: number
  batchNo: string
  lastEdited: 'box' | 'quantity'
}

const partRows = ref<PartRow[]>([])
const syncingSelection = ref(false)
const loadVersion = ref(0)
const checkedCount = computed(() => partRows.value.filter(r => r.checked).length)
const isEdit = computed(() => !!(props.initialDetails && props.initialDetails.length > 0))

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
    // 版本检查：忽略过期响应
    if (version !== loadVersion.value) return

    const parts = res.data || []

    if (props.initialDetails && props.initialDetails.length > 0) {
      const detailMap = new Map(props.initialDetails.map(d => [d.partId!, d]))
      partRows.value = parts.map(p => {
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
          lastEdited: 'box',
        }
      })
    } else {
      partRows.value = parts.map(p => ({
        ...p,
        checked: false,
        boxCount: 0,
        plannedQty: 0,
        actualQty: 0,
        warehouseAreaId: p.warehouseAreaId,
        batchNo: '',
        lastEdited: 'box',
      }))
    }

    // 恢复勾选状态（Element Plus 表格需要 toggleRowSelection）
    await nextTick()
    // 二次版本检查：nextTick 期间可能有新的 loadParts 调用
    if (version !== loadVersion.value) return
    syncingSelection.value = true
    partRows.value.forEach(row => {
      if (tableRef.value) {
        tableRef.value.toggleRowSelection(row, row.checked)
      }
    })
    syncingSelection.value = false
  } catch { /* ignore */ }
  finally {
    loading.value = false
  }
}

// 合并监听：supplierId 和 initialDetails 任一变化都重新加载
watch(
  () => [props.supplierId, props.initialDetails] as const,
  ([sid, details]) => {
    if (!sid) return
    // 编辑模式：等 initialDetails 加载完再一次性处理
    if (details && details.length > 0) {
      loadParts(sid)
    } else if (!props.orderId) {
      // 新建模式：没有已有明细，直接加载
      loadParts(sid)
    }
  },
  { immediate: true, deep: true }
)

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

function onSelectionChange(rows: PartRow[]) {
  if (syncingSelection.value) return
  // 同步 checked 状态
  const selectedIds = new Set(rows.map(r => r.id))
  partRows.value.forEach(r => {
    r.checked = selectedIds.has(r.id)
  })
}

function getDetails(): InboundDetailDTO[] {
  return partRows.value
    .filter(r => r.checked && (r.plannedQty || 0) > 0)
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
.hint {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}
</style>
