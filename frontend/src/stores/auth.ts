import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
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

  const token = computed(() => activeAccount.value?.token || '')

  const userInfo = computed<LoginResponse | null>(() => {
    const a = activeAccount.value
    if (!a) return null
    return { id: a.id, userName: a.username, name: a.name, token: a.token, role: a.role as any }
  })

  const isLoggedIn = computed(() => accounts.value.length > 0 && !!activeAccount.value)

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
      id: data.id,
      role: data.role,
      username: data.userName,
      name: data.name,
      token: data.token,
    }
    const existIdx = accounts.value.findIndex(a => a.id === data.id && a.role === data.role)
    if (existIdx >= 0) {
      accounts.value[existIdx] = newAccount
      activeIndex.value = existIdx
    } else {
      accounts.value.push(newAccount)
      activeIndex.value = accounts.value.length - 1
    }
    persist()
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

  function logout() {
    const idx = activeIndex.value
    if (idx >= 0 && idx < accounts.value.length) {
      accounts.value.splice(idx, 1)
      if (accounts.value.length === 0) {
        activeIndex.value = 0
      } else if (idx >= accounts.value.length) {
        activeIndex.value = accounts.value.length - 1
      }
      persist()
    }
    import('@/stores/cart').then(m => m.useCartStore().clear())
  }

  function persist() {
    saveAccounts(accounts.value)
    localStorage.setItem(ACTIVE_KEY, String(activeIndex.value))
  }

  async function initAuth() {
    if (accounts.value.length === 0) return
    const a = activeAccount.value
    if (a) {
      import('@/stores/cart').then(m => m.useCartStore().loadForUser(a.role + '_' + a.id))
    }
  }

  return { accounts, activeIndex, activeAccount, token, userInfo, isLoggedIn, isAdmin, isInheritor, uid, login, switchAccount, logout, initAuth }
})
