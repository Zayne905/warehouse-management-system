import request from './request'

export interface AreaStock {
  areaId: number
  areaName: string
  quantity: number
}

export interface InventoryVO {
  partId: number
  partCode: string
  partName: string
  spec: string
  unit: string
  packageCapacity: number
  totalStock: number
  kanbanCount: number        // 在库箱数
  avgQtyPerBox: number        // 箱均数量
  areaStocks: AreaStock[]
}

export function getStockListApi(keyword?: string): Promise<{
  code: number
  message: string
  data: InventoryVO[]
}> {
  return request.post('/inventory/stock-list', { keyword: keyword || '' })
}
