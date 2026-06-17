import request from './request'
import type { Customer } from '@/types/inbound'

export function getCustomerListApi(): Promise<{
  code: number
  message: string
  data: Customer[]
}> {
  return request.get('/customer/list')
}

export function saveCustomerApi(customer: Customer): Promise<{
  code: number
  message: string
  data: Customer
}> {
  return request.post('/customer/save', customer)
}

export function deleteCustomerApi(id: number): Promise<{
  code: number
  message: string
  data: null
}> {
  return request.post('/customer/delete', { id })
}
