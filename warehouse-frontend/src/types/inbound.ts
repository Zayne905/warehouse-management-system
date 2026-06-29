// 入库状态枚举
export const InboundStatus = {
  PENDING: 0,       // 未入库
  PARTIAL: 1,       // 部分入库
  COMPLETED: 2,     // 已入库
} as const

export const InboundStatusText: Record<number, string> = {
  0: '未入库',
  1: '部分入库',
  2: '已入库',
}

export const InboundStatusTagType: Record<number, string> = {
  0: 'info',
  1: 'warning',
  2: 'success',
}

// 入库单 VO
export interface InboundOrderVO {
  id: number
  orderNo: string
  supplierId: number
  supplierName: string
  customerId?: number
  customerName?: string
  orderNumber: string
  status: number
  statusText: string
  remark: string
  createTime: string
  updateTime: string
  details?: InboundDetailVO[]
}

// 入库单明细 VO
export interface InboundDetailVO {
  id: number
  partId: number
  partCode: string
  partName: string
  unit: string
  plannedQty: number
  actualQty: number
  warehouseAreaId: number
  warehouseAreaName: string
  batchNo: string
  lineNo: number
  boxCount?: number
  packageCapacity?: number
}

// 入库单查询参数
export interface InboundQuery {
  current: number
  size: number
  orderNo?: string
  supplierId?: number
  orderNumber?: string
  status?: number
}

// 入库单保存 DTO
export interface InboundSaveDTO {
  id?: number
  supplierId: number
  customerId?: number
  orderNumber: string
  remark: string
  details: InboundDetailDTO[]
}

// 入库单明细 DTO
export interface InboundDetailDTO {
  partId: number
  plannedQty: number
  unit: string
  warehouseAreaId?: number
  batchNo?: string
  lineNo: number
  boxCount?: number
  actualQty?: number
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

// 供应商
export interface Supplier {
  id?: number
  code: string
  name: string
  contact?: string
  phone?: string
  address?: string
  enabled?: boolean
}

// 客户
export interface Customer {
  id?: number
  code: string
  name: string
  contact?: string
  phone?: string
  address?: string
  enabled?: boolean
}

// 物料/零件
export interface Part {
  id: number
  code: string
  name: string
  unit: string
  spec: string
  packageCapacity?: number
  minStock?: number
  maxStock?: number
  warehouseAreaId?: number
  warehouseAreaName?: string
  supplierId?: number
  supplierName?: string
}

// 库区
export interface WarehouseArea {
  id?: number
  code: string
  name: string
  warehouseId?: number
  warehouseName?: string
  enabled?: boolean
}

// 批量操作
export interface BatchOperationDTO {
  targetOrderId?: number
  sourceOrderId?: number
  warehouseAreaId?: number
  detailIds?: number[]
}

// 仓库
export interface Warehouse {
  id?: number
  code: string
  name: string
  address?: string
  adminName?: string
  enabled?: boolean
}

// 用户
export interface User {
  id?: number
  username: string
  password?: string
  nickname?: string
  role?: string
  enabled?: boolean
}

// 扫描记录
export interface ScanRecord {
  id: number
  inboundOrderId: number
  inboundOrderNo: string
  partId: number
  partCode: string
  partName: string
  batchNo: string
  scanQty: number
  scanTime: string
}
