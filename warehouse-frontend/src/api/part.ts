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
