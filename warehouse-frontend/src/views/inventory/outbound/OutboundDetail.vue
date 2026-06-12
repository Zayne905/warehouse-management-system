<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>出库单详情</span>
          <div>
            <el-button size="small" @click="printOrder"><el-icon><Printer /></el-icon>打印</el-button>
            <el-button size="small" v-if="order.status === 0 || order.status === 1" type="success" @click="showScanDialog = true"><el-icon><Search /></el-icon>手动出库</el-button>
            <el-button size="small" @click="router.back()"><el-icon><Back /></el-icon>返回</el-button>
          </div>
        </div>
      </template>

      <!-- 出库单信息 -->
      <el-descriptions :column="3" border>
        <el-descriptions-item label="出库单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="OutboundStatusTagType[order.status] || 'info'" size="small">{{ order.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ order.suppliers?.join('、') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ order.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 进度条 -->
      <div v-if="totalKanbans > 0" style="margin: 16px 0">
        <div style="display:flex;justify-content:space-between;margin-bottom:4px">
          <span>出库进度</span>
          <span style="color:#409eff;font-weight:bold">{{ outboundCount }} / {{ totalKanbans }} 箱</span>
        </div>
        <el-progress :percentage="progressPercent" :status="progressPercent === 100 ? 'success' : undefined" :stroke-width="20" />
      </div>

      <!-- 零件汇总 -->
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

      <!-- 待出库清单 -->
      <template v-if="pendingKanbans.length > 0">
        <h3 style="margin:16px 0 12px">
          待出库清单（{{ pendingKanbans.length }} 箱）
          <el-tag size="small" type="warning" style="margin-left:8px">待出库</el-tag>
          <el-button size="small" type="primary" style="margin-left:12px" @click="showOutboundPrint = true">
            <el-icon><Printer /></el-icon>
            打印出库看板
          </el-button>
        </h3>
        <el-table :data="pendingKanbans" border stripe :row-class-name="pendingRowClass">
          <el-table-column label="二维码" width="120" align="center">
            <template #default="{ row }">
              <canvas :ref="el => setKanbanQrRef(row.id, el)" width="100" height="100" style="display:block;margin:0 auto" />
            </template>
          </el-table-column>
          <el-table-column prop="kanbanNo" label="看板号" min-width="200" />
          <el-table-column prop="boxSeq" label="箱号" width="70" align="center">
            <template #default="{ row }">C-{{ row.boxSeq }}</template>
          </el-table-column>
          <el-table-column prop="partCode" label="物料编码" width="120" />
          <el-table-column prop="partName" label="物料名称" width="120" />
          <el-table-column prop="quantity" label="箱数量" width="80" align="center" />
          <el-table-column prop="warehouseAreaName" label="库位" width="120" />
          <el-table-column prop="createTime" label="入库时间" width="170" />
        </el-table>
      </template>

      <!-- 已出库记录 -->
      <template v-if="scans.length > 0">
        <h3 style="margin:16px 0 12px">
          出库记录（{{ scans.length }} 箱）
          <el-tag size="small" type="success" style="margin-left:8px">已出库</el-tag>
        </h3>
        <el-table :data="scans" border stripe>
          <el-table-column prop="kanbanNo" label="看板号" min-width="220" />
          <el-table-column prop="partCode" label="物料编码" width="120" />
          <el-table-column prop="partName" label="物料名称" width="120" />
          <el-table-column prop="quantity" label="出库数量" width="100" />
          <el-table-column prop="warehouseAreaName" label="出库库位" width="120" />
          <el-table-column prop="scanTime" label="出库时间" width="170" />
        </el-table>
      </template>
    </el-card>

    <!-- 手动出库对话框 -->
    <el-dialog v-model="showScanDialog" title="手动出库" width="500px" @closed="onScanDialogClosed">
      <el-form label-width="80px">
        <el-form-item label="看板号">
          <el-input v-model="scanKanbanNo" placeholder="请输入看板号" @keyup.enter="doScanOutbound" ref="scanInputRef" />
        </el-form-item>
      </el-form>
      <div v-if="scanResult" style="margin-top:12px">
        <el-alert :title="scanResult" :type="scanError ? 'error' : 'success'" :closable="false" />
      </div>
      <div v-if="scanAllDone" style="margin-top:12px">
        <el-alert title="所有待出库条码已全部出库，出库单已完成。" type="success" :closable="false" />
      </div>
      <template #footer>
        <el-button @click="showScanDialog = false">关闭</el-button>
        <el-button type="primary" @click="doScanOutbound" :loading="scanning" :disabled="scanAllDone">确认出库</el-button>
      </template>
    </el-dialog>

    <!-- 出库看板打印对话框 -->
    <OutboundPrintDialog v-model:visible="showOutboundPrint" :kanbans="pendingKanbans" :order="order" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Search, Printer } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { getOutboundDetailApi, scanOutboundApi, OutboundStatusTagType } from '@/api/outbound'
import OutboundPrintDialog from './components/OutboundPrintDialog.vue'

const route = useRoute(); const router = useRouter()
const showScanDialog = ref(false); const scanning = ref(false)
const scanKanbanNo = ref(''); const scanResult = ref(''); const scanError = ref(false)
const scanAllDone = ref(false)
const scanInputRef = ref<any>(null)
const showOutboundPrint = ref(false)
const scans = ref<any[]>([])
const pendingKanbans = ref<any[]>([])
const kanbanQrRefs = ref<Map<number, HTMLCanvasElement>>(new Map())

function setKanbanQrRef(id: number, el: any) {
  if (el) kanbanQrRefs.value.set(id, el)
}

