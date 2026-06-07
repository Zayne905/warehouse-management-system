import request from './request'
import type { WarehouseArea } from '@/types/inbound'

export function getAreaListApi(): Promise<{
  code: number
  message: string
  data: WarehouseArea[]
}> {
  return request.get('/warehouse-area/list')
}
