import request from './request'

export interface KpiData {
  todayInbound: number
  todayOutbound: number
  todayInboundQty: number
  todayOutboundQty: number
  monthInbound: number
  monthOutbound: number
  totalStock: number
  totalBoxCount: number
  pendingInbound: number
}

export interface TrendItem {
  date: string
  inboundOrders: number
  outboundOrders: number
  inboundQuantity: number
  outboundQuantity: number
}

export interface TrendData {
  days: number
  trend: TrendItem[]
}

export interface SummaryData {
  date: string
  inboundOrderCount: number
  inboundCompletedCount: number
  inboundPendingCount: number
  inboundQuantity: number
  outboundOrderCount: number
  outboundCompletedCount: number
  outboundQuantity: number
  totalBoxCount: number
  totalStock: number
}

export function getKpiApi() {
  return request.get<KpiData>('/analytics/kpi')
}

export function getTrendApi(days: number = 7) {
  return request.get<TrendData>('/analytics/trend', { params: { days } })
}

export function getSummaryApi() {
  return request.get<SummaryData>('/analytics/summary')
}
