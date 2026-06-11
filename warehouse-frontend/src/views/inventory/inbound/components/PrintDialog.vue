<template>
  <el-dialog
    :model-value="visible"
    title="看板标签打印"
    width="950px"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-tabs v-model="activeTab" v-loading="loading">
      <!-- 入库单看板 -->
      <el-tab-pane label="入库单看板" name="order">
        <div class="order-kanban-preview">
          <div id="order-kanban-print" class="order-kanban-card">
            <h2 style="text-align: center; margin: 0 0 12px 0;">入库单看板</h2>
            <table class="ok-info">
              <tr>
                <td class="ok-label">入库单号</td><td>{{ order.orderNo }}</td>
                <td class="ok-label">供应商</td><td>{{ order.supplierName }}</td>
              </tr>
              <tr>
                <td class="ok-label">创建时间</td><td>{{ order.createTime }}</td>
                <td class="ok-label">状态</td><td>{{ order.statusText }}</td>
              </tr>
              <tr>
                <td class="ok-label">物料总数</td><td class="ok-em">{{ totalQty }}</td>
                <td class="ok-label">零件种类</td><td>{{ order.details?.length || 0 }}</td>
              </tr>
            </table>
            <h4 style="margin: 12px 0 6px 0;">物料明细</h4>
            <table class="ok-detail">
              <thead>
                <tr>
                  <th>物料编码</th><th>物料名称</th><th>单位</th><th>包装容量</th><th>箱数</th><th>计划数量</th><th>库区</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="d in order.details" :key="d.lineNo">
                  <td>{{ d.partCode }}</td><td>{{ d.partName }}</td><td>{{ d.unit }}</td>
                  <td>{{ d.packageCapacity || 1 }}</td><td>{{ d.boxCount || 0 }}</td>
                  <td>{{ d.plannedQty }}</td><td>{{ d.warehouseAreaName || '-' }}</td>
                </tr>
              </tbody>
            </table>
            <div style="text-align: center; margin-top: 12px;">
              <canvas ref="orderQrRef" width="150" height="150"></canvas>
            </div>
          </div>
          <el-button type="primary" style="margin-top: 12px; width: 100%" @click="printOrderKanban">
            <el-icon><Printer /></el-icon>
            打印入库单看板
          </el-button>
        </div>
      </el-tab-pane>

      <!-- 零件看板 -->
      <el-tab-pane label="零件看板" name="part">
        <div v-if="kanbans.length === 0 && !loading" style="text-align: center; padding: 40px; color: #909399">
          该入库单暂无零件明细，请先添加零件
        </div>

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
                <canvas :ref="el => setPartQrRef(k.id, el)" width="130" height="130"></canvas>
              </div>
            </div>
          </div>
        </div>

        <template v-if="kanbans.length > 0">
          <div style="display: flex; gap: 8px; justify-content: space-between; margin-top: 12px;">
            <el-button @click="selectAll">{{ selectedIds.size === kanbans.length ? '取消全选' : '全选' }}</el-button>
            <el-button type="primary" @click="printSelectedParts" :disabled="selectedIds.size === 0">
              <el-icon><Printer /></el-icon>
              打印选中 ({{ selectedIds.size }})
            </el-button>
          </div>
        </template>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
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

const activeTab = ref('order')
const loading = ref(false)
const kanbans = ref<Kanban[]>([])
const selectedIds = ref(new Set<number>())
const partQrRefs = ref<Map<number, HTMLCanvasElement>>(new Map())
const orderQrRef = ref<HTMLCanvasElement>()

function setPartQrRef(id: number, el: any) {
  if (el) partQrRefs.value.set(id, el)
}

const totalQty = computed(() => {
  return (props.order.details || []).reduce((sum, d) => sum + (d.plannedQty || 0), 0)
})

watch(() => props.visible, async (val) => {
  if (val && props.order.id) {
    activeTab.value = 'order'
    loading.value = true
    selectedIds.value = new Set()

    // 生成入库单看板 QR 码
    await nextTick()
    if (orderQrRef.value) {
      const qrData = JSON.stringify({
        type: 'inbound-order',
        orderNo: props.order.orderNo,
        supplierName: props.order.supplierName,
        totalQty: totalQty.value,
        partCount: props.order.details?.length || 0,
      })
      await QRCode.toCanvas(orderQrRef.value, qrData, {
        width: 150, margin: 1,
        color: { dark: '#000', light: '#fff' },
      })
    }

    // 加载零件看板（优先从DB，没有则从订单详情生成）
    try {
      kanbans.value = await listKanbansByOrder(props.order.id).then(r => r.data)
    } catch { /* ignore */ }

    try {
      // DB没有看板数据时，从订单详情直接生成
      if (kanbans.value.length === 0 && props.order.details) {
        let genId = -1
        kanbans.value = []
        for (const d of props.order.details) {
          const boxCount = d.boxCount || 0
          const capacity = d.packageCapacity || 1
          for (let seq = 0; seq < boxCount; seq++) {
            const genNo = `${props.order.orderNo}-${d.partCode}C-${seq}`
            kanbans.value.push({
              id: genId--,
              kanbanNo: genNo,
              inboundOrderId: props.order.id,
              inboundOrderNo: props.order.orderNo,
              partId: d.partId,
              partCode: d.partCode,
              partName: d.partName,
              supplierName: props.order.supplierName,
              quantity: capacity,
              boxSeq: seq,
              warehouseAreaId: d.warehouseAreaId || 0,
              warehouseAreaName: d.warehouseAreaName || '',
              status: 0,
              createTime: '',
            })
          }
        }
      }

      selectedIds.value = new Set(kanbans.value.map(k => k.id))
      await nextTick()
      for (const k of kanbans.value) {
        const canvas = partQrRefs.value.get(k.id)
        if (canvas) {
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
        }
      }
    } catch { /* ignore */ }
    finally { loading.value = false }
  }
})

