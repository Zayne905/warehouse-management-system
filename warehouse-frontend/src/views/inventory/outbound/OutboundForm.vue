<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header><span>{{ isEdit ? '编辑出库单' : '新增出库单' }}</span></template>
      <el-form :model="form" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="出库单号">
              <el-input :model-value="form.orderNo || '保存后自动生成'" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="客户">
          <el-select v-model="form.customerName" placeholder="请选择客户" clearable style="width:240px">
            <el-option v-for="c in customerList" :key="c.id" :label="c.name" :value="c.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <template #header><span>零件明细</span></template>
      <div class="toolbar">
        <el-button size="small" type="primary" @click="openPartSelector"><el-icon><Plus /></el-icon>添加零件</el-button>
        <el-button size="small" @click="removeRows" :disabled="selectedRows.length===0"><el-icon><Delete /></el-icon>删除选中</el-button>
      </div>
      <el-table :data="details" border stripe @selection-change="onSelect">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="partCode" label="物料编码" width="120" />
        <el-table-column prop="partName" label="物料名称" width="140" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column label="可用库存" width="100" align="center">
          <template #default="{ row }">{{ row._stock ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="计划出库数量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.plannedQty" :min="1" :max="row._stock||99999" controls-position="right" size="small" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column label="实出" width="70" align="center">
          <template #default="{ row }">{{ row.actualQty || 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="60" fixed="right">
          <template #default="{ $index }">
            <el-button size="small" type="danger" text @click="details.splice($index,1)"><el-icon><Delete /></el-icon></el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="footer-bar">
      <el-button @click="router.back()">取消</el-button>
      <el-button type="primary" @click="doSave">保存</el-button>
    </div>

    <!-- 零件选择器 -->
    <el-dialog v-model="showPartSelector" title="选择零件（仅显示有库存的零件）" width="650px">
      <div style="margin-bottom: 12px;">
        <el-input
          v-model="partSearchKeyword"
          placeholder="搜索物料编码或名称"
          clearable
          style="width: 260px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <el-table :data="filteredPartList" border stripe max-height="400" @selection-change="onPartSelect">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column label="库存" width="90" align="center">
          <template #default="{ row }">{{ row._stock }}</template>
        </el-table-column>
      </el-table>
      <div v-if="filteredPartList.length === 0" style="text-align:center;padding:20px;color:#909399">
        {{ partSearchKeyword ? '无匹配零件' : '暂无有库存的零件' }}
      </div>
      <template #footer>
        <el-button @click="showPartSelector = false">取消</el-button>
        <el-button type="primary" @click="addSelectedParts" :disabled="selParts.length===0">添加选中 ({{ selParts.length }})</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Search } from '@element-plus/icons-vue'
import { getPartListApi } from '@/api/part'
import { saveOutboundApi, getOutboundDetailApi, getAvailableStockApi } from '@/api/outbound'
import { getCustomerListApi } from '@/api/customer'
import type { Customer } from '@/types/inbound'

const router = useRouter(); const route = useRoute()
const isEdit = ref(false); const showPartSelector = ref(false); const partSearchKeyword = ref('')
const selectedRows = ref<any[]>([]); const selParts = ref<any[]>([])
const customerList = ref<Customer[]>([])
const form = reactive({ id: undefined as number|undefined, orderNo: '', remark: '', customerName: '' })
const details = ref<any[]>([])
const partList = ref<any[]>([])

const filteredPartList = computed(() => {
  const kw = partSearchKeyword.value.trim().toLowerCase()
  if (!kw) return partList.value
  return partList.value.filter((p: any) =>
    (p.code && p.code.toLowerCase().includes(kw)) ||
    (p.name && p.name.toLowerCase().includes(kw))
  )
})

onMounted(async () => {
  // 加载客户列表
  try {
    const res = await getCustomerListApi()
    customerList.value = res.data || []
  } catch { /* */ }
  const editId = route.params.id
  if (editId) {
    isEdit.value = true
    try {
      const res = await getOutboundDetailApi(Number(editId))
      const o = res.data; form.id = o.id; form.orderNo = o.orderNo; form.remark = o.remark || ''; form.customerName = o.customerName || ''
      details.value = (o.details || []).map((d: any) => ({ ...d, _stock: d.availableStock }))
    } catch { /* */ }
  }
})

function onSelect(rows: any[]) { selectedRows.value = rows }
function onPartSelect(rows: any[]) { selParts.value = rows }
function removeRows() {
  const ids = new Set(selectedRows.value.map((r: any) => r.partId))
  details.value = details.value.filter(d => !ids.has(d.partId))
}

async function openPartSelector() {
  partSearchKeyword.value = ''
  showPartSelector.value = true
  try {
    const res = await getPartListApi()
    const all = (res.data || []).map((p: any) => ({ ...p, _stock: 0 }))
    for (const p of all) {
      try {
        const sr = await getAvailableStockApi(p.id)
        p._stock = sr.data || 0
      } catch { /* */ }
    }
    // 只显示有库存的零件
    partList.value = all.filter((p: any) => p._stock > 0)
  } catch { /* */ }
}

function addSelectedParts() {
  for (const p of selParts.value) {
    if (details.value.some(d => d.partId === p.id)) continue
    details.value.push({
      partId: p.id, partCode: p.code, partName: p.name, unit: p.unit,
      plannedQty: 0, actualQty: 0,
      _stock: p._stock || 0
    })
  }
  showPartSelector.value = false
}

async function doSave() {
  if (details.value.length === 0) { ElMessage.warning('请添加零件'); return }
  for (const d of details.value) {
    if (!d.plannedQty || d.plannedQty <= 0) { ElMessage.warning(`${d.partName}: 请输入计划出库数量`); return }
  }
  try {
    const res = await saveOutboundApi({
      id: form.id, remark: form.remark, customerName: form.customerName,
      details: details.value.map((d, i) => ({
        partId: d.partId, plannedQty: d.plannedQty,
        boxCount: 0, unit: d.unit, lineNo: i + 1
      }))
    })
    const o = res.data
    ElMessage.success(`保存成功！请在详情页手动匹配看板或直接扫码出库。`)
    router.push(`/inventory/outbound/detail/${o.id}`)
  } catch { /* */ }
}
</script>

<style scoped>
.toolbar { margin-bottom: 12px; display: flex; gap: 8px; }
.footer-bar { margin-top: 16px; display: flex; justify-content: center; gap: 12px; }
</style>
