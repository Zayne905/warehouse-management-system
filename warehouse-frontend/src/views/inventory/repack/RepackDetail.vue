<template>
  <div class="page-container">
    <!-- 转包单基本信息 -->
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px">
          <el-button size="small" @click="router.back()"><el-icon><ArrowLeft /></el-icon>返回</el-button>
          <span>转包单详情</span>
        </div>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="转包单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="RepackStatusTagType[order.status] || 'info'" size="small">{{ order.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="转包类型">{{ order.repackTypeText || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联出库单">{{ order.outboundOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="明细行数">{{ order.detailCount }}</el-descriptions-item>
        <el-descriptions-item label="总转出量">
          <el-tag type="warning" effect="plain">{{ order.totalTransferQty }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ order.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ order.updateTime }}</el-descriptions-item>
      </el-descriptions>

      <div style="margin-top: 12px" v-if="order.status === 0">
        <el-button type="success" @click="doConfirm">
          <el-icon><Check /></el-icon>确认执行转包
        </el-button>
        <el-button type="warning" @click="doCancel">
          <el-icon><Close /></el-icon>取消转包单
        </el-button>
      </div>
    </el-card>

    <!-- 转包明细 -->
    <el-card shadow="never" style="margin-top: 12px">
      <template #header><span>转包明细（共 {{ details.length }} 行）</span></template>
      <el-table :data="details" border stripe>
        <el-table-column prop="lineNo" label="行号" width="55" align="center" />
        <el-table-column label="源看板号" width="240">
          <template #default="{ row }">
            <el-tag type="warning" effect="plain">{{ row.sourceKanbanNo }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="partCode" label="物料编码" width="120" />
        <el-table-column prop="partName" label="物料名称" min-width="140" />
        <el-table-column label="转出数量" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="danger" effect="plain">{{ row.transferQty }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remainderQty" label="余量" width="70" align="center">
          <template #default="{ row }">{{ row.remainderQty > 0 ? row.remainderQty : '-' }}</template>
        </el-table-column>
        <el-table-column label="目标看板号" width="240">
          <template #default="{ row }">
            <el-tag v-if="row.targetKanbanNo" type="success" effect="plain">{{ row.targetKanbanNo }}</el-tag>
            <span v-else class="text-hint">待执行</span>
          </template>
        </el-table-column>
        <el-table-column label="源剩余数量" width="110" align="center">
          <template #default="{ row }">
            <span v-if="row.sourceRemainingQty != null">{{ row.sourceRemainingQty }}</span>
            <span v-else class="text-hint">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 追溯区域 -->
    <el-card shadow="never" style="margin-top: 12px" v-if="order.status === 1">
      <template #header><span>追溯看板转包链</span></template>
      <div class="trace-bar">
        <el-input v-model="traceKanbanNo" placeholder="输入看板号查询追溯链" style="width: 300px" clearable />
        <el-button type="primary" @click="doTrace" style="margin-left: 8px">
          <el-icon><Search /></el-icon>追溯
        </el-button>
      </div>

      <div v-if="traceResult" style="margin-top: 12px">
        <!-- 当前看板信息 -->
        <el-descriptions :column="3" border size="small" title="当前看板">
          <el-descriptions-item label="看板号">{{ traceResult.currentKanban?.kanbanNo }}</el-descriptions-item>
          <el-descriptions-item label="物料">{{ traceResult.currentKanban?.partCode }} / {{ traceResult.currentKanban?.partName }}</el-descriptions-item>
          <el-descriptions-item label="数量">{{ traceResult.currentKanban?.quantity }}</el-descriptions-item>
          <el-descriptions-item label="库区">{{ traceResult.currentKanban?.warehouseAreaName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ traceResult.currentKanban?.statusText }}</el-descriptions-item>
          <el-descriptions-item label="入库单号">{{ traceResult.currentKanban?.inboundOrderNo }}</el-descriptions-item>
        </el-descriptions>

        <!-- 向上追溯（父链） -->
        <div v-if="traceResult.parentChain?.length" style="margin-top: 12px">
          <h4>⬆ 向上追溯（来源）</h4>
          <el-table :data="traceResult.parentChain" border stripe size="small">
            <el-table-column prop="level" label="层级" width="60" align="center" />
            <el-table-column prop="parentKanbanNo" label="父看板号" min-width="200">
              <template #default="{ row }">
                <el-tag type="warning" effect="plain">{{ row.parentKanbanNo }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="转出数量" width="90" align="center">
              <template #default="{ row }">
                <el-tag type="danger" effect="plain">{{ row.transferQty }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="repackOrderNo" label="转包单号" width="160" />
            <el-table-column prop="repackTime" label="转包时间" width="170" />
          </el-table>
        </div>

        <!-- 向下追溯（子链） -->
        <div v-if="traceResult.childChain?.length" style="margin-top: 12px">
          <h4>⬇ 向下追溯（去往）</h4>
          <el-table :data="traceResult.childChain" border stripe size="small">
            <el-table-column prop="level" label="层级" width="60" align="center" />
            <el-table-column prop="childKanbanNo" label="子看板号" min-width="200">
              <template #default="{ row }">
                <el-tag type="success" effect="plain">{{ row.childKanbanNo }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="转出数量" width="90" align="center">
              <template #default="{ row }">
                <el-tag type="danger" effect="plain">{{ row.transferQty }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="repackOrderNo" label="转包单号" width="160" />
            <el-table-column prop="repackTime" label="转包时间" width="170" />
          </el-table>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, Close, Search } from '@element-plus/icons-vue'
import { getRepackDetailApi, confirmRepackApi, cancelRepackApi, traceRepackApi } from '@/api/repack'
import { RepackStatusTagType } from '@/types/repack'

const router = useRouter()
const route = useRoute()

const order = ref<any>({})
const details = ref<any[]>([])
const traceKanbanNo = ref('')
const traceResult = ref<any>(null)

async function fetchData() {
  const id = Number(route.params.id)
  try {
    const res = await getRepackDetailApi(id)
    order.value = res.data
    details.value = res.data.details || []
  } catch { /* */ }
}

async function doConfirm() {
  try {
    await ElMessageBox.confirm('确认执行此转包单？', '确认执行', { type: 'warning' })
    await confirmRepackApi(order.value.id)
    ElMessage.success('转包执行成功')
    fetchData()
  } catch { /* */ }
}

async function doCancel() {
  try {
    await ElMessageBox.confirm('确认取消此转包单？', '确认取消', { type: 'warning' })
    await cancelRepackApi(order.value.id)
    ElMessage.success('已取消')
    fetchData()
  } catch { /* */ }
}

async function doTrace() {
  if (!traceKanbanNo.value) { ElMessage.warning('请输入看板号'); return }
  try {
    const res = await traceRepackApi(traceKanbanNo.value)
    traceResult.value = res.data
    if (!res.data.parentChain?.length && !res.data.childChain?.length) {
      ElMessage.info('该看板暂无转包记录')
    }
  } catch { /* */ }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.page-container { padding: 12px; }
.text-hint { color: #c0c4cc; font-size: 12px; }
.trace-bar { display: flex; align-items: center; margin-bottom: 8px; }
h4 { margin: 8px 0; color: #303133; }
</style>
