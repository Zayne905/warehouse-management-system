import request from './request'
import type {
  InboundQuery,
  InboundSaveDTO,
  InboundOrderVO,
  PageResult,
  BatchOperationDTO,
  ScanRecord,
} from '@/types/inbound'

// 入库单列表 (POST 方式传查询参数)
export function listInboundApi(params: InboundQuery): Promise<{
  code: number
  message: string
  data: PageResult<InboundOrderVO>
}> {
  return request.post('/inbound-order/list', params)
}

// 保存入库单（新增/修改）
export function saveInboundApi(params: InboundSaveDTO): Promise<{
  code: number
  message: string
  data: InboundOrderVO
}> {
  return request.post('/inbound-order/save', params)
}

// 提交入库单
export function submitInboundApi(id: number): Promise<{
  code: number
  message: string
  data: InboundOrderVO
}> {
  return request.post('/inbound-order/submit', { id })
}

// 入库单详情
export function getInboundDetailApi(id: number): Promise<{
  code: number
  message: string
  data: InboundOrderVO
}> {
  return request.post('/inbound-order/detail', { id })
}

// 删除入库单
export function deleteInboundApi(id: number): Promise<{
  code: number
  message: string
  data: null
}> {
  return request.post('/inbound-order/delete', { id })
}

// 作废入库单
export function cancelInboundApi(id: number): Promise<{
  code: number
  message: string
  data: null
}> {
  return request.post('/inbound-order/cancel', { id })
}

// 批量复制零件
export function batchCopyPartsApi(dto: BatchOperationDTO): Promise<{
  code: number
  message: string
  data: null
}> {
  return request.post('/inbound-order/batch-copy-parts', dto)
}

// 批量设置库区
export function batchSetAreaApi(dto: BatchOperationDTO): Promise<{
  code: number
  message: string
  data: null
}> {
  return request.post('/inbound-order/batch-set-area', dto)
}

// ============ 扫描相关 API ============

// 检查扫描重复
export function checkScanDuplicateApi(params: {
  inboundOrderId: number
  partCode: string
  batchNo: string
}): Promise<{
  code: number
  message: string
  data: Record<string, any>
}> {
  return request.post('/scan/check-duplicate', params)
}

// 提交扫描
export function submitScanApi(params: {
  inboundOrderId: number
  inboundOrderNo: string
  partCode: string
  batchNo: string
  scanQty: number
  operatorId: number
}): Promise<{
  code: number
  message: string
  data: Record<string, any>
}> {
  return request.post('/scan/submit', params)
}

// 扫描列表
export function listScansApi(inboundOrderId: number): Promise<{
  code: number
  message: string
  data: ScanRecord[]
}> {
  return request.post('/scan/list', { inboundOrderId })
}

// 删除扫描记录
export function deleteScanApi(scanRecordId: number): Promise<{
  code: number
  message: string
  data: null
}> {
  return request.post('/scan/delete', { scanRecordId })
}

// 扫描回显
export function getScanFeedbackApi(orderNo: string, partCode?: string): Promise<{
  code: number
  message: string
  data: Record<string, any>
}> {
  return request.post('/scan/feedback', { orderNo, partCode })
}
