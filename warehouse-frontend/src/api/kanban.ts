import request from './request'

export interface Kanban {
  id: number
  kanbanNo: string
  inboundOrderId: number
  inboundOrderNo: string
  outboundOrderId?: number
  outboundOrderNo?: string
  outboundScanTime?: string
  partId: number
  partCode: string
  partName: string
  supplierName: string
  quantity: number
  boxSeq: number
  warehouseAreaId: number
  warehouseAreaName: string
  status: number
  statusText: string
  createTime: string
}

export function listKanbansByOrder(orderId: number): Promise<{
  code: number
  message: string
  data: Kanban[]
}> {
  return request.post('/kanban/list-by-order', { orderId })
}

export function listKanbansByPart(partId: number): Promise<{
  code: number
  message: string
  data: Kanban[]
}> {
  return request.post('/kanban/list-by-part', { partId })
}

/** 封存看板 */
export function blockKanbanApi(kanbanNo: string): Promise<{
  code: number
  message: string
  data: null
}> {
  return request.post('/kanban/block', { kanbanNo })
}

/** 解封看板 */
export function unblockKanbanApi(kanbanNo: string): Promise<{
  code: number
  message: string
  data: null
}> {
  return request.post('/kanban/unblock', { kanbanNo })
}

/** 按零件批量封存 */
export function blockByPartApi(partId: number): Promise<{
  code: number
  message: string
  data: number
}> {
  return request.post('/kanban/block-by-part', { partId })
}

/** 批量解封 */
export function batchUnblockApi(kanbanNos: string[]): Promise<{
  code: number
  message: string
  data: number
}> {
  return request.post('/kanban/batch-unblock', { kanbanNos })
}

/** 扫码翻转封存/解封 */
export function toggleBlockApi(kanbanNo: string): Promise<{
  code: number
  message: string
  data: {
    kanbanNo: string
    partName: string
    partCode: string
    action: string
    previousStatus: number
    previousStatusText: string
    newStatus: number
  }
}> {
  return request.post('/kanban/toggle-block', { kanbanNo })
}

/** 看板状态映射 */
export const KanbanStatusText: Record<number, string> = {
  0: '待入库',
  1: '在库可用',
  2: '待出库',
  3: '已出库',
  4: '封存',
}

export const KanbanStatusTagType: Record<number, string> = {
  0: 'info',
  1: 'success',
  2: 'warning',
  3: 'danger',
  4: 'info',
}
