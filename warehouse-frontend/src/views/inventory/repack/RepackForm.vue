<template>
  <div class="page-container">
    <!-- 头部 -->
    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <el-button size="small" @click="$router.push('/inventory/repack')"><el-icon><ArrowLeft /></el-icon>返回</el-button>
          <span style="font-weight:bold">{{ order.orderNo }}</span>
          <el-tag :type="RepackStatusTagType[order.status]||'info'" size="small">{{ order.statusText }}</el-tag>
          <el-tag effect="plain" size="small">{{ order.repackTypeText }}</el-tag>
        </div>
      </template>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="转包单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ order.repackTypeText }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="RepackStatusTagType[order.status]" size="small">{{ order.statusText }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="限定零件"><el-tag type="warning">{{ order.partCode }} / {{ order.partName }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="限定库区"><el-tag>{{ order.warehouseAreaName }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="备注">{{ order.remark||'-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 向下转包：一步操作卡 -->
    <el-card shadow="never" style="margin-top:12px" v-if="order.status===0 && order.repackType==='BREAKDOWN'">
      <template #header><span>扫描源箱，设定目标容量，自动拆包</span></template>
      <el-form label-width="100px">
        <el-form-item label="源看板号">
          <el-input v-model="bdSourceNo" placeholder="扫描或输入要拆的源看板号" style="width:380px" clearable @keyup.enter="doBdPreview" ref="scanRef">
            <template #prefix><el-icon><Camera /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="doBdPreview" :loading="bdPreviewLoading" style="margin-left:12px">查询</el-button>
        </el-form-item>
        <el-form-item label="源箱信息" v-if="bdSourceInfo">
          <el-tag type="success" size="large">{{ bdSourceInfo.partCode }} {{ bdSourceInfo.partName }}</el-tag>
          <span style="margin-left:8px">数量 <b>{{ bdSourceInfo.quantity }}</b> 件</span>
          <span style="margin-left:12px">库区 {{ bdSourceInfo.warehouseAreaName }}</span>
        </el-form-item>
        <el-form-item label="目标每箱容量" v-if="bdSourceInfo">
          <el-input-number v-model="bdCapacity" :min="1" :max="bdSourceInfo.quantity" style="width:160px" controls-position="right" @change="calcPreview" />
          <span style="margin-left:16px;color:#409eff" v-if="bdPreview">{{ bdPreview }}</span>
        </el-form-item>
        <el-form-item v-if="bdSourceInfo && bdCapacity>0">
          <el-button type="success" size="large" @click="doBdGenerate" :loading="bdLoading">
            <el-icon><MagicStick /></el-icon>生成拆包方案并确认
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 向上转包：逐个扫描源包装，自动取全部，合并为一个 -->
    <el-card shadow="never" style="margin-top:12px" v-if="order.status===0 && order.repackType==='CONSOLIDATE'">
      <template #header><span>逐个扫描源包装（自动取全部剩余量，最终合并为一个目标箱）</span></template>
      <div class="scan-bar">
        <el-input v-model="scanNo" placeholder="扫描或输入源看板号" style="width:380px" clearable @keyup.enter="doConsolidateAdd" ref="scanRef">
          <template #prefix><el-icon><Camera /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="doConsolidateAdd" :loading="cLoading" style="margin-left:8px"><el-icon><Plus /></el-icon>添加</el-button>
        <span v-if="cInfo" style="margin-left:12px;color:#67c23a;font-size:13px">{{ cInfo }}</span>
      </div>
    </el-card>

    <!-- 带余量：扫码添加1个源包装 -->
    <el-card shadow="never" style="margin-top:12px" v-if="order.status===0 && order.repackType==='REMAINDER'">
      <template #header><span>扫码添加源包装</span></template>
      <div class="scan-bar">
        <el-input v-model="scanNo" placeholder="扫描或输入源看板号" style="width:340px" clearable @keyup.enter="doRemainderAdd" ref="scanRef">
          <template #prefix><el-icon><Camera /></el-icon></template>
        </el-input>
        <el-input-number v-model="scanQty" :min="1" :max="99999" placeholder="转出数量" style="width:120px;margin-left:8px" controls-position="right" />
        <el-button type="primary" @click="doRemainderAdd" :loading="rLoading" style="margin-left:8px"><el-icon><Plus /></el-icon>添加</el-button>
      </div>
    </el-card>

    <!-- 明细表格 -->
    <el-card shadow="never" style="margin-top:12px">
      <template #header>
        <span>明细列表（{{ details.length }}行）</span>
        <span v-if="details.length>0" style="margin-left:8px;color:#409eff">合计 {{ totalQty }} 件</span>
      </template>
      <el-table :data="details" border stripe size="small">
        <el-table-column prop="lineNo" label="行号" width="60" align="center" />
        <el-table-column label="源看板号" min-width="250">
          <template #default="{ row }"><el-tag type="warning" effect="plain">{{ row.sourceKanbanNo }}</el-tag></template>
        </el-table-column>
        <el-table-column label="转出数量" width="120" align="center">
          <template #default="{ row }"><b>{{ row.transferQty }}</b> 件</template>
        </el-table-column>
        <el-table-column label="目标看板号" min-width="270" v-if="order.status===1">
          <template #default="{ row }"><el-tag v-if="row.targetKanbanNo" type="success" effect="plain">{{ row.targetKanbanNo }}</el-tag><span v-else>-</span></template>
        </el-table-column>
        <el-table-column label="操作" width="70" fixed="right" v-if="order.status===0 && order.repackType!=='BREAKDOWN'">
          <template #default="{ row }"><el-button size="small" type="danger" text @click="doRemove(row.id)"><el-icon><Delete /></el-icon></el-button></template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="footer-bar" v-if="order.status===0 && order.repackType!=='BREAKDOWN'">
      <el-button @click="$router.push('/inventory/repack')">返回列表</el-button>
      <el-button type="success" size="large" @click="doConfirm" :disabled="details.length===0"><el-icon><Check /></el-icon>保存并执行转包</el-button>
    </div>
    <div class="footer-bar" v-else><el-button @click="$router.push('/inventory/repack')">返回列表</el-button></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Camera, Plus, Check, Delete, MagicStick } from '@element-plus/icons-vue'
