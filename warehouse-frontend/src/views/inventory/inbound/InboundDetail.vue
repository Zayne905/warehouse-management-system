<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>入库单详情</span>
          <div>
            <el-button
              v-if="canReceive"
              size="small"
              type="success"
              @click="openReceiveCart"
            >
              <el-icon><ShoppingCart /></el-icon>
              手动入库
            </el-button>
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
          <el-table-column prop="kanbanNo" label="看板号/箱号" min-width="180" />
          <el-table-column prop="partCode" label="物料编码" width="120" />
          <el-table-column prop="partName" label="物料名称" width="120" />
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

    <!-- 收货车对话框 -->
    <el-dialog v-model="showReceiveCart" title="收货车" width="700px">
      <el-table :data="cartItems" border stripe>
        <el-table-column prop="partCode" label="物料编码" width="100" />
        <el-table-column prop="partName" label="物料名称" width="120" />
        <el-table-column label="计划数量" width="80" align="center">
          <template #default="{ row }">{{ row.plannedQty }}</template>
        </el-table-column>
        <el-table-column label="已收数量" width="80" align="center">
          <template #default="{ row }">{{ row.actualQty }}</template>
        </el-table-column>
        <el-table-column label="剩余" width="80" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.remaining <= 0 ? '#67c23a' : '#e6a23c' }">{{ row.remaining }}</span>
          </template>
        </el-table-column>
        <el-table-column label="本次收货" width="130">
          <template #default="{ row }">
            <el-input-number
              v-model="row.receiveQty"
              :min="0"
              :max="row.remaining"
              :precision="0"
              size="small"
              controls-position="right"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="看板号" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.kanbanNo" placeholder="可选" size="small" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showReceiveCart = false">取消</el-button>
        <el-button type="primary" @click="submitReceive" :loading="receiving" :disabled="!hasReceiveQty">
          确认收货
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Printer, Edit, Back, ShoppingCart } from '@element-plus/icons-vue'
import { getInboundDetailApi, listScansApi, submitScanApi } from '@/api/inbound'
import { useAuthStore } from '@/stores/auth'
import { InboundStatusText, InboundStatusTagType } from '@/types/inbound'
import PrintDialog from './components/PrintDialog.vue'
import type { InboundOrderVO, ScanRecord } from '@/types/inbound'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const showPrintDialog = ref(false)
const scanList = ref<ScanRecord[]>([])
const showReceiveCart = ref(false)
const receiving = ref(false)

// 收货车条目
interface CartItem {
  detailId: number
  partId: number
  partCode: string
  partName: string
  plannedQty: number
  actualQty: number
  remaining: number
  receiveQty: number
  kanbanNo: string
}
const cartItems = ref<CartItem[]>([])

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
  if (order.value.status === 3 || order.value.status === 2) return false
  return true
})

const canReceive = computed(() => {
  return order.value.status !== 3 && order.value.details && order.value.details.length > 0
})

const hasReceiveQty = computed(() => {
  return cartItems.value.some(item => item.receiveQty > 0)
})

// 打开收货车时初始化
function openReceiveCart() {
  if (order.value.details) {
    cartItems.value = order.value.details.map(d => ({
      detailId: d.id,
      partId: d.partId,
      partCode: d.partCode,
      partName: d.partName,
      plannedQty: d.plannedQty,
      actualQty: d.actualQty,
      remaining: Math.max(0, d.plannedQty - d.actualQty),
      receiveQty: 0,
      kanbanNo: '',
    }))
  }
  showReceiveCart.value = true
}

async function submitReceive() {
  receiving.value = true
  let ok = 0; let fail = 0
  for (const item of cartItems.value) {
    if (item.receiveQty <= 0) continue
    try {
      await submitScanApi({
        inboundOrderId: order.value.id,
        inboundOrderNo: order.value.orderNo,
        partCode: item.partCode,
        batchNo: item.kanbanNo || '',
        scanQty: item.receiveQty,
        operatorId: 1,
      })
      ok++
    } catch { fail++ }
  }
  receiving.value = false
  showReceiveCart.value = false
  if (ok > 0) {
    ElMessage.success(`收货完成: 成功 ${ok} 条` + (fail > 0 ? `, 失败 ${fail} 条` : ''))
    // 刷新数据
    try {
      const res = await getInboundDetailApi(order.value.id)
      order.value = res.data
    } catch { /* ignore */ }
    try {
      const scanRes = await listScansApi(order.value.id)
      scanList.value = scanRes.data
    } catch { /* ignore */ }
  } else {
    ElMessage.error('收货失败')
  }
}

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
