<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="搜索">
          <el-input
            v-model="query.keyword"
            placeholder="零件编码或名称"
            clearable
            style="width: 220px"
            @keyup.enter="fetchData"
          />
        </el-form-item>
        <el-form-item label="库位">
          <el-select v-model="query.warehouseAreaId" clearable placeholder="全部库位" style="width: 180px">
            <el-option v-for="area in areaList" :key="area.id" :label="area.name" :value="area.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="openToggleDialog">
            <el-icon><Camera /></el-icon>
            扫码封存/解封
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        :default-sort="{ prop: 'totalStock', order: 'descending' }"
        @expand-change="loadKanbans"
        :row-class-name="rowClassName"
      >
        <el-table-column type="expand">
          <template #default="{ row }">
            <div v-if="kanbanMap[row.partId]?.length" style="padding:8px 20px">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
                <h4 style="margin:0">库存看板明细</h4>
                <div class="batch-actions">
                  <span class="selection-hint">已选 {{ selectedKanbans[row.partId]?.length || 0 }} 项</span>
                  <el-button
                    size="small"
                    type="warning"
                    @click="doBatchBlock(row.partId)"
                    :disabled="selectedAvailableCount(row.partId) === 0"
                  >
                    封存选中项
                  </el-button>
                  <el-button
                    size="small"
                    type="success"
                    @click="doBatchUnblock(row.partId)"
                    :disabled="selectedBlockedCount(row.partId) === 0"
                  >
                    解封选中项
                  </el-button>
                </div>
              </div>
              <el-table
                :data="kanbanMap[row.partId]"
                size="small"
                border
                row-key="id"
                @selection-change="(selection: Kanban[]) => handleKanbanSelection(row.partId, selection)"
              >
                <el-table-column type="selection" width="46" :selectable="isSelectableKanban" />
                <el-table-column prop="kanbanNo" label="看板号" min-width="240" />
                <el-table-column prop="boxSeq" label="箱号" width="60" align="center">
                  <template #default="{ row: k }">C-{{ k.boxSeq }}</template>
                </el-table-column>
                <el-table-column label="箱容量" width="75" align="center">
                  <template #default="{ row: k }">{{ k.originalQty ?? k.quantity }}</template>
                </el-table-column>
                <el-table-column label="剩余数量" width="85" align="center">
                  <template #default="{ row: k }">
                    <span :style="{color: k.quantity < (k.originalQty ?? k.quantity) ? '#e6a23c' : ''}">
                      {{ k.quantity }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="warehouseAreaName" label="库区" width="120" />
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row: k }">
                    <el-tag :type="KanbanStatusTagType[k.status]" size="small">
                      {{ KanbanStatusText[k.status] }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="inboundOrderNo" label="入库单号" width="140" />
                <el-table-column prop="createTime" label="入库时间" width="170" />
                <el-table-column prop="outboundOrderNo" label="出库单号" width="140">
                  <template #default="{ row: k }">{{ k.outboundOrderNo || '-' }}</template>
                </el-table-column>
                <el-table-column label="出库时间" width="170">
                  <template #default="{ row: k }">{{ k.outboundScanTime || '-' }}</template>
                </el-table-column>
                <el-table-column label="操作" width="140" fixed="right">
                  <template #default="{ row: k }">
                    <el-button v-if="k.status === 1" size="small" type="warning" @click="doBlock(k)">封存</el-button>
                    <el-button v-if="k.status === 4" size="small" type="success" @click="doUnblock(k)">解封</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div v-else style="padding:8px 20px;color:#909399">暂无看板记录</div>
          </template>
        </el-table-column>
        <el-table-column prop="partCode" label="零件编码" width="120" sortable="custom" />
        <el-table-column prop="partName" label="零件名称" min-width="140" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column prop="kanbanCount" label="在库箱数" width="85" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.kanbanCount" size="small" effect="plain">{{ row.kanbanCount }}</el-tag>
            <span v-else class="text-hint">0</span>
          </template>
        </el-table-column>
        <el-table-column label="箱均数量" width="90" align="center">
          <template #default="{ row }">
            <span v-if="row.avgQtyPerBox" class="avg-qty">{{ row.avgQtyPerBox }}</span>
            <span v-else class="text-hint">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalStock" label="库存总量" width="110" align="center" sortable="custom">
          <template #default="{ row }">
            <span class="stock-qty">{{ row.totalStock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="isLowStock(row)" type="danger" size="small" effect="dark">低储</el-tag>
            <el-tag v-else-if="isHighStock(row)" type="warning" size="small" effect="dark">高储</el-tag>
            <el-tag v-else-if="hasThreshold(row)" type="success" size="small" effect="plain">正常</el-tag>
            <span v-else class="text-hint">-</span>
          </template>
        </el-table-column>
        <el-table-column label="最低储备" width="85" align="center">
          <template #default="{ row }">
            <span v-if="row.minStock">{{ row.minStock }}</span>
            <span v-else class="text-hint">-</span>
          </template>
        </el-table-column>
        <el-table-column label="最高储备" width="85" align="center">
          <template #default="{ row }">
            <span v-if="row.maxStock">{{ row.maxStock }}</span>
            <span v-else class="text-hint">-</span>
          </template>
        </el-table-column>
        <el-table-column label="库区分布" min-width="200">
          <template #default="{ row }">
            <div class="area-tags">
              <el-tag v-for="a in row.areaStocks" :key="a.areaId" size="small" effect="plain" style="margin:2px 4px 2px 0">
                {{ a.areaName }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="summary" v-if="tableData.length > 0">
        共 {{ tableData.length }} 种零件，库存总量 {{ totalAll }}
      </div>
    </el-card>

    <!-- 扫码封存/解封对话框 -->
    <el-dialog v-model="showToggleDialog" title="扫码封存/解封" width="500px" @closed="onToggleDialogClosed">
      <el-form label-width="80px">
        <el-form-item label="看板号">
          <el-input v-model="toggleKanbanNo" placeholder="请输入看板号" @keyup.enter="doToggleBlock" ref="toggleInputRef" />
        </el-form-item>
      </el-form>
      <div v-if="toggleResult" style="margin-top:12px">
        <el-alert :title="toggleResult" :type="toggleError ? 'error' : 'success'" :closable="false" />
      </div>
      <template #footer>
        <el-button @click="showToggleDialog = false">关闭</el-button>
        <el-button type="primary" @click="doToggleBlock" :loading="toggleLoading">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { Search, Camera } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getStockListApi, type InventoryVO } from '@/api/inventory'
import { getAreaListApi } from '@/api/warehouseArea'
import type { WarehouseArea } from '@/types/inbound'
import { listKanbansByPart, blockKanbanApi, unblockKanbanApi, batchBlockApi, toggleBlockApi, batchUnblockApi, KanbanStatusText, KanbanStatusTagType, type Kanban } from '@/api/kanban'

const loading = ref(false)
const tableData = ref<InventoryVO[]>([])
const kanbanMap = ref<Record<number, Kanban[]>>({})
const selectedKanbans = ref<Record<number, Kanban[]>>({})
const areaList = ref<WarehouseArea[]>([])

// 扫码封存/解封
const showToggleDialog = ref(false)
const toggleLoading = ref(false)
const toggleKanbanNo = ref('')
const toggleResult = ref('')
const toggleError = ref(false)
const toggleInputRef = ref<any>(null)

function openToggleDialog() {
  toggleKanbanNo.value = ''
  toggleResult.value = ''
  toggleError.value = false
  showToggleDialog.value = true
  nextTick(() => toggleInputRef.value?.focus())
}

async function doToggleBlock() {
  const no = toggleKanbanNo.value.trim()
  if (!no) { ElMessage.warning('请输入看板号'); return }
  toggleLoading.value = true; toggleResult.value = ''; toggleError.value = false
  try {
    const res = await toggleBlockApi(no)
    const d = res.data
    toggleResult.value = `${d.partName}(${d.partCode}) 已${d.action}`
    toggleError.value = false
    toggleKanbanNo.value = ''
    // 刷新已展开的看板明细
    const partIds = Object.keys(kanbanMap.value).map(Number)
    for (const pid of partIds) {
      try {
        const r = await listKanbansByPart(pid)
        kanbanMap.value[pid] = r.data || []
        selectedKanbans.value[pid] = []
      } catch { /* */ }
    }
    await fetchData()
    nextTick(() => toggleInputRef.value?.focus())
  } catch (e: any) {
    toggleResult.value = e?.message || '操作失败'; toggleError.value = true
  } finally { toggleLoading.value = false }
}

function onToggleDialogClosed() {
  toggleKanbanNo.value = ''
  toggleResult.value = ''
  toggleError.value = false
}

const query = reactive({
  keyword: '',
  warehouseAreaId: undefined as number | undefined,
})

const totalAll = computed(() =>
  tableData.value.reduce((s, r) => s + (r.totalStock || 0), 0)
)

async function fetchData() {
  loading.value = true
  try {
    const res = await getStockListApi(query.keyword || undefined, query.warehouseAreaId)
    tableData.value = res.data || []
  } catch { /* ignore */ } finally { loading.value = false }
}

async function loadKanbans(row: any, expandedRows: any[]) {
  if (expandedRows.some((r: any) => r.partId === row.partId)) {
    if (!kanbanMap.value[row.partId]) {
      try {
        const res = await listKanbansByPart(row.partId)
        kanbanMap.value[row.partId] = res.data || []
        selectedKanbans.value[row.partId] = []
      } catch { /* */ }
    }
  }
}

function isSelectableKanban(k: Kanban) {
  return k.status === 1 || k.status === 4
}

function handleKanbanSelection(partId: number, selection: Kanban[]) {
  selectedKanbans.value[partId] = selection
}

function selectedAvailableCount(partId: number) {
  return (selectedKanbans.value[partId] || []).filter(k => k.status === 1).length
}

function selectedBlockedCount(partId: number) {
  return (selectedKanbans.value[partId] || []).filter(k => k.status === 4).length
}

async function refreshPart(partId: number) {
  const res = await listKanbansByPart(partId)
  kanbanMap.value[partId] = res.data || []
  selectedKanbans.value[partId] = []
  await fetchData()
}

async function doBlock(k: Kanban) {
  try {
    await blockKanbanApi(k.kanbanNo)
    ElMessage.success(`看板 ${k.kanbanNo} 已封存`)
    await refreshPart(k.partId)
  } catch (e: any) {
    ElMessage.error(e?.message || '封存失败')
  }
}

async function doUnblock(k: Kanban) {
  try {
    await unblockKanbanApi(k.kanbanNo)
    ElMessage.success(`看板 ${k.kanbanNo} 已解封`)
    await refreshPart(k.partId)
  } catch (e: any) {
    ElMessage.error(e?.message || '解封失败')
  }
}

async function doBatchBlock(partId: number) {
  const availableNos = (selectedKanbans.value[partId] || [])
    .filter(k => k.status === 1)
    .map(k => k.kanbanNo)
  if (availableNos.length === 0) {
    ElMessage.warning('请勾选要封存的在库看板')
    return
  }
  try {
    const count = await batchBlockApi(availableNos)
    ElMessage.success(`已批量封存 ${count.data} 个看板`)
    await refreshPart(partId)
  } catch (e: any) {
    ElMessage.error(e?.message || '批量封存失败')
  }
}

async function doBatchUnblock(partId: number) {
  const blockedNos = (selectedKanbans.value[partId] || [])
    .filter(k => k.status === 4)
    .map(k => k.kanbanNo)
  if (blockedNos.length === 0) {
    ElMessage.warning('请勾选要解封的看板')
    return
  }
  try {
    const count = await batchUnblockApi(blockedNos)
    ElMessage.success(`已批量解封 ${count.data} 个看板`)
    await refreshPart(partId)
  } catch (e: any) {
    ElMessage.error(e?.message || '批量解封失败')
  }
}

// ============ 高低储状态判断 ============

function isLowStock(row: InventoryVO): boolean {
  const min = row.minStock ?? 0
  const stock = Number(row.totalStock ?? 0)
  return min > 0 && stock <= min
}

function isHighStock(row: InventoryVO): boolean {
  const max = row.maxStock ?? 0
  const stock = Number(row.totalStock ?? 0)
  return max > 0 && stock >= max
}

function hasThreshold(row: InventoryVO): boolean {
  return (row.minStock ?? 0) > 0 || (row.maxStock ?? 0) > 0
}

function rowClassName({ row }: { row: InventoryVO }) {
  if (isLowStock(row)) return 'row-low-stock'
  if (isHighStock(row)) return 'row-high-stock'
  return ''
}

onMounted(async () => {
  const areas = await getAreaListApi()
  areaList.value = areas.data || []
  fetchData()
})
</script>

<style scoped>
.stock-qty {
  font-weight: bold;
  color: #409eff;
  font-size: 15px;
}
.summary {
  margin-top: 12px;
  text-align: right;
  color: #909399;
  font-size: 13px;
}
.batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.selection-hint {
  color: #909399;
  font-size: 13px;
}

.text-hint {
  color: #c0c4cc;
}

:deep(.row-low-stock) {
  background-color: #fef0f0 !important;
}

:deep(.row-high-stock) {
  background-color: #fdf6ec !important;
}
</style>
