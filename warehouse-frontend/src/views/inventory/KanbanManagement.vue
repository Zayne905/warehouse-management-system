<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="关键字">
          <el-input v-model="queryForm.keyword" clearable
            placeholder="入库单号 / 出库单号 / 看板号 / 零件号"
            style="width: 360px" @keyup.enter="loadList" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option v-for="(text, value) in KanbanStatusText" :key="value"
              :label="text" :value="Number(value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="库区">
          <el-select v-model="queryForm.warehouseAreaName" clearable filterable placeholder="全部库区" style="width: 160px">
            <el-option v-for="name in warehouseAreas" :key="name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="queryForm.supplierName" clearable filterable placeholder="全部供应商" style="width: 180px">
            <el-option v-for="name in suppliers" :key="name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="loadList">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="reset">重置</el-button>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <el-table v-loading="loading" :data="rows" border stripe @row-click="openDetail" style="cursor: pointer">
        <el-table-column prop="kanbanNo" label="看板号" min-width="230" />
        <el-table-column prop="inboundOrderNo" label="入库单号" min-width="150" />
        <el-table-column prop="outboundOrderNo" label="出库单号" min-width="150">
          <template #default="{ row }">{{ row.outboundOrderNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="partCode" label="零件号" min-width="130" />
        <el-table-column prop="partName" label="零件名称" min-width="130" />
        <el-table-column prop="supplierName" label="供应商" min-width="130" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="KanbanStatusTagType[row.status]">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数量/箱数" width="110" align="center">
          <template #default="{ row }">{{ quantityBoxes(row) }}</template>
        </el-table-column>
        <el-table-column prop="warehouseName" label="仓库" width="100" />
        <el-table-column prop="warehouseAreaName" label="库区/库位" min-width="120" />
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
      </el-table>
      <el-empty v-if="!loading && rows.length === 0" description="暂无看板数据" />
    </el-card>

    <el-dialog v-model="detailVisible" title="看板详细信息" width="900px">
      <div v-loading="detailLoading">
        <div v-if="detail" class="info-layout">
          <el-descriptions :column="3" border class="details">
            <el-descriptions-item label="看板号">{{ detail.kanban.kanbanNo }}</el-descriptions-item>
            <el-descriptions-item label="状态"><el-tag>{{ detail.kanban.statusText }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="数量/箱数">{{ quantityBoxes(detail.kanban) }}</el-descriptions-item>
            <el-descriptions-item label="入库单号">{{ detail.kanban.inboundOrderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="出库单号">{{ detail.kanban.outboundOrderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="零件号">{{ detail.kanban.partCode }}</el-descriptions-item>
            <el-descriptions-item label="零件名称">{{ detail.kanban.partName }}</el-descriptions-item>
            <el-descriptions-item label="供应商">{{ detail.kanban.supplierName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="仓库">{{ detail.kanban.warehouseName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="库区/库位">{{ detail.kanban.warehouseAreaName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="器具型号">{{ detail.kanban.containerModel || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ detail.kanban.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="入库时间">{{ detail.kanban.inboundTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="出库时间">{{ detail.kanban.outboundTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="转包次数">{{ detail.kanban.repackRecords?.length || 0 }}</el-descriptions-item>
          </el-descriptions>
          <div class="qr"><canvas ref="qrRef" width="160" height="160" /><div>看板二维码</div></div>
        </div>
        <el-divider>生命周期记录</el-divider>
        <el-timeline v-if="detail">
          <el-timeline-item v-for="(event, index) in detail.events" :key="index"
            :timestamp="event.time || '-'" placement="top">
            <b>{{ event.title }}</b><span v-if="event.orderNo">：{{ event.orderNo }}</span>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import {
  getKanbanLifecycleApi, listKanbansApi, KanbanStatusTagType, KanbanStatusText,
  type Kanban, type KanbanLifecycle,
} from '@/api/kanban'

const loading = ref(false)
const detailLoading = ref(false)
const rows = ref<Kanban[]>([])
const allSuppliers = ref<string[]>([])
const allWarehouseAreas = ref<string[]>([])
const detailVisible = ref(false)
const detail = ref<KanbanLifecycle>()
const qrRef = ref<HTMLCanvasElement>()
const queryForm = reactive({ keyword: '', status: '' as number | '', warehouseAreaName: '', supplierName: '' })
const suppliers = computed(() => allSuppliers.value)
const warehouseAreas = computed(() => allWarehouseAreas.value)

function quantityBoxes(row: Kanban) {
  const capacity = Number(row.originalQty || row.quantity || 1)
  const boxes = capacity > 0 ? Number(row.quantity || 0) / capacity : 0
  return `${row.quantity}/${Number(boxes.toFixed(2))}箱`
}

async function loadList() {
  loading.value = true
  try {
    const res = await listKanbansApi(queryForm)
    rows.value = res.data || []
    if (!queryForm.keyword && queryForm.status === '' && !queryForm.warehouseAreaName && !queryForm.supplierName) {
      allSuppliers.value = [...new Set(rows.value.map(row => row.supplierName).filter(Boolean))].sort()
      allWarehouseAreas.value = [...new Set(rows.value.map(row => row.warehouseAreaName).filter(Boolean))].sort()
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '加载看板列表失败')
  } finally {
    loading.value = false
  }
}

function reset() {
  Object.assign(queryForm, { keyword: '', status: '', warehouseAreaName: '', supplierName: '' })
  loadList()
}

async function openDetail(row: Kanban) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = undefined
  try {
    const res = await getKanbanLifecycleApi(row.kanbanNo)
    detail.value = res.data
    await nextTick()
    if (qrRef.value) await QRCode.toCanvas(qrRef.value, row.kanbanNo, { width: 160, margin: 1 })
  } catch (e: any) {
    ElMessage.error(e?.message || '加载看板详情失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.info-layout { display: flex; gap: 20px; align-items: flex-start; }
.details { flex: 1; }
.qr { width: 180px; text-align: center; color: #909399; }
</style>