import { getRepackDetailApi, confirmRepackApi, previewRepackApi, addRepackDetailApi, removeRepackDetailApi, breakdownGenerateApi } from '@/api/repack'
import { RepackStatusTagType } from '@/types/repack'

const route = useRoute()
const order = ref<any>({})
const details = ref<any[]>([])
const totalQty = computed(() => details.value.reduce((s: number, d: any) => s + (d.transferQty || 0), 0))

// 向下转包专用
const bdSourceNo = ref('')
const bdSourceInfo = ref<any>(null)
const bdCapacity = ref<number>(0)
const bdPreview = ref('')
const bdPreviewLoading = ref(false)
const bdLoading = ref(false)

// 向上转包专用
const cLoading = ref(false); const cInfo = ref('')

// 带余量专用
const scanNo = ref(''); const scanQty = ref(1); const rLoading = ref(false)

const scanRef = ref<any>(null)

async function fetch() { const id=Number(route.params.id); try { const r=await getRepackDetailApi(id); order.value=r.data; details.value=r.data.details||[] } catch {} }

// ---- 通用校验 ----
async function validateSource(no: string) {
  const p=await previewRepackApi(no); const d=p.data
  if(d.status!==1&&d.status!==5) throw new Error(`状态为"${d.statusText}"，不可转包`)
  if(d.partId!==order.value.partId) throw new Error(`零件不匹配！限定[${order.value.partCode}]，看板[${d.partCode}]`)
  if(d.warehouseAreaId!==order.value.warehouseAreaId) throw new Error(`库区不匹配！限定[${order.value.warehouseAreaName}]，看板[${d.warehouseAreaName}]`)
  return d
}

// ---- 向下转包：扫描源箱预览 ----
async function doBdPreview() {
  const no=bdSourceNo.value.trim(); if(!no){ElMessage.warning('请输入看板号');return}
  bdPreviewLoading.value=true; bdSourceInfo.value=null; bdPreview.value=''
  try {
    const d=await validateSource(no); bdSourceInfo.value=d
    if(d.quantity<2){ElMessage.error(`拆包需要源箱至少2件，当前${d.quantity}件`);return}
    bdCapacity.value=Math.min(30,d.quantity)
    calcPreview()
  } catch(e:any){ElMessage.error(e?.message||'查询失败')} finally{bdPreviewLoading.value=false}
}

