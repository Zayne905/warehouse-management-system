<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增用户
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column label="角色" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled !== false ? 'success' : 'danger'" size="small">
              {{ row.enabled !== false ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="密码" :required="!editingId">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="editingId ? '留空则不修改密码' : '请输入密码'"
            show-password
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="user" />
          </el-select>
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
import { getUserListApi, saveUserApi, deleteUserApi } from '@/api/user'
import type { User } from '@/types/inbound'

const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | undefined>(undefined)
const tableData = ref<User[]>([])

const form = reactive<User>({
  username: '',
  nickname: '',
  password: '',
  role: 'user',
  enabled: true,
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserListApi()
    tableData.value = res.data || []
  } finally { loading.value = false }
}

function handleAdd() {
  editingId.value = undefined
  form.username = ''
  form.nickname = ''
  form.password = ''
  form.role = 'user'
  form.enabled = true
  dialogVisible.value = true
}

function handleEdit(row: User) {
  editingId.value = row.id
  form.username = row.username
  form.nickname = row.nickname || ''
  form.password = ''
  form.role = row.role || 'user'
  form.enabled = row.enabled !== false
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.username) {
    ElMessage.warning('请填写用户名')
    return
  }
  if (!editingId.value && !form.password) {
    ElMessage.warning('新建用户必须填写密码')
    return
  }
  try {
    await saveUserApi({ ...form, id: editingId.value })
    ElMessage.success(editingId.value ? '更新成功' : '新增成功')
    dialogVisible.value = false
    fetchData()
  } catch { /* */ }
}

async function handleDelete(row: User) {
  try {
    await ElMessageBox.confirm(`确定删除用户 ${row.username} 吗？`, '删除确认', { type: 'warning' })
    await deleteUserApi(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* */ }
}

onMounted(() => fetchData())
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
