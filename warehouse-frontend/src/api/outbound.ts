import request from './request'
import type { Kanban } from './kanban'

export interface OutboundOrderVO {
  id: number
  orderNo: string
  status: number
  statusText: string
  remark: string
  customerName?: string
  suppliers?: string[]
  createTime: string
  partCount: number
  totalQty: number
  totalKanbans?: number
  outboundCount?: number
  details?: OutboundDetailVO[]
  pendingKanbans?: Kanban[]
  scans?: OutboundScanVO[]
}

export interface OutboundDetailVO {
  id: number
  partId: number
  partCode: string
  partName: string
  unit: string
  plannedQty: number
  actualQty: number
  boxCount: number
  warehouseAreaId?: number
  lineNo: number
  availableStock: number
}

export interface OutboundScanVO {
  id: number
  outboundOrderId: number
  outboundOrderNo: string
  kanbanNo: string
  partId: number
  partCode: string
  partName: string
  quantity: number
  warehouseAreaId?: number
  warehouseAreaName?: string
  scanTime: string
  operatorId: number
}

export function listOutboundApi(params: any): Promise<{ code: number; message: string; data: any }> {
  return request.post('/outbound-order/list', params)
}

export function getOutboundDetailApi(id: number): Promise<{ code: number; message: string; data: any }> {
  return request.post('/outbound-order/detail', { id })
}

export function saveOutboundApi(data: any): Promise<{ code: number; message: string; data: any }> {
  return request.post('/outbound-order/save', data)
}

export function deleteOutboundApi(id: number): Promise<{ code: number; message: string; data: null }> {
  return request.post('/outbound-order/delete', { id })
}

export function cancelOutboundApi(id: number): Promise<{ code: number; message: string; data: null }> {
  return request.post('/outbound-order/cancel', { id })
}

export function scanOutboundApi(params: { orderId?: number; kanbanNo?: string; operatorId?: number }): Promise<{ code: number; message: string; data: any }> {
  return request.post('/outbound/scan', params)
}

export function getAvailableStockApi(partId: number): Promise<{ code: number; message: string; data: number }> {
  return request.post('/outbound/available-stock', { partId })
}

/** 获取出库单的待出库清单 */
export function getPendingKanbansApi(id: number): Promise<{ code: number; message: string; data: Kanban[] }> {
  return request.post('/outbound-order/pending-kanbans', { id })
}

/** 出库单状态文本 */
export const OutboundStatusText: Record<number, string> = {
  0: '未出库',
  1: '部分出库',
  2: '已出库',
  3: '作废',
}

export const OutboundStatusTagType: Record<number, string> = {
  0: 'info',
  1: 'warning',
  2: 'success',
  3: 'danger',
}
