import request from './request'
import type { Warehouse } from '@/types/inbound'

export function getWarehouseListApi(): Promise<{
  code: number
  message: string
  data: Warehouse[]
}> {
  return request.get('/warehouse/list')
}

export function saveWarehouseApi(warehouse: Warehouse): Promise<{
  code: number
  message: string
  data: Warehouse
}> {
  return request.post('/warehouse/save', warehouse)
}

export function deleteWarehouseApi(id: number): Promise<{
  code: number
  message: string
  data: null
}> {
  return request.post('/warehouse/delete', { id })
}
