import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export interface HistoryEntry {
  path: string
  title: string
  time: string
}

const STORAGE_KEY = 'code1st_history'

function loadAll(): Record<string, HistoryEntry[]> {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') } catch { return {} }
}
function saveAll(data: Record<string, HistoryEntry[]>) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

export function useHistory() {
  const authStore = useAuthStore()
  const route = useRoute()

  function record() {
    const uid = authStore.uid
    if (!uid || !route.path || route.path === '/login') return
    const all = loadAll()
    if (!all[uid]) all[uid] = []
    // 去重
    all[uid] = all[uid].filter(e => e.path !== route.path)
    all[uid].unshift({
      path: route.path,
      title: (route.meta?.title as string) || document.title || route.path,
      time: new Date().toLocaleString('zh-CN'),
    })
    if (all[uid].length > 30) all[uid] = all[uid].slice(0, 30)
    saveAll(all)
  }

  /** 获取当前用户的历史 */
  function getUserHistory(): HistoryEntry[] {
    const uid = authStore.uid
    if (!uid) return []
    const all = loadAll()
    return all[uid] || []
  }

  // 自动记录路由变化
  watch(() => route.fullPath, () => { record() }, { immediate: false })

  return { record, getUserHistory }
}
