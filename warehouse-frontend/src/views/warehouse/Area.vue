<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增库区
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="code" label="库区编码" min-width="120" />
        <el-table-column prop="name" label="库区名称" min-width="150" />
        <el-table-column prop="warehouseName" label="所属仓库" min-width="140">
          <template #default="{ row }">
            {{ row.warehouseName || '-' }}
          </template>
        </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑库区' : '新增库区'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="所属仓库" required>
          <el-select v-model="form.warehouseId" placeholder="选择仓库" style="width: 100%">
            <el-option
              v-for="wh in warehouseList"
              :key="wh.id"
              :label="wh.name"
              :value="wh.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="库区编码" required>
          <el-input v-model="form.code" placeholder="如 A" />
        </el-form-item>
        <el-form-item label="库区名称" required>
          <el-input v-model="form.name" placeholder="库区名称" />
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
import { getAreaListApi, saveAreaApi, deleteAreaApi } from '@/api/warehouseArea'
import { getWarehouseListApi } from '@/api/warehouse'
import type { WarehouseArea, Warehouse } from '@/types/inbound'

const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | undefined>(undefined)
const tableData = ref<WarehouseArea[]>([])
const warehouseList = ref<Warehouse[]>([])

const form = reactive<WarehouseArea>({
  code: '',
  name: '',
  warehouseId: undefined,
  enabled: true,
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getAreaListApi()
    tableData.value = res.data || []
  } finally { loading.value = false }
}

async function fetchWarehouses() {
  try {
    const res = await getWarehouseListApi()
    warehouseList.value = res.data || []
  } catch { /* */ }
}

function handleAdd() {
  editingId.value = undefined
  form.code = ''
  form.name = ''
  form.warehouseId = warehouseList.value[0]?.id
  form.enabled = true
  dialogVisible.value = true
}

function handleEdit(row: WarehouseArea) {
  editingId.value = row.id
  form.code = row.code
  form.name = row.name
  form.warehouseId = row.warehouseId
  form.enabled = row.enabled !== false
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.code || !form.name) {
    ElMessage.warning('请填写库区编码和名称')
    return
  }
  if (!form.warehouseId) {
    ElMessage.warning('请选择所属仓库')
    return
  }
  try {
    await saveAreaApi({ ...form, id: editingId.value })
    ElMessage.success(editingId.value ? '更新成功' : '新增成功')
    dialogVisible.value = false
    fetchData()
  } catch { /* */ }
}

async function handleDelete(row: WarehouseArea) {
  try {
    await ElMessageBox.confirm(`确定删除库区 ${row.name} 吗？`, '删除确认', { type: 'warning' })
    await deleteAreaApi(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* */ }
}

onMounted(() => {
  fetchWarehouses()
  fetchData()
})
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