function toggleSelect(id: number) {
  const next = new Set(selectedIds.value)
  next.has(id) ? next.delete(id) : next.add(id)
  selectedIds.value = next
}

function selectAll() {
  selectedIds.value = selectedIds.value.size === kanbans.value.length
    ? new Set()
    : new Set(kanbans.value.map(k => k.id))
}

// ========== 打印入库单看板 ==========
function printOrderKanban() {
  const el = document.getElementById('order-kanban-print')
  if (!el) return
  const win = window.open('', '_blank', 'width=900,height=700')
  if (!win) return
  win.document.write(`
    <html><head><title>入库单看板 - ${props.order.orderNo}</title>
    <style>
      body { font-family: 'Microsoft YaHei', sans-serif; padding: 20px; }
      .ok-info { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
      .ok-info td { padding: 8px; border: 1px solid #333; }
      .ok-label { background: #f0f0f0; font-weight: bold; width: 15%; }
      .ok-em { font-size: 18px; font-weight: bold; color: #409eff; }
      .ok-detail { width: 100%; border-collapse: collapse; font-size: 12px; }
      .ok-detail th, .ok-detail td { padding: 4px 6px; border: 1px solid #333; text-align: center; }
      .ok-detail thead { background: #f0f0f0; }
      @media print { button { display: none; } }
    </style></head>
    <body>${el.innerHTML}</body></html>
  `)
  win.document.close()
  win.focus()
  setTimeout(() => win.print(), 500)
}

// ========== 打印选中零件看板 ==========
function printSelectedParts() {
  const selected = kanbans.value.filter(k => selectedIds.value.has(k.id))
  if (selected.length === 0) return
  const win = window.open('', '_blank', 'width=900,height=700')
  if (!win) return
  const cards = selected.map(k => `
    <div class="pc">
      <div class="pc-h"><span>${k.kanbanNo}</span></div>
      <div class="pc-b">
        <table class="pc-i">
          <tr><td class="l">零件号</td><td>${k.partCode}</td></tr>
          <tr><td class="l">零件名</td><td>${k.partName}</td></tr>
          <tr><td class="l">供应商</td><td>${k.supplierName}</td></tr>
          <tr><td class="l">库区</td><td>${k.warehouseAreaName || '-'}</td></tr>
          <tr><td class="l">数量</td><td class="q">${k.quantity}</td></tr>
          <tr><td class="l">箱号</td><td>C-${k.boxSeq}</td></tr>
        </table>
        <div class="pc-q"><img src="${getPartQrUrl(k.id)}" width="120" height="120" /></div>
      </div>
    </div>
  `).join('')
  win.document.write(`
    <html><head><title>零件看板打印</title>
    <style>
      body { font-family: 'Microsoft YaHei', sans-serif; padding: 10px; }
      .pc { display: inline-block; width: 380px; margin: 8px; padding: 10px; border: 2px solid #333; border-radius: 4px; page-break-inside: avoid; vertical-align: top; }
      .pc-h { border-bottom: 1px solid #999; padding-bottom: 6px; margin-bottom: 8px; font-weight: bold; font-size: 13px; }
      .pc-b { display: flex; gap: 12px; }
      .pc-i { border-collapse: collapse; flex: 1; }
      .pc-i td { padding: 2px 6px; font-size: 12px; }
      .pc-i .l { font-weight: bold; color: #666; width: 50px; }
      .pc-i .q { font-size: 16px; font-weight: bold; color: #409eff; }
      .pc-q { flex-shrink: 0; }
      @media print { .pc { border-color: #000; } }
    </style></head>
    <body>${cards}</body></html>
  `)
  win.document.close()
  win.focus()
  setTimeout(() => win.print(), 500)
}

function getPartQrUrl(id: number): string {
  const canvas = partQrRefs.value.get(id)
  return canvas ? canvas.toDataURL() : ''
}
</script>

<style scoped>
.order-kanban-preview {
  border: 1px solid #e4e7ed; padding: 16px; background: #fafafa;
}
.order-kanban-card { font-family: 'Microsoft YaHei', sans-serif; }
.ok-info { width: 100%; border-collapse: collapse; }
.ok-info td { padding: 6px 8px; border: 1px solid #ccc; }
.ok-label { background: #f0f0f0; font-weight: bold; width: 14%; }
.ok-em { font-size: 18px; font-weight: bold; color: #409eff; }
.ok-detail { width: 100%; border-collapse: collapse; margin-top: 8px; }
.ok-detail th, .ok-detail td { padding: 4px 6px; border: 1px solid #ccc; text-align: center; font-size: 12px; }
.ok-detail thead { background: #f0f0f0; }

.kanban-grid { display: flex; flex-wrap: wrap; gap: 12px; max-height: 450px; overflow-y: auto; }
.kanban-card { width: 400px; border: 2px solid #e4e7ed; border-radius: 6px; padding: 10px; cursor: pointer; transition: border-color 0.2s; }
.kanban-card:hover { border-color: #409eff; }
.kanban-card.selected { border-color: #409eff; background: #ecf5ff; }
.kanban-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #e4e7ed; padding-bottom: 6px; margin-bottom: 8px; }
.kanban-no { font-weight: bold; font-size: 13px; }
.kanban-body { display: flex; gap: 12px; }
.kanban-info { flex: 1; }
.info-row { display: flex; margin-bottom: 2px; font-size: 12px; }
.info-row .label { color: #909399; width: 42px; flex-shrink: 0; }
.info-row .qty { font-weight: bold; color: #409eff; font-size: 14px; }
.kanban-qr { flex-shrink: 0; }
</style>
