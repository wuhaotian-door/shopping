import { ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  recordBrowse as recordBrowseApi,
  listBrowseHistory,
  deleteBrowseHistory,
  clearBrowseHistory,
  type BrowseHistoryVO,
  type RecordBrowseDTO,
} from '@/api/history'

export interface HistoryEntry {
  id: number
  path: string
  title: string
  cover?: string
  time: string
  targetType: string
  targetId: number
}

/** 当前活跃 UID 对应的浏览记录缓存 */
const historyList = ref<HistoryEntry[]>([])
const loading = ref(false)
/** 当前 historyList 归属的 UID，切换账号时立即清空并重载 */
let ownerUid = ''

function mapEntry(h: BrowseHistoryVO): HistoryEntry {
  return {
    id: h.id,
    path: h.targetPath || `/${h.targetType}/${h.targetId}`,
    title: h.targetTitle || '(无标题)',
    cover: h.targetCover || '',
    time: h.createTime?.slice(0, 16) || '',
    targetType: h.targetType,
    targetId: h.targetId,
  }
}

export function useHistory() {
  const authStore = useAuthStore()

  /** 记录一次浏览 — 仅当当前 UID 与缓存所有者一致时才写入本地缓存 */
  async function record(data: RecordBrowseDTO) {
    const uid = authStore.uid
    if (!uid) return
    try {
      await recordBrowseApi(data)
      // 只有当前缓存的 UID 匹配时才更新本地列表，避免跨账号污染
      if (uid === ownerUid) {
        const entry: HistoryEntry = {
          id: 0,
          path: data.targetPath || `/${data.targetType}/${data.targetId}`,
          title: data.targetTitle,
          cover: data.targetCover,
          time: new Date().toLocaleString('zh-CN').slice(0, 16),
          targetType: data.targetType,
          targetId: data.targetId,
        }
        historyList.value = historyList.value.filter(
          e => !(e.targetType === data.targetType && e.targetId === data.targetId)
        )
        historyList.value.unshift(entry)
        if (historyList.value.length > 50) historyList.value = historyList.value.slice(0, 50)
      }
    } catch { /* 静默失败，不影响页面正常浏览 */ }
  }

  /** 加载当前用户的浏览记录（后端按 user_id + user_role 隔离） */
  async function loadHistory() {
    const uid = authStore.uid
    if (!uid) { historyList.value = []; ownerUid = ''; return }
    if (uid !== ownerUid) { historyList.value = []; ownerUid = uid }
    loading.value = true
    try {
      const res = await listBrowseHistory()
      // 二次校验：请求返回时 UID 可能已变化
      if (authStore.uid === ownerUid) {
        historyList.value = (res.data || []).map(mapEntry)
      }
    } catch { /* ignore */ } finally {
      loading.value = false
    }
  }

  /** 删除单条记录 */
  async function deleteHistory(id: number) {
    try {
      await deleteBrowseHistory(id)
      historyList.value = historyList.value.filter(e => e.id !== id)
    } catch { /* ignore */ }
  }

  /** 清空当前用户全部浏览记录 */
  async function clearHistory() {
    try {
      await clearBrowseHistory()
      historyList.value = []
    } catch { /* ignore */ }
  }

  /** 获取当前用户历史（响应式） */
  function getUserHistory(): HistoryEntry[] {
    return historyList.value
  }

  // ★ 监听 UID 变化：切换账号时立即清空缓存并重载
  watch(() => authStore.uid, (newUid, oldUid) => {
    if (newUid !== oldUid) {
      historyList.value = []
      ownerUid = newUid || ''
      if (newUid) loadHistory()
    }
  })

  return { historyList, loading, record, loadHistory, deleteHistory, clearHistory, getUserHistory }
}
