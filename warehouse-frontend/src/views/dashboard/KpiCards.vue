<template>
  <div class="kpi-row">
    <el-card v-for="card in cards" :key="card.key" class="kpi-card" shadow="hover">
      <div class="kpi-inner">
        <div class="kpi-icon" :style="{ backgroundColor: card.bg }">
          <el-icon :size="24"><component :is="card.icon" /></el-icon>
        </div>
        <div class="kpi-info">
          <div class="kpi-label">{{ card.label }}</div>
          <div class="kpi-value">
            <span v-if="!loading">{{ card.value }}</span>
            <el-skeleton v-else animated :rows="1" style="width: 60px" />
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Download, Upload, Box, List, DataLine, Clock
} from '@element-plus/icons-vue'
import type { KpiData } from '@/api/analytics'

const props = defineProps<{
  data: KpiData | null
  loading: boolean
}>()

const cards = computed(() => {
  const d = props.data
  return [
    {
      key: 'todayInbound',
      label: '今日入库单',
      value: d?.todayInbound ?? '-',
      icon: Download,
      bg: '#e6f7ff',
    },
    {
      key: 'todayOutbound',
      label: '今日出库单',
      value: d?.todayOutbound ?? '-',
      icon: Upload,
      bg: '#fff7e6',
    },
    {
      key: 'totalStock',
      label: '库存总量',
      value: d?.totalStock != null ? Number(d.totalStock).toLocaleString() : '-',
      icon: Box,
      bg: '#f6ffed',
    },
    {
      key: 'totalBoxCount',
      label: '在库箱数',
      value: d?.totalBoxCount ?? '-',
      icon: List,
      bg: '#f0f5ff',
    },
    {
      key: 'pendingInbound',
      label: '待处理入库',
      value: d?.pendingInbound ?? '-',
      icon: Clock,
      bg: '#fff1f0',
    },
    {
      key: 'monthTotal',
      label: '本月订单',
      value: d ? (d.monthInbound + d.monthOutbound) : '-',
      icon: DataLine,
      bg: '#f9f0ff',
    },
  ]
})
</script>

<style scoped>
.kpi-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

@media (max-width: 1400px) {
  .kpi-row {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .kpi-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

.kpi-card {
  cursor: default;
}

.kpi-card :deep(.el-card__body) {
  padding: 16px;
}

.kpi-inner {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kpi-info {
  flex: 1;
  min-width: 0;
}

.kpi-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.kpi-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}
</style>
