<template>
  <el-card shadow="never">
    <template #header>
      <div class="chart-header">
        <span>📈 出入库趋势</span>
        <el-radio-group v-model="days" size="small" @change="loadTrend">
          <el-radio-button :value="7">近7天</el-radio-button>
          <el-radio-button :value="30">近30天</el-radio-button>
        </el-radio-group>
      </div>
    </template>
    <div v-loading="loading" class="chart-body">
      <v-chart v-if="!loading && chartOption" :option="chartOption" autoresize style="height: 300px" />
      <el-empty v-else-if="!loading" description="暂无趋势数据" />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, LegendComponent, GridComponent,
} from 'echarts/components'
import { getTrendApi, type TrendItem } from '@/api/analytics'

use([CanvasRenderer, BarChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const props = defineProps<{
  data: TrendItem[]
  loading: boolean
}>()

const days = ref(7)
const trendLocal = ref<TrendItem[]>([])
const chartLoading = ref(false)

async function loadTrend() {
  chartLoading.value = true
  try {
    const res = await getTrendApi(days.value)
    trendLocal.value = res.data.trend || []
  } catch {
    // handled by interceptor
  }
  chartLoading.value = false
}

// Merge prop data or fetched data
const displayData = computed(() => trendLocal.value.length > 0 ? trendLocal.value : props.data)

const chartOption = computed(() => {
  const data = displayData.value
  if (!data || data.length === 0) return null

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    legend: {
      data: ['入库数量', '出库数量', '入库单数', '出库单数'],
      bottom: 0,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '12%',
      top: '4%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.date.substring(5)), // MM-DD
    },
    yAxis: [
      {
        type: 'value',
        name: '数量',
      },
      {
        type: 'value',
        name: '单数',
      },
    ],
    series: [
      {
        name: '入库数量',
        type: 'bar',
        data: data.map((d) => d.inboundQuantity),
        itemStyle: { color: '#409EFF' },
        barMaxWidth: 20,
      },
      {
        name: '出库数量',
        type: 'bar',
        data: data.map((d) => d.outboundQuantity),
        itemStyle: { color: '#E6A23C' },
        barMaxWidth: 20,
      },
      {
        name: '入库单数',
        type: 'line',
        yAxisIndex: 1,
        data: data.map((d) => d.inboundOrders),
        lineStyle: { color: '#67C23A' },
        itemStyle: { color: '#67C23A' },
      },
      {
        name: '出库单数',
        type: 'line',
        yAxisIndex: 1,
        data: data.map((d) => d.outboundOrders),
        lineStyle: { color: '#F56C6C' },
        itemStyle: { color: '#F56C6C' },
      },
    ],
  }
})
</script>

<style scoped>
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-body {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
