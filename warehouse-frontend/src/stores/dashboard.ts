import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getKpiApi, getTrendApi, type KpiData, type TrendItem } from '@/api/analytics'

export const useDashboardStore = defineStore('dashboard', () => {
  const kpiData = ref<KpiData | null>(null)
  const trendData = ref<TrendItem[]>([])
  const loading = ref(false)

  async function loadKpi() {
    try {
      const res = await getKpiApi()
      kpiData.value = res.data
    } catch {
      // handled by interceptor
    }
  }

  async function loadTrend(days: number = 7) {
    try {
      const res = await getTrendApi(days)
      trendData.value = res.data.trend || []
    } catch {
      // handled by interceptor
    }
  }

  async function loadAll() {
    loading.value = true
    await Promise.all([loadKpi(), loadTrend()])
    loading.value = false
  }

  return { kpiData, trendData, loading, loadKpi, loadTrend, loadAll }
})
