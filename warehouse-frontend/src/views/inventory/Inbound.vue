<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="入库单号">
          <el-input
            v-model="query.orderNo"
            placeholder="输入入库单号"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select
            v-model="query.supplierId"
            placeholder="全部"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="s in supplierList"
              :key="s.id"
              :label="s.name"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="订单号">
          <el-input
            v-model="query.orderNumber"
            placeholder="输入订单号"
            clearable
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 140px"
          >
            <el-option label="未入库" :value="0" />
            <el-option label="部分入库" :value="1" />
            <el-option label="已入库" :value="2" />
            <el-option label="作废" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <el-card class="table-card" shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增入库单
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="orderNo" label="入库单号" min-width="160" />
        <el-table-column prop="supplierName" label="供应商" min-width="140" />
        <el-table-column prop="orderNumber" label="订单号" min-width="140" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="310" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              link
              @click="handlePrint(row)"
            >
              <el-icon><Printer /></el-icon>
              打印
            </el-button>
            <el-button
              size="small"
              type="primary"
              link
              @click="handleDetail(row)"
            >
              详情
            </el-button>
            <el-button
              size="small"
              type="primary"
              link
              :disabled="!canEdit(row)"
              @click="handleEdit(row)"
            >
              修改
            </el-button>
            <el-button
              v-if="authStore.role === 'admin'"
              size="small"
              type="warning"
              link
              :disabled="row.status === 3"
              @click="handleCancel(row)"
            >
              作废
            </el-button>
            <el-button
              size="small"
              type="danger"
              link
              :disabled="!canEdit(row)"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 打印对话框 -->
    <PrintDialog
      v-if="printOrderData"
      v-model:visible="showPrintDialog"
      :order="printOrderData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Printer } from '@element-plus/icons-vue'
import { listInboundApi, deleteInboundApi, cancelInboundApi, getInboundDetailApi } from '@/api/inbound'
import { getSupplierListApi } from '@/api/supplier'
import { useAuthStore } from '@/stores/auth'
import {
  InboundStatusText,
  InboundStatusTagType,
} from '@/types/inbound'
import type { InboundOrderVO, Supplier } from '@/types/inbound'
import PrintDialog from './inbound/components/PrintDialog.vue'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const tableData = ref<InboundOrderVO[]>([])
const total = ref(0)
const showPrintDialog = ref(false)
const printOrderData = ref<InboundOrderVO | null>(null)
const supplierList = ref<Supplier[]>([])

const query = reactive({
  current: 1,
  size: 10,
  orderNo: '',
  supplierId: undefined as number | undefined,
  orderNumber: '',
  status: undefined as number | undefined,
})

function getStatusText(status: number): string {
  return InboundStatusText[status] || '未知'
}

function getStatusTagType(status: number): string {
  return InboundStatusTagType[status] || 'info'
}

function canEdit(row: InboundOrderVO): boolean {
  if (row.status === 3) return false
  if (row.status === 2) {
    return authStore.role === 'admin'
  }
  return true
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listInboundApi({
      current: query.current,
      size: query.size,
      orderNo: query.orderNo || undefined,
      supplierId: query.supplierId,
      orderNumber: query.orderNumber || undefined,
      status: query.status,
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch {
    // 错误由拦截器处理
  } finally {
    loading.value = false
  }
}

async function fetchSuppliers() {
  try {
    const res = await getSupplierListApi()
    supplierList.value = res.data
  } catch {
    // ignore
  }
}

function handleSearch() {
  query.current = 1
  fetchData()
}

function handlePageSizeChange() {
  query.current = 1
  fetchData()
}

function handlePageChange() {
  fetchData()
}

function handleReset() {
  query.orderNo = ''
  query.supplierId = undefined
  query.orderNumber = ''
  query.status = undefined
  handleSearch()
}

function handleCreate() {
  router.push('/inventory/inbound/create')
}

async function handlePrint(row: InboundOrderVO) {
  try {
    const res = await getInboundDetailApi(row.id)
    printOrderData.value = res.data
    showPrintDialog.value = true
  } catch { /* ignore */ }
}

function handleDetail(row: InboundOrderVO) {
  router.push(`/inventory/inbound/detail/${row.id}`)
}

function handleEdit(row: InboundOrderVO) {
  router.push(`/inventory/inbound/edit/${row.id}`)
}

async function handleDelete(row: InboundOrderVO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除入库单 ${row.orderNo} 吗？`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteInboundApi(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 取消
  }
}

async function handleCancel(row: InboundOrderVO) {
  try {
    await ElMessageBox.confirm(
      `确定要作废入库单 ${row.orderNo} 吗？作废后不可恢复。`,
      '作废确认',
      { type: 'warning' }
    )
    await cancelInboundApi(row.id)
    ElMessage.success('作废成功')
    fetchData()
  } catch {
    // 取消
  }
}

onMounted(() => {
  fetchSuppliers()
  fetchData()
})
</script>

<style scoped>
.search-card {
  margin-bottom: 12px;
}
.search-card :deep(.el-card__body) {
  padding-bottom: 0;
}
.table-card {
  margin-bottom: 12px;
}
.toolbar {
  margin-bottom: 12px;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
