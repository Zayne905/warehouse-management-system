<template>
  <div class="dashboard-container">
    <!-- KPI Cards Row -->
    <KpiCards :data="dashboardStore.kpiData" :loading="dashboardStore.loading" />

    <!-- Two-column layout: Charts + Chat -->
    <div class="dashboard-main">
      <div class="dashboard-left">
        <TrendCharts :data="dashboardStore.trendData" :loading="dashboardStore.loading" />
        <AlertList :kpi-data="dashboardStore.kpiData" />
      </div>
      <div class="dashboard-right">
        <ChatPanel />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import KpiCards from './KpiCards.vue'
import TrendCharts from './TrendCharts.vue'
import AlertList from './AlertList.vue'
import ChatPanel from './ChatPanel.vue'

const dashboardStore = useDashboardStore()

onMounted(() => {
  dashboardStore.loadAll()
})
</script>

<style scoped>
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dashboard-main {
  display: flex;
  gap: 16px;
  min-height: 0;
}

.dashboard-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.dashboard-right {
  width: 420px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

@media (max-width: 1200px) {
  .dashboard-main {
    flex-direction: column;
  }
  .dashboard-right {
    width: 100%;
    min-height: 500px;
  }
}
</style>
