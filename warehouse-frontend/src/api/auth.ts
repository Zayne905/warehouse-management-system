import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  nickname: string
}

export interface UserInfo {
  username: string
  nickname: string
}

export function loginApi(params: LoginParams): Promise<{
  code: number
  message: string
  data: LoginResult
}> {
  return request.post('/auth/login', params)
}

export function getUserInfoApi(): Promise<{
  code: number
  message: string
  data: UserInfo
}> {
  return request.get('/user/info')
}
