<template>
  <el-dialog
    :model-value="visible"
    title="看板标签打印"
    width="900px"
    @update:model-value="$emit('update:visible', $event)"
  >
    <div v-loading="loading">
      <div v-if="kanbans.length === 0 && !loading" style="text-align: center; padding: 40px; color: #909399">
        暂无看板数据，请先保存入库单
      </div>

      <!-- 看板标签网格 -->
      <div class="kanban-grid" v-if="kanbans.length > 0">
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
              <div class="info-row"><span class="label">供应商</span><span>{{ k.supplierName }}</span></div>
              <div class="info-row"><span class="label">库区</span><span>{{ k.warehouseAreaName || '-' }}</span></div>
              <div class="info-row"><span class="label">数量</span><span class="qty">{{ k.quantity }}</span></div>
              <div class="info-row"><span class="label">箱号</span><span>C-{{ k.boxSeq }}</span></div>
            </div>
            <div class="kanban-qr">
              <canvas :ref="el => setQrRef(k.id, el)" width="130" height="130"></canvas>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div style="display: flex; gap: 8px; justify-content: space-between; width: 100%">
        <el-button @click="selectAll">{{ selectedIds.size === kanbans.length ? '取消全选' : '全选' }}</el-button>
        <div>
          <el-button @click="$emit('update:visible', false)">关闭</el-button>
          <el-button type="primary" @click="printSelected" :disabled="selectedIds.size === 0">
            <el-icon><Printer /></el-icon>
            打印选中 ({{ selectedIds.size }})
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { Printer } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { listKanbansByOrder, type Kanban } from '@/api/kanban'
import type { InboundOrderVO } from '@/types/inbound'

const props = defineProps<{
  visible: boolean
  order: InboundOrderVO
}>()

defineEmits<{
  'update:visible': [value: boolean]
}>()

const loading = ref(false)
const kanbans = ref<Kanban[]>([])
const selectedIds = ref(new Set<number>())
const qrRefs = ref<Map<number, HTMLCanvasElement>>(new Map())

function setQrRef(id: number, el: any) {
  if (el) qrRefs.value.set(id, el)
}

watch(() => props.visible, async (val) => {
  if (val && props.order.id) {
    loading.value = true
    selectedIds.value = new Set()
    try {
      kanbans.value = await listKanbansByOrder(props.order.id).then(r => r.data)
      // 默认全选
      selectedIds.value = new Set(kanbans.value.map(k => k.id))
      // 生成 QR 码
      await nextTick()
      for (const k of kanbans.value) {
        const canvas = qrRefs.value.get(k.id)
        if (canvas) {
          const qrData = JSON.stringify({
            kanbanNo: k.kanbanNo,
            partCode: k.partCode,
            partName: k.partName,
            supplierName: k.supplierName,
            quantity: k.quantity,
            warehouseArea: k.warehouseAreaName,
            inboundOrderNo: k.inboundOrderNo,
            boxSeq: k.boxSeq,
          })
          await QRCode.toCanvas(canvas, qrData, {
            width: 130,
            margin: 1,
            color: { dark: '#000', light: '#fff' },
          })
        }
      }
    } catch { /* ignore */ }
    finally { loading.value = false }
  }
})

function toggleSelect(id: number) {
  const next = new Set(selectedIds.value)
  if (next.has(id)) next.delete(id); else next.add(id)
  selectedIds.value = next
}

function selectAll() {
  if (selectedIds.value.size === kanbans.value.length) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(kanbans.value.map(k => k.id))
  }
}

function printSelected() {
  const selected = kanbans.value.filter(k => selectedIds.value.has(k.id))
  if (selected.length === 0) return

  const win = window.open('', '_blank', 'width=900,height=700')
  if (!win) return

  const cards = selected.map(k => `
    <div class="print-card">
      <div class="print-header">
        <span class="print-no">${k.kanbanNo}</span>
      </div>
      <div class="print-body">
        <table class="print-info">
          <tr><td class="lbl">零件号</td><td>${k.partCode}</td></tr>
          <tr><td class="lbl">零件名</td><td>${k.partName}</td></tr>
          <tr><td class="lbl">供应商</td><td>${k.supplierName}</td></tr>
          <tr><td class="lbl">库区</td><td>${k.warehouseAreaName || '-'}</td></tr>
          <tr><td class="lbl">数量</td><td class="qty">${k.quantity}</td></tr>
          <tr><td class="lbl">箱号</td><td>C-${k.boxSeq}</td></tr>
        </table>
        <div class="print-qr"><img src="${getQrDataUrl(k.id)}" width="120" height="120" /></div>
      </div>
    </div>
  `).join('')

  win.document.write(`
    <html>
      <head>
        <title>看板标签打印</title>
        <style>
          body { font-family: 'Microsoft YaHei', sans-serif; padding: 10px; }
          .print-card {
            display: inline-block; width: 380px; margin: 8px; padding: 10px;
            border: 2px solid #333; border-radius: 4px; page-break-inside: avoid; vertical-align: top;
          }
          .print-header { border-bottom: 1px solid #999; padding-bottom: 6px; margin-bottom: 8px; font-weight: bold; font-size: 13px; }
          .print-body { display: flex; gap: 12px; }
          .print-info { border-collapse: collapse; flex: 1; }
          .print-info td { padding: 2px 6px; font-size: 12px; }
          .print-info .lbl { font-weight: bold; color: #666; width: 50px; }
          .print-info .qty { font-size: 16px; font-weight: bold; color: #409eff; }
          .print-qr { flex-shrink: 0; }
          @media print {
            button { display: none; }
            .print-card { border-color: #000; }
          }
        </style>
      </head>
      <body>${cards}</body>
    </html>
  `)
  win.document.close()
  win.focus()
  setTimeout(() => win.print(), 500)
}

function getQrDataUrl(id: number): string {
  const canvas = qrRefs.value.get(id)
  return canvas ? canvas.toDataURL() : ''
}
</script>

<style scoped>
.kanban-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  max-height: 500px;
  overflow-y: auto;
}
.kanban-card {
  width: 400px;
  border: 2px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px;
  cursor: pointer;
  transition: border-color 0.2s;
}
.kanban-card:hover { border-color: #409eff; }
.kanban-card.selected { border-color: #409eff; background: #ecf5ff; }
.kanban-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 6px;
  margin-bottom: 8px;
}
.kanban-no { font-weight: bold; font-size: 13px; }
.kanban-body { display: flex; gap: 12px; }
.kanban-info { flex: 1; }
.info-row { display: flex; margin-bottom: 2px; font-size: 12px; }
.info-row .label { color: #909399; width: 42px; flex-shrink: 0; }
.info-row .qty { font-weight: bold; color: #409eff; font-size: 14px; }
.kanban-qr { flex-shrink: 0; }
</style>
