<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增零件
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="code" label="物料编码" min-width="120" />
        <el-table-column prop="name" label="物料名称" min-width="140" />
        <el-table-column prop="spec" label="规格" min-width="120" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column label="包装容量" width="100" align="center">
          <template #default="{ row }">
            {{ row.packageCapacity || 1 }}
          </template>
        </el-table-column>
        <el-table-column label="供应商" width="160">
          <template #default="{ row }">
            {{ row.supplierName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="默认库区" width="140">
          <template #default="{ row }">
            {{ row.warehouseAreaName || '-' }}
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑零件' : '新增零件'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="物料编码" required>
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="物料名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.spec" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" placeholder="个/箱/kg" />
        </el-form-item>
        <el-form-item label="供应商" required>
          <el-select v-model="form.supplierId" placeholder="选择供应商" style="width: 100%">
            <el-option v-for="s in supplierList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="包装容量">
          <el-input-number v-model="form.packageCapacity" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="默认库区">
          <el-select v-model="form.warehouseAreaId" placeholder="选择默认库区" clearable style="width: 100%">
            <el-option v-for="a in areaList" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
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
import { getPartListApi, savePartApi, deletePartApi } from '@/api/part'
import { getAreaListApi } from '@/api/warehouseArea'
import { getSupplierListApi } from '@/api/supplier'
import type { Part, WarehouseArea, Supplier } from '@/types/inbound'

const loading = ref(false)
const tableData = ref<Part[]>([])
const areaList = ref<WarehouseArea[]>([])
const supplierList = ref<Supplier[]>([])
const dialogVisible = ref(false)
const editingId = ref<number>()

const form = reactive<Part>({
  id: 0,
  code: '',
  name: '',
  spec: '',
  unit: '',
  packageCapacity: 1,
  warehouseAreaId: undefined,
  supplierId: undefined,
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getPartListApi()
    tableData.value = res.data
  } catch { /* ignore */ } finally { loading.value = false }
}

async function fetchAreas() {
  try {
    const res = await getAreaListApi()
    areaList.value = res.data
  } catch { /* ignore */ }
}

async function fetchSuppliers() {
  try {
    const res = await getSupplierListApi()
    supplierList.value = res.data
  } catch { /* ignore */ }
}

onMounted(() => { fetchData(); fetchAreas(); fetchSuppliers() })

function handleAdd() {
  editingId.value = undefined
  Object.assign(form, { id: 0, code: '', name: '', spec: '', unit: '', packageCapacity: 1, warehouseAreaId: undefined, supplierId: undefined })
  dialogVisible.value = true
}

function handleEdit(row: Part) {
  editingId.value = row.id
  Object.assign(form, {
    ...row,
    packageCapacity: row.packageCapacity || 1,
  })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.code || !form.name) {
    ElMessage.warning('物料编码和名称不能为空')
    return
  }
  try {
    await savePartApi({ ...form, id: editingId.value || 0 })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchData()
  } catch { /* ignore */ }
}

async function handleDelete(row: Part) {
  try {
    await ElMessageBox.confirm(
      `确定删除零件 ${row.code} ${row.name} 吗？`,
      '删除确认',
      { type: 'warning' }
    )
    await deletePartApi(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* ignore */ }
}
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>
