import request from './request'
import type { User } from '@/types/inbound'

export function getUserListApi(): Promise<{
  code: number
  message: string
  data: User[]
}> {
  return request.get('/user/list')
}

export function saveUserApi(user: User): Promise<{
  code: number
  message: string
  data: User
}> {
  return request.post('/user/save', user)
}

export function deleteUserApi(id: number): Promise<{
  code: number
  message: string
  data: null
}> {
  return request.post('/user/delete', { id })
}
