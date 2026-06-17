import request from './request'
import type { Supplier } from '@/types/inbound'

export function getSupplierListApi(): Promise<{
  code: number
  message: string
  data: Supplier[]
}> {
  return request.get('/supplier/list')
}

export function saveSupplierApi(supplier: Supplier): Promise<{
  code: number
  message: string
  data: Supplier
}> {
  return request.post('/supplier/save', supplier)
}

export function deleteSupplierApi(id: number): Promise<{
  code: number
  message: string
  data: null
}> {
  return request.post('/supplier/delete', { id })
}
