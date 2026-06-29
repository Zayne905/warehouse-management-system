import request from './request'
import type { WarehouseArea } from '@/types/inbound'

export function getAreaListApi(): Promise<{
  code: number
  message: string
  data: WarehouseArea[]
}> {
  return request.get('/warehouse-area/list')
}

export function saveAreaApi(area: WarehouseArea): Promise<{
  code: number
  message: string
  data: WarehouseArea
}> {
  return request.post('/warehouse-area/save', area)
}

export function deleteAreaApi(id: number): Promise<{
  code: number
  message: string
  data: null
}> {
  return request.post('/warehouse-area/delete', { id })
}
