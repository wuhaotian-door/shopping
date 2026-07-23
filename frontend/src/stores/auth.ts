import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type { LoginResponse } from '@/types/api'
import { login as loginApi, checkAuth } from '@/api/auth'

const ACCOUNTS_KEY = 'code1st_accounts'
const ACTIVE_KEY = 'code1st_active'

interface StoredAccount {
  id: number
  role: string
  username: string
  name: string
  token: string
  avatarUrl?: string
}

function loadAccounts(): StoredAccount[] {
  try { return JSON.parse(localStorage.getItem(ACCOUNTS_KEY) || '[]') }
  catch { return [] }
}
function saveAccounts(accounts: StoredAccount[]) {
  localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(accounts))
}

export const useAuthStore = defineStore('auth', () => {
  const accounts = ref<StoredAccount[]>(loadAccounts())
  const activeIndex = ref<number>(parseInt(localStorage.getItem(ACTIVE_KEY) || '0') || 0)

  const activeAccount = computed(() => accounts.value[activeIndex.value] || null)

  // 独立 avatarUrl ref，跟随 activeAccount 变化
  const avatarUrl = ref(activeAccount.value?.avatarUrl || '')
  watch(activeAccount, (a) => { avatarUrl.value = a?.avatarUrl || '' }, { immediate: true })

  const token = computed(() => activeAccount.value?.token || '')

  const userInfo = computed<LoginResponse | null>(() => {
    const a = activeAccount.value
    if (!a) return null
    return { id: a.id, userName: a.username, name: a.name, token: a.token, role: a.role as any }
  })

  const isLoggedIn = computed(() => !!token.value)

  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')

  const isInheritor = computed(() => userInfo.value?.role === 'INHERITOR')

  const uid = computed(() => {
    if (!userInfo.value) return ''
    return userInfo.value.role + '_' + userInfo.value.id
  })

  async function login(username: string, password: string) {
    const res = await loginApi({ username, password })
    const data = res.data
    const newAccount: StoredAccount = {
      id: data.id, role: data.role, username: data.userName,
      name: data.name, token: data.token,
    }
    // 先写入账号（让 interceptor 能获取到新 token）
    const existIdx = accounts.value.findIndex(a => a.id === data.id && a.role === data.role)
    if (existIdx >= 0) { accounts.value[existIdx] = newAccount; activeIndex.value = existIdx }
    else { accounts.value.push(newAccount); activeIndex.value = accounts.value.length - 1 }
    persist()
    // 再获取头像（此时 interceptor 已能读到新 token）
    try {
      if (data.role === 'INHERITOR') {
        const { getMyProfile } = await import('@/api/profile')
        const p = await getMyProfile()
        if (p.data?.avatarUrl) { newAccount.avatarUrl = p.data.avatarUrl; accounts.value[activeIndex.value] = { ...newAccount }; persist() }
      } else {
        const { getMyProfile } = await import('@/api/user')
        const p = await getMyProfile()
        if (p.data?.avatarUrl) { newAccount.avatarUrl = p.data.avatarUrl; accounts.value[activeIndex.value] = { ...newAccount }; persist() }
      }
    } catch { /* ignore */ }
    import('@/stores/cart').then(m => m.useCartStore().loadForUser(data.role + '_' + data.id))
  }

  function switchAccount(index: number) {
    if (index >= 0 && index < accounts.value.length) {
      activeIndex.value = index
      localStorage.setItem(ACTIVE_KEY, String(index))
      const a = accounts.value[index]
      import('@/stores/cart').then(m => m.useCartStore().loadForUser(a.role + '_' + a.id))
    }
  }

  // 退出登录：保留账号信息（仅清空 token），下次可直接切回
  function logout() {
    const idx = activeIndex.value
    const a = accounts.value[idx]
    if (a) {
      accounts.value[idx] = { ...a, token: '' }
      persist()
    }
    import('@/stores/cart').then(m => m.useCartStore().clear())
  }

  // 完全删除账号
  function removeAccount(index: number) {
    if (index < 0 || index >= accounts.value.length) return
    accounts.value.splice(index, 1)
    if (index === activeIndex.value) {
      activeIndex.value = Math.min(index, accounts.value.length - 1)
    }
    persist()
  }

  function persist() {
    saveAccounts(accounts.value)
    localStorage.setItem(ACTIVE_KEY, String(activeIndex.value))
  }

  async function initAuth() {
    if (accounts.value.length === 0) return
    const idx = activeIndex.value
    const a = accounts.value[idx]
    if (a && a.token) {
      try {
        let apiAvatar = ''
        if (a.role === 'INHERITOR') {
          const { getMyProfile } = await import('@/api/profile')
          const res = await getMyProfile()
          apiAvatar = res.data?.avatarUrl || ''
        } else {
          const { getMyProfile } = await import('@/api/user')
          const res = await getMyProfile()
          apiAvatar = res.data?.avatarUrl || ''
        }
        if (apiAvatar && apiAvatar !== a.avatarUrl) {
          accounts.value[idx] = { ...a, avatarUrl: apiAvatar }
          avatarUrl.value = apiAvatar
          persist()
        }
      } catch { /* ignore */ }
      import('@/stores/cart').then(m => m.useCartStore().loadForUser(a.role + '_' + a.id))
    }
  }

  function updateAvatar(url: string) {
    const idx = activeIndex.value
    const a = accounts.value[idx]
    if (!a) return
    accounts.value[idx] = { ...a, avatarUrl: url }
    avatarUrl.value = url
    persist()
  }

  return { accounts, activeIndex, activeAccount, avatarUrl, token, userInfo, isLoggedIn, isAdmin, isInheritor, uid, login, switchAccount, logout, removeAccount, initAuth, updateAvatar }
})
