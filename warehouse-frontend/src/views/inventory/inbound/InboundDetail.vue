<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>入库单详情</span>
          <div>
            <el-button size="small" @click="showPrintDialog = true">
              <el-icon><Printer /></el-icon>
              打印
            </el-button>
            <el-button
              v-if="canEdit"
              size="small"
              type="primary"
              @click="handleEdit"
            >
              <el-icon><Edit /></el-icon>
              修改
            </el-button>
            <el-button size="small" @click="handleBack">
              <el-icon><Back /></el-icon>
              返回
            </el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="入库单号">
          {{ order.orderNo }}
        </el-descriptions-item>
        <el-descriptions-item label="供应商">
          {{ order.supplierName }}
        </el-descriptions-item>
        <el-descriptions-item label="订单号">
          {{ order.orderNumber || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(order.status)" size="small">
            {{ order.statusText }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ order.createTime }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ order.updateTime }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">
          {{ order.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 零件明细 -->
      <h3 style="margin: 16px 0 12px 0">零件明细</h3>
      <el-table :data="order.details" border stripe>
        <el-table-column prop="lineNo" label="行号" width="60" />
        <el-table-column prop="partCode" label="物料编码" min-width="120" />
        <el-table-column prop="partName" label="物料名称" min-width="120" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column label="包装容量" width="90" align="center">
          <template #default="{ row }">
            {{ row.packageCapacity || 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="boxCount" label="箱数" width="70" align="center" />
        <el-table-column prop="plannedQty" label="计划数量" width="100" />
        <el-table-column prop="actualQty" label="实入数量" width="100" />
        <el-table-column prop="warehouseAreaName" label="库区" width="120" />
        <el-table-column prop="batchNo" label="批次号" width="120" />
      </el-table>

      <!-- 扫描记录 -->
      <template v-if="scanList.length > 0">
        <h3 style="margin: 16px 0 12px 0">扫描记录</h3>
        <el-table :data="scanList" border stripe>
          <el-table-column prop="partCode" label="物料编码" width="120" />
          <el-table-column prop="partName" label="物料名称" width="120" />
          <el-table-column prop="batchNo" label="批次号" width="120" />
          <el-table-column prop="scanQty" label="扫描数量" width="100" />
          <el-table-column prop="scanTime" label="扫描时间" width="170" />
        </el-table>
      </template>
    </el-card>

    <!-- 打印对话框 -->
    <PrintDialog
      v-model:visible="showPrintDialog"
      :order="order"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Printer, Edit, Back } from '@element-plus/icons-vue'
import { getInboundDetailApi } from '@/api/inbound'
import { listScansApi } from '@/api/inbound'
import { useAuthStore } from '@/stores/auth'
import { InboundStatusText, InboundStatusTagType } from '@/types/inbound'
import PrintDialog from './components/PrintDialog.vue'
import type { InboundOrderVO, ScanRecord } from '@/types/inbound'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const showPrintDialog = ref(false)
const scanList = ref<ScanRecord[]>([])

const order = ref<InboundOrderVO>({
  id: 0,
  orderNo: '',
  supplierId: 0,
  supplierName: '',
  orderNumber: '',
  status: 0,
  statusText: '',
  remark: '',
  createTime: '',
  updateTime: '',
  details: [],
})

const canEdit = computed(() => {
  if (order.value.status === 3) return false
  if (order.value.status === 2) return authStore.role === 'admin'
  return true
})

function getStatusText(status: number): string {
  return InboundStatusText[status] || '未知'
}

function getStatusTagType(status: number): string {
  return InboundStatusTagType[status] || 'info'
}

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    const res = await getInboundDetailApi(id)
    order.value = res.data
  } catch { /* ignore */ }

  // 加载扫描记录
  try {
    const scanRes = await listScansApi(id)
    scanList.value = scanRes.data
  } catch { /* ignore */ }
})

function handleEdit() {
  router.push(`/inventory/inbound/edit/${order.value.id}`)
}

function handleBack() {
  router.back()
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
