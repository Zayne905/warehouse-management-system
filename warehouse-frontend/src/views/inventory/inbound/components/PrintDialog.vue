<template>
  <el-dialog
    :model-value="visible"
    title="打印"
    width="550px"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-tabs v-model="activeTab">
      <!-- 入库单打印 -->
      <el-tab-pane label="入库单" name="order">
        <div class="print-preview">
          <div id="print-content" class="print-doc">
            <h2 style="text-align: center; margin-bottom: 16px">入库单</h2>
            <table class="info-table">
              <tr>
                <td class="label">入库单号:</td>
                <td>{{ order.orderNo }}</td>
                <td class="label">供应商:</td>
                <td>{{ order.supplierName }}</td>
              </tr>
              <tr>
                <td class="label">订单号:</td>
                <td>{{ order.orderNumber || '-' }}</td>
                <td class="label">状态:</td>
                <td>{{ order.statusText }}</td>
              </tr>
              <tr>
                <td class="label">创建时间:</td>
                <td>{{ order.createTime }}</td>
                <td class="label">备注:</td>
                <td>{{ order.remark || '-' }}</td>
              </tr>
            </table>
            <table class="detail-table" style="margin-top: 16px">
              <thead>
                <tr>
                  <th>行号</th>
                  <th>物料编码</th>
                  <th>物料名称</th>
                  <th>单位</th>
                  <th>计划数量</th>
                  <th>实入数量</th>
                  <th>库区</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="d in order.details" :key="d.lineNo">
                  <td>{{ d.lineNo }}</td>
                  <td>{{ d.partCode }}</td>
                  <td>{{ d.partName }}</td>
                  <td>{{ d.unit }}</td>
                  <td>{{ d.plannedQty }}</td>
                  <td>{{ d.actualQty }}</td>
                  <td>{{ d.warehouseAreaName || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <el-button type="primary" style="margin-top: 12px; width: 100%" @click="printOrder">
          <el-icon><Printer /></el-icon>
          打印入库单
        </el-button>
      </el-tab-pane>

      <!-- 二维码看板 -->
      <el-tab-pane label="二维码看板" name="qr">
        <div class="qr-preview" style="text-align: center">
          <div id="qr-code" style="display: inline-block; padding: 20px; background: #fff">
            <div ref="qrRef" style="width: 200px; height: 200px; margin: 0 auto"></div>
            <div style="margin-top: 12px; font-size: 16px; font-weight: bold">
              {{ order.orderNo }}
            </div>
            <div style="margin-top: 4px; color: #909399">
              {{ order.supplierName }}
            </div>
          </div>
        </div>
        <el-button type="primary" style="margin-top: 12px; width: 100%" @click="printQr">
          <el-icon><Printer /></el-icon>
          打印二维码看板
        </el-button>
      </el-tab-pane>
    </el-tabs>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { Printer } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import type { InboundOrderVO } from '@/types/inbound'

const props = defineProps<{
  visible: boolean
  order: InboundOrderVO
}>()

defineEmits<{
  'update:visible': [value: boolean]
}>()

const activeTab = ref('order')
const qrRef = ref<HTMLElement>()

watch(() => props.visible, async (val) => {
  if (val) {
    await nextTick()
    if (qrRef.value) {
      qrRef.value.innerHTML = ''
      const canvas = document.createElement('canvas')
      await QRCode.toCanvas(canvas, props.order.orderNo, {
        width: 200,
        margin: 1,
        color: { dark: '#000', light: '#fff' },
      })
      qrRef.value.appendChild(canvas)
    }
  }
})

function printOrder() {
  const el = document.getElementById('print-content')
  if (!el) return
  const win = window.open('', '_blank', 'width=800,height=600')
  if (!win) return
  win.document.write(`
    <html>
      <head>
        <title>入库单 - ${props.order.orderNo}</title>
        <style>
          body { font-family: 'Microsoft YaHei', sans-serif; padding: 20px; }
          .info-table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
          .info-table td { padding: 8px; border: 1px solid #333; }
          .info-table .label { background: #f0f0f0; font-weight: bold; width: 15%; }
          .detail-table { width: 100%; border-collapse: collapse; }
          .detail-table th, .detail-table td { padding: 8px; border: 1px solid #333; text-align: center; }
          .detail-table thead { background: #f0f0f0; }
          @media print { button { display: none; } }
        </style>
      </head>
      <body>${el.innerHTML}</body>
    </html>
  `)
  win.document.close()
  win.focus()
  setTimeout(() => win.print(), 500)
}

function printQr() {
  const el = document.getElementById('qr-code')
  if (!el) return
  const win = window.open('', '_blank', 'width=400,height=450')
  if (!win) return
  win.document.write(`
    <html>
      <head>
        <title>二维码看板 - ${props.order.orderNo}</title>
        <style>
          body { font-family: 'Microsoft YaHei', sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
          @media print { body { margin: 0; } }
        </style>
      </head>
      <body>${el.innerHTML}</body>
    </html>
  `)
  win.document.close()
  win.focus()
  setTimeout(() => win.print(), 500)
}
</script>

<style scoped>
.print-preview {
  border: 1px solid #e4e7ed;
  padding: 16px;
  background: #fafafa;
}
.print-doc {
  font-family: 'Microsoft YaHei', sans-serif;
}
.info-table {
  width: 100%;
  border-collapse: collapse;
}
.info-table td {
  padding: 6px 8px;
  border: 1px solid #ccc;
}
.info-table .label {
  background: #f0f0f0;
  font-weight: bold;
  width: 15%;
}
.detail-table {
  width: 100%;
  border-collapse: collapse;
}
.detail-table th,
.detail-table td {
  padding: 6px 8px;
  border: 1px solid #ccc;
  text-align: center;
}
.detail-table thead {
  background: #f0f0f0;
}
</style>