function calcPreview() {
  const qty=bdSourceInfo.value?.quantity||0; const cap=bdCapacity.value||0
  if(!qty||!cap||cap>qty){bdPreview.value='';return}
  const n=Math.max(2,Math.ceil(qty/cap))
  const last=qty-cap*(n-1)
  bdPreview.value=`预计生成 ${n} 箱，前${n-1}个各${cap}件，最后一个${last}件`
}

// ---- 向下转包：生成+确认 ----
async function doBdGenerate() {
  const no=bdSourceNo.value.trim(); if(!no||!bdSourceInfo.value){ElMessage.warning('请先查询源箱');return}
  if(!bdCapacity.value||bdCapacity.value<=0){ElMessage.warning('请输入目标容量');return}
  bdLoading.value=true
  try {
    await addRepackDetailApi(order.value.id,no,bdSourceInfo.value.quantity)
    const r=await breakdownGenerateApi(order.value.id,bdCapacity.value)
    order.value=r.data; details.value=r.data.details||[]
    ElMessage.success(`已生成 ${details.value.length} 箱拆包方案`)
    await ElMessageBox.confirm(`确认执行拆包？`, '确认执行', {type:'warning'})
    await confirmRepackApi(order.value.id)
    ElMessage.success('拆包完成！')
    fetch()
  } catch(e:any){if(e!=='cancel')ElMessage.error(e?.message||'操作失败')} finally{bdLoading.value=false}
}

// ---- 向上转包：自动取全部，合并为一个 ----
async function doConsolidateAdd() {
  const no=scanNo.value.trim(); if(!no){ElMessage.warning('请输入看板号');return}
  cLoading.value=true; cInfo.value=''
  try {
    const d=await validateSource(no)
    cInfo.value=`${d.partCode} ${d.partName} / ${d.transferableQty}件`
    const r=await addRepackDetailApi(order.value.id,no,0) // transferQty=0, backend auto-fills full
    order.value=r.data; details.value=r.data.details||[]
    ElMessage.success(`已添加: ${d.partName}，${d.transferableQty} 件`)
    scanNo.value=''; cInfo.value=''; nextTick(()=>scanRef.value?.focus())
  } catch(e:any){ElMessage.error(e?.message||'添加失败'); cInfo.value=''} finally{cLoading.value=false}
}

// ---- 带余量：手动输数量 ----
async function doRemainderAdd() {
  const no=scanNo.value.trim(); if(!no){ElMessage.warning('请输入看板号');return}
  rLoading.value=true
  try {
    const d=await validateSource(no)
    if(scanQty.value>=d.transferableQty){ElMessage.error(`带余量转包要求转出量<源数量(${d.transferableQty})`);return}
    const r=await addRepackDetailApi(order.value.id,no,scanQty.value)
    order.value=r.data; details.value=r.data.details||[]
    ElMessage.success(`已添加: ${d.partName}，转出 ${scanQty.value} 件，余 ${d.transferableQty-scanQty.value} 件`)
    scanNo.value='';scanQty.value=1; nextTick(()=>scanRef.value?.focus())
  } catch(e:any){ElMessage.error(e?.message||'添加失败')} finally{rLoading.value=false}
}

async function doRemove(detailId:number) {
  try { const r=await removeRepackDetailApi(detailId); order.value=r.data; details.value=r.data.details||[] } catch(e:any){ElMessage.error(e?.message||'删除失败')}
}

async function doConfirm() {
  if(details.value.length===0){ElMessage.warning('请先添加明细');return}
  try {
    await ElMessageBox.confirm('确认执行转包？将生成新看板并自动入库。','确认执行',{type:'warning'})
    await confirmRepackApi(order.value.id)
    ElMessage.success('转包执行成功'); fetch()
  } catch {}
}

onMounted(()=>{fetch();nextTick(()=>scanRef.value?.focus())})
</script>

<style scoped>
.page-container{padding:12px}
.header-row{display:flex;align-items:center;gap:12px}
.scan-bar{display:flex;align-items:center;flex-wrap:wrap;gap:4px}
.footer-bar{display:flex;justify-content:flex-end;gap:12px;margin-top:12px;padding:12px;background:#fff;border-radius:4px}
</style>
