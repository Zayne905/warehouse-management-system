<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="转包单号"><el-input v-model="query.orderNo" placeholder="输入转包单号" clearable style="width:180px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:140px">
            <el-option label="待转包" :value="0" /><el-option label="已完成" :value="1" /><el-option label="已取消" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="转包类型">
          <el-select v-model="query.repackType" placeholder="全部" clearable style="width:180px">
            <el-option v-for="t in RepackTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon>重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="toolbar">
        <el-button type="primary" @click="openCreate"><el-icon><Plus /></el-icon>新增转包单</el-button>
        <el-button type="warning" @click="showTrace=true; nextTick(()=>traceInputRef?.focus())"><el-icon><Search /></el-icon>扫码溯源</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="orderNo" label="转包单号" min-width="160">
          <template #default="{ row }">
            <el-link :type="row.status===0?'':'primary'" @click="row.status===0 ? handleOperate(row) : handleDetail(row)">{{ row.orderNo }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag :type="RepackStatusTagType[row.status]||'info'" size="small">{{ row.statusText }}</el-tag></template>
        </el-table-column>
        <el-table-column label="转包类型" width="160" align="center">
          <template #default="{ row }">{{ row.repackTypeText||'-' }}</template>
        </el-table-column>
        <el-table-column label="限定零件" min-width="140">
          <template #default="{ row }">{{ row.partCode ? row.partCode+' / '+row.partName : '-' }}</template>
        </el-table-column>
        <el-table-column label="限定库区" width="120">
          <template #default="{ row }">{{ row.warehouseAreaName||'-' }}</template>
        </el-table-column>
        <el-table-column prop="detailCount" label="行数" width="60" align="center" />
        <el-table-column label="总转出量" width="90" align="center">
          <template #default="{ row }"><el-tag type="warning" effect="plain">{{ row.totalTransferQty }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="130" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status===0" size="small" type="primary" @click="handleOperate(row)"><el-icon><Edit /></el-icon>扫码添加</el-button>
            <el-button v-else size="small" type="primary" link @click="handleDetail(row)"><el-icon><View /></el-icon>详情</el-button>
            <el-button v-if="row.status===0" size="small" type="success" link @click="handleConfirm(row)"><el-icon><Check /></el-icon>执行</el-button>
            <el-button v-if="row.status===0" size="small" type="warning" link @click="handleCancel(row)"><el-icon><Close /></el-icon>取消</el-button>
            <el-button v-if="row.status!==1" size="small" type="danger" link @click="handleDelete(row)"><el-icon><Delete /></el-icon>删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper" v-if="page.total>0">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :page-sizes="[10,20,50]" :total="page.total"
          layout="total,sizes,prev,pager,next,jumper" @size-change="fetchData" @current-change="fetchData" />
      </div>
    </el-card>

    <!-- 创建弹窗 -->
    <el-dialog v-model="showCreate" title="新增转包单" width="500px" @closed="resetCreate">
      <el-form :model="form" label-width="90px">
        <el-form-item label="转包类型" required>
          <el-select v-model="form.repackType" style="width:100%" @change="onTypeChange">
            <el-option v-for="t in RepackTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-alert v-if="form.repackType==='BREAKDOWN'" title="向下转包（拆包1→N）：扫码同一个源箱，逐行添加拆出的目标数量和容量" type="info" :closable="false" show-icon style="margin-bottom:12px" />
        <el-alert v-if="form.repackType==='CONSOLIDATE'" title="向上转包（合并N→1）：扫码多个同种箱子，合并成一个大箱" type="info" :closable="false" show-icon style="margin-bottom:12px" />
        <el-alert v-if="form.repackType==='REMAINDER'" title="带余量转包（1→1）：转出一部分，源保留余量" type="info" :closable="false" show-icon style="margin-bottom:12px" />
        <el-form-item label="限定零件" required>
          <el-select v-model="form.partId" placeholder="选择零件" filterable style="width:100%">
            <el-option v-for="p in partList" :key="p.id" :label="p.code+' - '+p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="限定库区" required>
          <el-select v-model="form.warehouseAreaId" placeholder="选择库区" style="width:100%">
            <el-option v-for="a in areaList" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate=false">取消</el-button>
        <el-button type="primary" @click="doCreate" :loading="creating">创建转包单</el-button>
      </template>
    </el-dialog>

    <!-- 扫码溯源弹窗 -->
    <el-dialog v-model="showTrace" title="扫码溯源" width="800px" @closed="traceResult=null;traceNo=''">
      <div class="trace-bar" style="margin-bottom:12px">
        <el-input v-model="traceNo" placeholder="扫描或输入看板号" style="width:380px" clearable @keyup.enter="doTrace" ref="traceInputRef">
          <template #prefix><el-icon><Camera /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="doTrace" :loading="traceLoading" style="margin-left:8px"><el-icon><Search /></el-icon>追溯</el-button>
      </div>
      <div v-if="traceResult">
        <el-descriptions :column="3" border size="small" title="当前看板">
          <el-descriptions-item label="看板号">{{ traceResult.currentKanban?.kanbanNo }}</el-descriptions-item>
          <el-descriptions-item label="物料">{{ traceResult.currentKanban?.partCode }} / {{ traceResult.currentKanban?.partName }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ traceResult.currentKanban?.quantity }}</el-descriptions-item>
          <el-descriptions-item label="库区">{{ traceResult.currentKanban?.warehouseAreaName||'-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ traceResult.currentKanban?.statusText }}</el-descriptions-item>
          <el-descriptions-item label="入库单号">{{ traceResult.currentKanban?.inboundOrderNo||'-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="traceResult.parentChain?.length" style="margin-top:12px">
          <h4 style="margin:8px 0">⬆ 向上追溯 — 来源（共 {{ traceResult.parentChain.length }} 条）</h4>
          <el-table :data="traceResult.parentChain" border stripe size="small">
            <el-table-column prop="level" label="层级" width="60" align="center" />
            <el-table-column label="父看板号" min-width="200">
              <template #default="{row}"><el-tag type="warning" effect="plain">{{ row.parentKanbanNo }}</el-tag></template>
            </el-table-column>
            <el-table-column label="转出数量" width="100" align="center">
              <template #default="{row}"><el-tag type="danger" effect="plain">{{ row.transferQty }} 件</el-tag></template>
            </el-table-column>
            <el-table-column prop="repackOrderNo" label="转包单号" width="160" />
            <el-table-column prop="repackTime" label="转包时间" width="170" />
          </el-table>
        </div>

        <div v-if="traceResult.childChain?.length" style="margin-top:12px">
          <h4 style="margin:8px 0">⬇ 向下追溯 — 去往（共 {{ traceResult.childChain.length }} 条）</h4>
          <el-table :data="traceResult.childChain" border stripe size="small">
            <el-table-column prop="level" label="层级" width="60" align="center" />
            <el-table-column label="子看板号" min-width="200">
              <template #default="{row}"><el-tag type="success" effect="plain">{{ row.childKanbanNo }}</el-tag></template>
            </el-table-column>
            <el-table-column label="转出数量" width="100" align="center">
              <template #default="{row}"><el-tag type="danger" effect="plain">{{ row.transferQty }} 件</el-tag></template>
            </el-table-column>
            <el-table-column prop="repackOrderNo" label="转包单号" width="160" />
            <el-table-column prop="repackTime" label="转包时间" width="170" />
          </el-table>
        </div>

        <div v-if="!traceResult.parentChain?.length && !traceResult.childChain?.length" style="text-align:center;padding:16px;color:#909399">
          该看板暂无转包记录
        </div>
      </div>
      <template #footer>
        <el-button @click="showTrace=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, View, Check, Close, Delete, Edit, Camera } from '@element-plus/icons-vue'
import { listRepackApi, saveRepackApi, cancelRepackApi, confirmRepackApi, deleteRepackApi, traceRepackApi } from '@/api/repack'
import { RepackStatusTagType, RepackTypeOptions } from '@/types/repack'
import { getPartListApi } from '@/api/part'
import { getAreaListApi } from '@/api/warehouseArea'

const router = useRouter()
const loading = ref(false); const tableData = ref<any[]>([])
const query = reactive({ current: 1, size: 10, orderNo: '', status: undefined as number|undefined, repackType: '' })
const page = reactive({ current: 1, size: 10, total: 0 })
const showCreate = ref(false); const creating = ref(false)
const form = reactive({ repackType: 'REMAINDER', partId: undefined as number|undefined, warehouseAreaId: undefined as number|undefined, remark: '' })
const partList = ref<any[]>([]); const areaList = ref<any[]>([])
const showTrace = ref(false); const traceNo = ref(''); const traceLoading = ref(false); const traceResult = ref<any>(null)
const traceInputRef = ref<any>(null)

function resetCreate() { form.repackType='REMAINDER'; form.partId=undefined; form.warehouseAreaId=undefined; form.remark='' }
function onTypeChange() {}

async function fetchData() {
  loading.value = true
  try { query.current=page.current; query.size=page.size; const r=await listRepackApi(query as any); tableData.value=r.data.records||[]; page.total=r.data.total; page.current=r.data.current; page.size=r.data.size } finally { loading.value=false }
}
function handleSearch() { page.current=1; fetchData() }
function handleReset() { query.orderNo=''; query.status=undefined; query.repackType=''; page.current=1; fetchData() }
function handleOperate(row:any) { router.push(`/inventory/repack/edit/${row.id}`) }
function handleDetail(row:any) { router.push(`/inventory/repack/detail/${row.id}`) }
async function handleConfirm(row:any) {
  try { await ElMessageBox.confirm(`确认执行转包单「${row.orderNo}」？`,'确认执行',{type:'warning'}); await confirmRepackApi(row.id); ElMessage.success('转包执行成功'); fetchData() } catch {}
}
async function handleCancel(row:any) {
  try { await ElMessageBox.confirm(`确认取消转包单「${row.orderNo}」？`,'确认取消',{type:'warning'}); await cancelRepackApi(row.id); ElMessage.success('已取消'); fetchData() } catch {}
}
async function handleDelete(row:any) {
  try { await ElMessageBox.confirm(`确认删除转包单「${row.orderNo}」？`,'确认删除',{type:'warning'}); await deleteRepackApi(row.id); ElMessage.success('已删除'); fetchData() } catch {}
}

async function openCreate() {
  showCreate.value = true
  if (partList.value.length===0) try { const r=await getPartListApi(); partList.value=r.data||[] } catch {}
  if (areaList.value.length===0) try { const r=await getAreaListApi(); areaList.value=r.data||[] } catch {}
}

async function doCreate() {
  if (!form.repackType) { ElMessage.warning('请选择转包类型'); return }
  if (!form.partId) { ElMessage.warning('请选择限定零件'); return }
  if (!form.warehouseAreaId) { ElMessage.warning('请选择限定库区'); return }
  creating.value = true
  try {
    const res = await saveRepackApi({ repackType: form.repackType, partId: form.partId, warehouseAreaId: form.warehouseAreaId, remark: form.remark })
    ElMessage.success('转包单创建成功: '+res.data.orderNo)
    showCreate.value = false
    router.push(`/inventory/repack/edit/${res.data.id}`)
  } finally { creating.value = false }
}

async function doTrace() {
  if(!traceNo.value.trim()){ElMessage.warning('请输入看板号');return}
  traceLoading.value=true; traceResult.value=null
  try{
    const r=await traceRepackApi(traceNo.value.trim())
    traceResult.value=r.data
    if(!r.data.parentChain?.length&&!r.data.childChain?.length) ElMessage.info('该看板暂无转包记录')
  }catch(e:any){ElMessage.error(e?.message||'追溯失败')}finally{traceLoading.value=false}
}

onMounted(() => fetchData())
</script>

<style scoped>
.page-container { padding: 12px; }
.search-card,.table-card { margin-bottom: 12px; }
.toolbar { margin-bottom: 12px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
