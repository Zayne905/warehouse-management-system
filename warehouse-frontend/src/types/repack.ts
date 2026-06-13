// 转包状态枚举
export const RepackStatus = {
  PENDING: 0,       // 待转包
  COMPLETED: 1,     // 已完成
  CANCELLED: 2,     // 已取消
} as const

export const RepackStatusText: Record<number, string> = {
  0: '待转包',
  1: '已完成',
  2: '已取消',
}

export const RepackStatusTagType: Record<number, string> = {
  0: 'warning',
  1: 'success',
  2: 'danger',
}

// 转包类型（三种）
export const RepackTypeOptions = [
  { value: 'BREAKDOWN', label: '向下转包（拆包1→N）' },
  { value: 'CONSOLIDATE', label: '向上转包（合并N→1）' },
  { value: 'REMAINDER', label: '带余量转包（1→1）' },
]

export const RepackTypeText: Record<string, string> = {
  BREAKDOWN: '向下转包（拆包）',
  CONSOLIDATE: '向上转包（合并）',
  REMAINDER: '带余量转包',
}

// 转包单 VO
export interface RepackOrderVO {
  id: number
  orderNo: string
  status: number
  statusText: string
  repackType: string
  repackTypeText: string
  outboundOrderId?: number
  outboundOrderNo?: string
  remark: string
  operatorId?: number
  detailCount: number
  totalTransferQty: number
  createTime: string
  updateTime: string
  details?: RepackDetailVO[]
}

// 转包明细 VO
export interface RepackDetailVO {
  id: number
  repackOrderId: number
  sourceKanbanId: number
  sourceKanbanNo: string
  sourceOriginalQty?: number
  targetKanbanNo?: string
  partId: number
  partCode: string
  partName: string
  transferQty: number
  remainderQty: number
  sourceRemainingQty: number
  lineNo: number
  createTime: string
}

// 转包单查询参数
export interface RepackOrderQuery {
  current: number
  size: number
  orderNo?: string
  status?: number
  repackType?: string
}

// 转包单保存 DTO
export interface RepackOrderSaveDTO {
  id?: number
  repackType: string
  partId?: number
  warehouseAreaId?: number
  targetBoxCapacity?: number
  outboundOrderNo?: string
  remark: string
  operatorId?: number
  details?: RepackDetailDTO[]
}

// 转包明细 DTO
export interface RepackDetailDTO {
  sourceKanbanId?: number
  sourceKanbanNo: string
  transferQty: number
  remainderQty?: number
  lineNo: number
}

// 追溯节点
export interface RepackRelationNode {
  relationId: number
  parentKanbanNo: string
  childKanbanNo: string
  repackOrderNo: string
  transferQty: number
  partCode: string
  partName: string
  repackTime: string
  level: number
}

// 追溯结果
export interface RepackRelationVO {
  kanbanNo: string
  currentKanban: {
    kanbanNo: string
    partCode: string
    partName: string
    quantity: number
    warehouseAreaName: string
    supplierName: string
    statusText: string
    inboundOrderNo: string
    createTime: string
  }
  parentChain: RepackRelationNode[]
  childChain: RepackRelationNode[]
}
