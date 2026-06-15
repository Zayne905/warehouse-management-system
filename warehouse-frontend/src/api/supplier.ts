import request from './request'
import type { Supplier } from '@/types/inbound'

export function getSupplierListApi(): Promise<{
  code: number
  message: string
  data: Supplier[]
}> {
  return request.get('/supplier/list')
}
