import request from './request'

export interface Kanban {
  id: number
  kanbanNo: string
  inboundOrderId: number
  inboundOrderNo: string
  partId: number
  partCode: string
  partName: string
  supplierName: string
  quantity: number
  boxSeq: number
  warehouseAreaId: number
  warehouseAreaName: string
  status: number
  createTime: string
}

export function listKanbansByOrder(orderId: number): Promise<{
  code: number
  message: string
  data: Kanban[]
}> {
  return request.post('/kanban/list-by-order', { orderId })
}
