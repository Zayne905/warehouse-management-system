<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>出库单详情</span>
          <div>
            <el-button size="small" v-if="order.status !== 2" type="success" @click="showScanDialog = true"><el-icon><Scan /></el-icon>扫码出库</el-button>
            <el-button size="small" @click="router.back()"><el-icon><Back /></el-icon>返回</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="出库单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="['info','warning','success','danger'][order.status]" size="small">{{ order.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h3 style="margin:16px 0 12px">零件明细</h3>
      <el-table :data="order.details" border stripe>
        <el-table-column prop="lineNo" label="行号" width="60" />
        <el-table-column prop="partCode" label="物料编码" width="120" />
        <el-table-column prop="partName" label="物料名称" min-width="140" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column prop="availableStock" label="可用库存" width="90" align="center" />
        <el-table-column prop="plannedQty" label="计划出库" width="100" />
        <el-table-column prop="actualQty" label="实出数量" width="100" />
        <el-table-column prop="boxCount" label="箱数" width="70" align="center" />
      </el-table>

      <template v-if="scans.length > 0">
        <h3 style="margin:16px 0 12px">出库记录</h3>
        <el-table :data="scans" border stripe>
          <el-table-column prop="kanbanNo" label="看板号" min-width="180" />
          <el-table-column prop="partCode" label="物料编码" width="120" />
          <el-table-column prop="partName" label="物料名称" width="120" />
          <el-table-column prop="quantity" label="出库数量" width="100" />
          <el-table-column prop="scanTime" label="时间" width="170" />
        </el-table>
      </template>
    </el-card>

    <!-- 扫码出库对话框 -->
    <el-dialog v-model="showScanDialog" title="扫码出库" width="500px">
      <el-form label-width="80px">
        <el-form-item label="看板号">
          <el-input v-model="scanKanbanNo" placeholder="扫描或输入看板号" @keyup.enter="doScanOutbound" />
        </el-form-item>
      </el-form>
      <div v-if="scanResult" style="margin-top:12px">
        <el-alert :title="scanResult" :type="scanError ? 'error' : 'success'" :closable="false" />
      </div>
      <template #footer>
        <el-button @click="showScanDialog = false">关闭</el-button>
        <el-button type="primary" @click="doScanOutbound" :loading="scanning">确认出库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Scan } from '@element-plus/icons-vue'
import { getOutboundDetailApi, scanOutboundApi } from '@/api/outbound'

const route = useRoute(); const router = useRouter()
const showScanDialog = ref(false); const scanning = ref(false)
const scanKanbanNo = ref(''); const scanResult = ref(''); const scanError = ref(false)
const scans = ref<any[]>([])

const order = ref<any>({ id: 0, orderNo: '', status: 0, statusText: '', remark: '', createTime: '', details: [] })

async function loadData() {
  try {
    const res = await getOutboundDetailApi(Number(route.params.id))
    const o = res.data; order.value = o; scans.value = o.scans || []
  } catch { /* */ }
}

onMounted(loadData)

async function doScanOutbound() {
  if (!scanKanbanNo.value.trim()) { ElMessage.warning('请输入看板号'); return }
  scanning.value = true; scanResult.value = ''; scanError.value = false
  try {
    const res = await scanOutboundApi({ orderId: order.value.id, kanbanNo: scanKanbanNo.value.trim(), operatorId: 1 })
    scanResult.value = `出库成功: ${res.data.partName} ×${res.data.quantity}`
    scanKanbanNo.value = ''
    loadData()
  } catch (e: any) {
    scanResult.value = e?.message || '出库失败'; scanError.value = true
  } finally { scanning.value = false }
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