/** 为每个看板渲染二维码（与入库打印标签格式一致） */
async function renderKanbanQRCodes(kbList: any[]) {
  // 等 Vue 渲染完 DOM，确保 canvas ref 全部挂载
  await nextTick()
  await new Promise(r => setTimeout(r, 50))
  for (const k of kbList) {
    const canvas = kanbanQrRefs.value.get(k.id)
    if (canvas) {
      try {
        await QRCode.toCanvas(canvas, JSON.stringify({
          kanbanNo: k.kanbanNo,
          partCode: k.partCode,
          partName: k.partName,
          supplierName: k.supplierName,
          quantity: k.quantity,
          warehouseArea: k.warehouseAreaName,
          inboundOrderNo: k.inboundOrderNo,
          boxSeq: k.boxSeq,
        }), { width: 100, margin: 1, color: { dark: '#000', light: '#fff' } })
      } catch { /* ignore */ }
    }
  }
}

// 监听待出库清单变化，自动渲染二维码
watch(pendingKanbans, (list) => {
  if (list.length > 0) renderKanbanQRCodes(list)
})

const order = ref<any>({ id: 0, orderNo: '', status: 0, statusText: '', remark: '', createTime: '', details: [] })
const outboundCount = ref(0)
const totalKanbans = ref(0)

const progressPercent = computed(() => {
  if (totalKanbans.value === 0) return 0
  return Math.round((outboundCount.value / totalKanbans.value) * 100)
})

async function loadData() {
  try {
    const res = await getOutboundDetailApi(Number(route.params.id))
    const o = res.data
    order.value = o
    scans.value = o.scans || []
    pendingKanbans.value = o.pendingKanbans || []
    outboundCount.value = o.outboundCount || 0
    totalKanbans.value = o.totalKanbans || 0
    // 全部出库才算完成（status=2 已出库，或待出库清单为空）
    if (o.status === 2 || (totalKanbans.value > 0 && pendingKanbans.value.length === 0)) {
      scanAllDone.value = true
    } else {
      scanAllDone.value = false
    }
  } catch { /* */ }
}

onMounted(loadData)

function pendingRowClass({ row }: { row: any }) {
  // 如果看板号在已扫描记录中，标记为已出库
  const scanned = scans.value.some(s => s.kanbanNo === row.kanbanNo)
  return scanned ? 'scanned-row' : ''
}

function printOrder() {
  const o = order.value; const details = o.details || []
  const totalQty = details.reduce((s: number, d: any) => s + (d.plannedQty || 0), 0)
  const rows = details.map((d: any) =>
    `<tr><td>${d.partCode}</td><td>${d.partName}</td><td>${d.unit}</td><td>${d.plannedQty}</td><td>${d.actualQty||0}</td><td>${d.boxCount||0}</td></tr>`).join('')
  const win = window.open('', '_blank', 'width=800,height=600')
  if (!win) return
  win.document.write(`<html><head><title>出库单-${o.orderNo}</title>
    <style>body{font-family:'Microsoft YaHei',sans-serif;padding:20px}h2{text-align:center}
    .info{width:100%;border-collapse:collapse;margin-bottom:16px}.info td{padding:8px;border:1px solid #333}
    .info .lbl{background:#f0f0f0;font-weight:bold;width:14%}
    .dt{width:100%;border-collapse:collapse;font-size:12px}.dt th,.dt td{padding:4px 6px;border:1px solid #333;text-align:center}
    .dt thead{background:#f0f0f0}@media print{button{display:none}}</style></head><body>
    <h2>出库单</h2>
    <table class="info"><tr><td class="lbl">出库单号</td><td>${o.orderNo}</td><td class="lbl">状态</td><td>${o.statusText}</td></tr>
    <tr><td class="lbl">创建时间</td><td>${o.createTime}</td><td class="lbl">计划总数</td><td style="font-weight:bold;color:#409eff">${totalQty}</td></tr></table>
    <h4>零件明细</h4>
    <table class="dt"><thead><tr><th>物料编码</th><th>物料名称</th><th>单位</th><th>计划出库</th><th>实出</th><th>箱数</th></tr></thead><tbody>${rows}</tbody></table>
    <p style="text-align:center;margin-top:16px"><button onclick="window.print()" style="padding:10px 40px;font-size:16px">打印</button></p>
  </body></html>`)
  win.document.close(); win.focus()
}

async function doScanOutbound() {
  const kanbanNo = scanKanbanNo.value.trim()
  if (!kanbanNo) { ElMessage.warning('请输入看板号'); return }
  scanning.value = true; scanResult.value = ''; scanError.value = false
  try {
    const res = await scanOutboundApi({
      orderId: order.value.id,
      kanbanNo: kanbanNo,
      operatorId: 1
    })
    const d = res.data
    // 在待出库清单中查找匹配行，准备高亮
    const match = pendingKanbans.value.find(k => k.kanbanNo === d.kanbanNo)
    const boxInfo = match ? `箱号 C-${match.boxSeq}` : ''
    scanResult.value = `${d.partName} x${d.quantity} 已出库 ${boxInfo}`
    scanError.value = false
    scanKanbanNo.value = '' // 清空准备下一次扫描

    // 重新加载数据
    await loadData()

    // 聚焦输入框
    await nextTick()
    scanInputRef.value?.focus()
  } catch (e: any) {
    scanResult.value = e?.message || '出库失败'; scanError.value = true
  } finally { scanning.value = false }
}

function onScanDialogClosed() {
  scanKanbanNo.value = ''
  scanResult.value = ''
  scanError.value = false
}
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
:deep(.scanned-row) {
  background-color: #f0f9eb;
  text-decoration: line-through;
  color: #909399;
}
</style>
