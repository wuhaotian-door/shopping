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

  /** 全局唯一标识：role_id，确保不同角色表 ID 不冲突 */
  const uid = computed(() => {
    if (!userInfo.value) return ''
    return `${userInfo.value.role}_${userInfo.value.id}`
  })

  /** 登录 */
  async function login(username: string, password: string) {
    const res = await loginApi({ username, password })
    token.value = res.data.token
    userInfo.value = res.data
    localStorage.setItem(TOKEN_KEY, res.data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(res.data))
    // 恢复该用户的购物车（使用全局唯一 uid）
    import('@/stores/cart').then(m => m.useCartStore().loadForUser(`${res.data.role}_${res.data.id}`))
  }

  /** 登出 */
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    import('@/stores/cart').then(m => m.useCartStore().clear())
  }

  async function initAuth() {
    if (!token.value) return
    try {
      const res = await checkAuth()
      userInfo.value = res.data
      localStorage.setItem(USER_KEY, JSON.stringify(res.data))
      import('@/stores/cart').then(m => m.useCartStore().loadForUser(`${res.data.role}_${res.data.id}`))
    } catch {
      // token 失效
    }
  }

  return { token, userInfo, isLoggedIn, isAdmin, isInheritor, uid, login, logout, initAuth }
})
