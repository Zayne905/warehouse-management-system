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
        <el-tag :type="alert.tagType" size="small">{{ alert.tag }}</el-tag>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { WarningFilled, SuccessFilled, InfoFilled } from '@element-plus/icons-vue'
import type { KpiData } from '@/api/analytics'

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

  // Low stock (total boxes < 10 could indicate data issue or low inventory)
  if (d.totalBoxCount < 10 && d.totalBoxCount > 0) {
    result.push({
      key: 'lowStock',
      text: `在库箱数仅 ${d.totalBoxCount}，库存偏低`,
      tag: '关注',
      tagType: 'danger',
      icon: WarningFilled,
      level: 'danger',
    })
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
</style>
