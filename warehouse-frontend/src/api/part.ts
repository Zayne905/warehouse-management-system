import request from './request'
import type { Part } from '@/types/inbound'

export function getPartListApi(supplierId?: number): Promise<{
  code: number
  message: string
  data: Part[]
}> {
  const params = supplierId ? { supplierId } : {}
  return request.get('/part/list', { params })
}

export function savePartApi(part: Part): Promise<{
  code: number
  message: string
  data: Part
}> {
  return request.post('/part/save', part)
}

export function deletePartApi(id: number): Promise<{
  code: number
  message: string
  data: null
}> {
  return request.post('/part/delete', { id })
}
