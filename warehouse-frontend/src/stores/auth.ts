import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, type LoginParams } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')
  const role = ref(localStorage.getItem('role') || '')

  async function login(params: LoginParams): Promise<boolean> {
    try {
      const res = await loginApi(params)
      token.value = res.data.token
      nickname.value = res.data.nickname
      role.value = res.data.role || 'user'
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('nickname', res.data.nickname)
      localStorage.setItem('role', res.data.role || 'user')
      return true
    } catch {
      return false
    }
  }

  function logout() {
    token.value = ''
    nickname.value = ''
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('nickname')
    localStorage.removeItem('role')
  }

  return { token, nickname, role, login, logout }
})
