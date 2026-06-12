import request from './request'

export interface OutboundOrderVO {
  id: number
  orderNo: string
  status: number
  statusText: string
  remark: string
  createTime: string
  partCount: number
  totalQty: number
  details?: OutboundDetailVO[]
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
