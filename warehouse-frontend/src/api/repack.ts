import request from './request'
import type { RepackOrderVO, RepackOrderSaveDTO, RepackOrderQuery, RepackRelationVO } from '@/types/repack'
import type { PageResult } from '@/types/inbound'

/** 转包单列表 */
export function listRepackApi(params: RepackOrderQuery): Promise<{ code: number; message: string; data: PageResult<RepackOrderVO> }> {
  return request.post('/repack/list', params)
}

/** 转包单详情 */
export function getRepackDetailApi(id: number): Promise<{ code: number; message: string; data: RepackOrderVO }> {
  return request.post('/repack/detail', { id })
}

/** 创建转包单（支持空明细） */
export function saveRepackApi(data: RepackOrderSaveDTO): Promise<{ code: number; message: string; data: RepackOrderVO }> {
  return request.post('/repack/save', data)
}

/** 向已有转包单添加一行明细 */
export function addRepackDetailApi(orderId: number, sourceKanbanNo: string, transferQty: number): Promise<{ code: number; message: string; data: RepackOrderVO }> {
  return request.post('/repack/add-detail', { orderId, sourceKanbanNo, transferQty })
}

/** 删除转包单的一行明细 */
export function removeRepackDetailApi(detailId: number): Promise<{ code: number; message: string; data: RepackOrderVO }> {
  return request.post('/repack/remove-detail', { detailId })
}

/** 向下转包：输入目标箱容量自动生成拆分行 */
export function breakdownGenerateApi(orderId: number, targetBoxCapacity: number): Promise<{ code: number; message: string; data: RepackOrderVO }> {
  return request.post('/repack/breakdown-generate', { orderId, targetBoxCapacity })
}

/** 删除转包单 */
export function deleteRepackApi(id: number): Promise<{ code: number; message: string; data: null }> {
  return request.post('/repack/delete', { id })
}

/** 取消转包单 */
export function cancelRepackApi(id: number): Promise<{ code: number; message: string; data: null }> {
  return request.post('/repack/cancel', { id })
}

/** 确认执行转包 */
export function confirmRepackApi(id: number): Promise<{ code: number; message: string; data: RepackOrderVO }> {
  return request.post('/repack/confirm', { id })
}

/** 扫码预览看板信息 */
export function previewRepackApi(kanbanNo: string): Promise<{ code: number; message: string; data: any }> {
  return request.post('/repack/preview', { kanbanNo })
}

/** 追溯看板转包链 */
export function traceRepackApi(kanbanNo: string): Promise<{ code: number; message: string; data: RepackRelationVO }> {
  return request.post('/repack/trace', { kanbanNo })
}
