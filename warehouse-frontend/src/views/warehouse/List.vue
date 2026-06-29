<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增仓库
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="code" label="仓库编码" min-width="120" />
        <el-table-column prop="name" label="仓库名称" min-width="160" />
        <el-table-column prop="address" label="地址" min-width="200" />
        <el-table-column prop="adminName" label="负责人" width="100" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled !== false ? 'success' : 'danger'" size="small">
              {{ row.enabled !== false ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑仓库' : '新增仓库'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="仓库编码" required>
          <el-input v-model="form.code" placeholder="如 WH001" />
        </el-form-item>
        <el-form-item label="仓库名称" required>
          <el-input v-model="form.name" placeholder="仓库名称" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="详细地址" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.adminName" placeholder="负责人姓名" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getWarehouseListApi, saveWarehouseApi, deleteWarehouseApi } from '@/api/warehouse'
import type { Warehouse } from '@/types/inbound'

const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | undefined>(undefined)
const tableData = ref<Warehouse[]>([])

const form = reactive<Warehouse>({
  code: '',
  name: '',
  address: '',
  adminName: '',
  enabled: true,
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getWarehouseListApi()
    tableData.value = res.data || []
  } finally { loading.value = false }
}

function handleAdd() {
  editingId.value = undefined
  form.code = ''
  form.name = ''
  form.address = ''
  form.adminName = ''
  form.enabled = true
  dialogVisible.value = true
}

function handleEdit(row: Warehouse) {
  editingId.value = row.id
  form.code = row.code
  form.name = row.name
  form.address = row.address || ''
  form.adminName = row.adminName || ''
  form.enabled = row.enabled !== false
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.code || !form.name) {
    ElMessage.warning('请填写仓库编码和名称')
    return
  }
  try {
    await saveWarehouseApi({ ...form, id: editingId.value })
    ElMessage.success(editingId.value ? '更新成功' : '新增成功')
    dialogVisible.value = false
    fetchData()
  } catch { /* */ }
}

async function handleDelete(row: Warehouse) {
  try {
    await ElMessageBox.confirm(`确定删除仓库 ${row.name} 吗？`, '删除确认', { type: 'warning' })
    await deleteWarehouseApi(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* */ }
}

onMounted(() => fetchData())
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
