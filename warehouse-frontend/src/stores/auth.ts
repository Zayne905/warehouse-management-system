import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, type LoginParams } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const nickname = ref('')
  const role = ref('')

  async function login(params: LoginParams): Promise<boolean> {
    try {
      const res = await loginApi(params)
      token.value = res.data.token
      nickname.value = res.data.nickname
      role.value = (res.data as any).role || 'user'
      localStorage.setItem('token', res.data.token)
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
  }

  return { token, nickname, role, login, logout }
})
