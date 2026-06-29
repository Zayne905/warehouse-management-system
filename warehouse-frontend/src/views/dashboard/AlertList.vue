<template>
  <el-card shadow="never">
    <template #header>
      <span>⚠️ 提醒中心</span>
    </template>
    <div class="alert-body">
      <div v-if="alerts.length === 0" class="alert-empty">
        <el-icon :size="32" color="#67C23A"><SuccessFilled /></el-icon>
        <p>一切运行正常</p>
      </div>
      <div v-for="alert in alerts" :key="alert.key" class="alert-item" :class="alert.level">
        <el-icon><component :is="alert.icon" /></el-icon>
        <span class="alert-text">{{ alert.text }}</span>
        <el-tag
          v-if="alert.clickable"
          :type="alert.tagType"
          size="small"
          class="clickable-tag"
          @click="goToStock"
        >
          {{ alert.tag }}
        </el-tag>
        <el-tag v-else :type="alert.tagType" size="small">{{ alert.tag }}</el-tag>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { WarningFilled, SuccessFilled, InfoFilled } from '@element-plus/icons-vue'
import type { KpiData, ThresholdAlert } from '@/api/analytics'

const router = useRouter()
const props = defineProps<{
  kpiData: KpiData | null
}>()

interface Alert {
  key: string
  text: string
  tag: string
  tagType: 'warning' | 'danger' | 'info' | 'success'
  icon: any
  level: string
  clickable?: boolean
}

function goToStock() {
  router.push('/inventory/stock')
}

const alerts = computed<Alert[]>(() => {
  const result: Alert[] = []
  const d = props.kpiData
  if (!d) return result

  // Pending orders alert
  if (d.pendingInbound > 0) {
    result.push({
      key: 'pendingInbound',
      text: `有 ${d.pendingInbound} 个入库单待处理`,
      tag: '待处理',
      tagType: 'warning',
      icon: WarningFilled,
      level: 'warning',
    })
  }

  // Low stock alerts — per-part threshold based
  if (d.lowStockCount > 0) {
    const parts = d.lowStockDetails || []
    if (parts.length > 0) {
      parts.slice(0, 3).forEach((p: ThresholdAlert) => {
        result.push({
          key: `lowStock_${p.partId}`,
          text: `${p.partName}(${p.partCode}) 当前库存 ${p.currentStock}，低于最低储备 ${p.threshold}`,
          tag: '低储',
          tagType: 'danger',
          icon: WarningFilled,
          level: 'danger',
        })
      })
      if (parts.length > 3) {
        result.push({
          key: 'lowStock_more',
          text: `还有 ${parts.length - 3} 个零件库存过低`,
          tag: `查看全部 ${d.lowStockCount} 个 →`,
          tagType: 'danger',
          icon: WarningFilled,
          level: 'danger',
          clickable: true,
        })
      }
    } else {
      result.push({
        key: 'lowStock',
        text: `${d.lowStockCount} 个零件库存低于最低储备`,
        tag: '低储',
        tagType: 'danger',
        icon: WarningFilled,
        level: 'danger',
      })
    }
  }

  // High stock alerts — per-part threshold based
  if (d.highStockCount > 0) {
    const parts = d.highStockDetails || []
    if (parts.length > 0) {
      parts.slice(0, 3).forEach((p: ThresholdAlert) => {
        result.push({
          key: `highStock_${p.partId}`,
          text: `${p.partName}(${p.partCode}) 当前库存 ${p.currentStock}，高于最高储备 ${p.threshold}`,
          tag: '高储',
          tagType: 'warning',
          icon: WarningFilled,
          level: 'warning',
        })
      })
      if (parts.length > 3) {
        result.push({
          key: 'highStock_more',
          text: `还有 ${parts.length - 3} 个零件库存过高`,
          tag: `查看全部 ${d.highStockCount} 个 →`,
          tagType: 'warning',
          icon: WarningFilled,
          level: 'warning',
          clickable: true,
        })
      }
    } else {
      result.push({
        key: 'highStock',
        text: `${d.highStockCount} 个零件库存高于最高储备`,
        tag: '高储',
        tagType: 'warning',
        icon: WarningFilled,
        level: 'warning',
      })
    }
  }

  // No activity today
  if (d.todayInbound === 0 && d.todayOutbound === 0) {
    result.push({
      key: 'noActivity',
      text: '今日尚无出入库操作',
      tag: '提示',
      tagType: 'info',
      icon: InfoFilled,
      level: 'info',
    })
  }

  return result
})
</script>

<style scoped>
.alert-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alert-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: #67C23A;
}

.alert-empty p {
  margin: 8px 0 0;
  font-size: 14px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
}

.alert-item.warning {
  background-color: #fdf6ec;
  border-left: 3px solid #E6A23C;
}

.alert-item.danger {
  background-color: #fef0f0;
  border-left: 3px solid #F56C6C;
}

.alert-item.info {
  background-color: #f4f4f5;
  border-left: 3px solid #909399;
}

.alert-text {
  flex: 1;
  color: #303133;
}

.clickable-tag {
  cursor: pointer;
}
.clickable-tag:hover {
  opacity: 0.8;
}
</style>
