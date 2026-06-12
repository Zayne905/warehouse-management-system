<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="出库单号">
          <el-input v-model="query.orderNo" placeholder="输入出库单号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="未出库" :value="0" />
            <el-option label="部分出库" :value="1" />
            <el-option label="已出库" :value="2" />
            <el-option label="作废" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="query.supplier" placeholder="供应商名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="客户">
          <el-input v-model="query.customerName" placeholder="客户名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="handleCreate"><el-icon><Plus /></el-icon>新增出库单</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="orderNo" label="出库单号" min-width="160" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="OutboundStatusTagType[row.status] || 'info'" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="partCount" label="零件种类" width="80" align="center" />
        <el-table-column label="供应商" min-width="120">
          <template #default="{ row }">
            <span v-if="row.suppliers && row.suppliers.length">{{ [...new Set(row.suppliers)].join('、') }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" width="120">
          <template #default="{ row }">{{ row.customerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="totalQty" label="计划总数" width="100" align="center" />
        <el-table-column label="进度" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.totalKanbans">{{ row.outboundCount || 0 }}/{{ row.totalKanbans }} 箱</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handlePrint(row)"><el-icon><Printer /></el-icon>打印</el-button>
            <el-button size="small" type="primary" link @click="handleDetail(row)">详情</el-button>
            <el-button size="small" type="primary" link :disabled="row.status===2||row.status===3" @click="handleEdit(row)">修改</el-button>
            <el-button v-if="auth.role==='admin'" size="small" type="warning" link :disabled="row.status===3" @click="handleCancel(row)">作废</el-button>
            <el-button size="small" type="danger" link :disabled="row.status===2||row.status===3" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.current" v-model:page-size="query.size"
          :page-sizes="[10,20,50]" :total="total"
          layout="total,sizes,prev,pager,next,jumper"
          @size-change="handleSearch" @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Printer } from '@element-plus/icons-vue'
import { listOutboundApi, deleteOutboundApi, cancelOutboundApi, getOutboundDetailApi, OutboundStatusTagType } from '@/api/outbound'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10, orderNo: '', status: undefined as number | undefined, supplier: '', customerName: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await listOutboundApi({ current: query.current, size: query.size, orderNo: query.orderNo || undefined, status: query.status, supplier: query.supplier || undefined, customerName: query.customerName || undefined })
    tableData.value = res.data.records; total.value = res.data.total
  } catch { /* */ } finally { loading.value = false }
}

function handleSearch() { query.current = 1; fetchData() }
function handleReset() { query.orderNo = ''; query.status = undefined; query.supplier = ''; query.customerName = ''; handleSearch() }
function handleCreate() { router.push('/inventory/outbound/create') }

async function handlePrint(row: any) {
  try {
    const res = await getOutboundDetailApi(row.id)
    const o = res.data; const details = o.details || []
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
  } catch { /* */ }
}

function handleDetail(row: any) { router.push(`/inventory/outbound/detail/${row.id}`) }
function handleEdit(row: any) { router.push(`/inventory/outbound/edit/${row.id}`) }

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除出库单 ${row.orderNo}？`, '删除确认', { type: 'warning' })
    await deleteOutboundApi(row.id); ElMessage.success('删除成功'); fetchData()
  } catch { /* */ }
}

async function handleCancel(row: any) {
  try {
    await ElMessageBox.confirm(`确定作废出库单 ${row.orderNo}？作废后将释放所有锁定的条码。`, '作废确认', { type: 'warning' })
    await cancelOutboundApi(row.id); ElMessage.success('作废成功'); fetchData()
  } catch { /* */ }
}

onMounted(() => fetchData())
</script>

<style scoped>
.search-card { margin-bottom: 12px; }
.search-card :deep(.el-card__body) { padding-bottom: 0; }
.table-card { margin-bottom: 12px; }
.toolbar { margin-bottom: 12px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
