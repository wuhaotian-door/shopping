import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginResponse } from '@/types/api'
import { login as loginApi, checkAuth } from '@/api/auth'

const TOKEN_KEY = 'code1st_token'
const USER_KEY = 'code1st_user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref<LoginResponse | null>(
    JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
  )

  /** 是否已登录 */
  const isLoggedIn = computed(() => !!token.value)

  /** 是否为管理员 */
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')

  /** 是否为非遗传承人 */
  const isInheritor = computed(() => userInfo.value?.role === 'INHERITOR')

  /** 登录 */
  async function login(username: string, password: string) {
    const res = await loginApi({ username, password })
    token.value = res.data.token
    userInfo.value = res.data
    localStorage.setItem(TOKEN_KEY, res.data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(res.data))
  }

  /** 登出 */
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  /**
   * 应用启动时校验 localStorage 中的 token 是否仍然有效。
   * 如果后端已重启，旧 token 会被拦截器拒绝（iat < 服务启动时间），
   * 返回 401 → 触发 response 拦截器 → 自动清除登录状态。
   */
  async function initAuth() {
    if (!token.value) return
    try {
      const res = await checkAuth()
      // token 有效，用后端返回的最新用户信息更新本地存储
      userInfo.value = res.data
      localStorage.setItem(USER_KEY, JSON.stringify(res.data))
    } catch {
      // token 失效（后端重启或过期），response 拦截器已自动处理 logout
    }
  }

  return { token, userInfo, isLoggedIn, isAdmin, isInheritor, login, logout, initAuth }
})
