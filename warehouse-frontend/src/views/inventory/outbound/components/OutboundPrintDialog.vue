<template>
  <el-dialog
    :model-value="visible"
    title="出库看板打印"
    width="950px"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div v-if="kanbans.length === 0" style="text-align: center; padding: 40px; color: #909399">
      暂无待出库看板
    </div>

    <div class="kanban-grid" v-if="kanbans.length > 0" v-loading="loading">
      <div
        v-for="k in kanbans"
        :key="k.id"
        class="kanban-card"
        :class="{ selected: selectedIds.has(k.id) }"
        @click="toggleSelect(k.id)"
      >
        <div class="kanban-header">
          <span class="kanban-no">{{ k.kanbanNo }}</span>
          <el-checkbox :model-value="selectedIds.has(k.id)" @click.stop="toggleSelect(k.id)" />
        </div>
        <div class="kanban-body">
          <div class="kanban-info">
            <div class="info-row"><span class="label">零件号</span><span>{{ k.partCode }}</span></div>
            <div class="info-row"><span class="label">零件名</span><span>{{ k.partName }}</span></div>
            <div class="info-row"><span class="label">出库单号</span><span>{{ order?.orderNo || '-' }}</span></div>
            <div class="info-row"><span class="label">库位</span><span>{{ k.warehouseAreaName || '-' }}</span></div>
            <div class="info-row"><span class="label">数量</span><span class="qty">{{ k.quantity }}</span></div>
            <div class="info-row"><span class="label">箱号</span><span>C-{{ k.boxSeq }}</span></div>
          </div>
          <div class="kanban-qr">
            <canvas :ref="el => setQrCanvasRef(k.id, el)" width="130" height="130"></canvas>
          </div>
        </div>
      </div>
    </div>

    <template v-if="kanbans.length > 0">
      <div style="display: flex; gap: 8px; justify-content: space-between; margin-top: 12px;">
        <el-button @click="selectAll">{{ selectedIds.size === kanbans.length ? '取消全选' : '全选' }}</el-button>
        <el-button type="primary" @click="printSelected" :disabled="selectedIds.size === 0">
          <el-icon><Printer /></el-icon>
          打印选中 ({{ selectedIds.size }})
        </el-button>
      </div>
    </template>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { Printer } from '@element-plus/icons-vue'
import QRCode from 'qrcode'

const props = defineProps<{
  visible: boolean
  kanbans: any[]
  order: any
}>()

defineEmits<{
  'update:visible': [value: boolean]
}>()

const loading = ref(false)
const selectedIds = ref(new Set<number>())
const qrCanvasRefs = ref<Map<number, HTMLCanvasElement>>(new Map())

function setQrCanvasRef(id: number, el: any) {
  if (el) qrCanvasRefs.value.set(id, el)
}

function toggleSelect(id: number) {
  const s = new Set(selectedIds.value)
  if (s.has(id)) s.delete(id); else s.add(id)
  selectedIds.value = s
}

function selectAll() {
  if (selectedIds.value.size === props.kanbans.length) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(props.kanbans.map(k => k.id))
  }
}

// 对话框打开时渲染二维码
watch(() => props.visible, async (val) => {
  if (!val) return
  loading.value = true
  selectedIds.value = new Set(props.kanbans.map(k => k.id))
  await nextTick()
  for (const k of props.kanbans) {
    const canvas = qrCanvasRefs.value.get(k.id)
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
        }), { width: 130, margin: 1, color: { dark: '#000', light: '#fff' } })
      } catch { /* ignore */ }
    }
  }
  loading.value = false
})

function getQrImageUrl(id: number): string {
  const canvas = qrCanvasRefs.value.get(id)
  return canvas ? canvas.toDataURL() : ''
}

function printSelected() {
  const selected = props.kanbans.filter(k => selectedIds.has(k.id))
  if (selected.length === 0) return

  const orderNo = props.order?.orderNo || ''
  const cards = selected.map(k => {
    const qrUrl = getQrImageUrl(k.id)
    return `<div class="card">
      <div class="qr"><img src="${qrUrl}" width="120" height="120" /></div>
      <div class="info">
        <div class="line"><b>看板号:</b> ${k.kanbanNo}</div>
        <div class="line"><b>出库单号:</b> ${orderNo}</div>
        <div class="line"><b>零件:</b> ${k.partCode} ${k.partName}</div>
        <div class="line"><b>箱号:</b> C-${k.boxSeq} &nbsp; <b>数量:</b> ${k.quantity}</div>
        <div class="line"><b>库位:</b> ${k.warehouseAreaName || '-'}</div>
      </div>
    </div>`
  }).join('')

  const win = window.open('', '_blank', 'width=800,height=600')
  if (!win) return
  win.document.write(`<html><head><title>出库看板标签-${orderNo}</title>
    <style>
      body { font-family: 'Microsoft YaHei', sans-serif; padding: 20px; }
      .grid { display: flex; flex-wrap: wrap; gap: 16px; }
      .card { width: 380px; border: 2px dashed #333; padding: 12px; display: flex; gap: 12px; break-inside: avoid; page-break-inside: avoid; margin-bottom: 16px; }
      .card .qr { flex-shrink: 0; }
      .card .info { font-size: 13px; }
      .card .line { margin-bottom: 4px; }
      @media print { button { display: none; } .card { border-color: #000; } }
    </style></head><body>
      <h2 style="text-align:center">出库看板标签</h2>
      <p style="text-align:center;color:#666">出库单号: ${orderNo} &nbsp;|&nbsp; 共 ${selected.length} 箱</p>
      <div class="grid">${cards}</div>
      <p style="text-align:center;margin-top:16px"><button onclick="window.print()" style="padding:10px 40px;font-size:16px">打印</button></p>
    </body></html>`)
  win.document.close()
  win.focus()
}
</script>

<style scoped>
.kanban-grid { display: flex; flex-wrap: wrap; gap: 12px; max-height: 500px; overflow-y: auto; }
.kanban-card { width: 440px; border: 2px solid #e0e0e0; border-radius: 8px; padding: 10px; cursor: pointer; transition: border-color .2s; }
.kanban-card.selected { border-color: #409eff; background: #f0f7ff; }
.kanban-card:hover { border-color: #b0d0ff; }
.kanban-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.kanban-no { font-family: 'Courier New', monospace; font-weight: bold; font-size: 13px; }
.kanban-body { display: flex; gap: 12px; }
.kanban-info { flex: 1; }
.info-row { display: flex; justify-content: space-between; margin-bottom: 2px; font-size: 12px; }
.info-row .label { color: #909399; }
.info-row .qty { font-weight: bold; color: #409eff; }
.kanban-qr { flex-shrink: 0; }
</style>
