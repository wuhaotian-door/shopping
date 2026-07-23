import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { listConversations } from '@/api/chat'

export const unreadCount = ref(0)

export function useUnread() {
  const authStore = useAuthStore()
  let timer: ReturnType<typeof setInterval> | null = null
  let started = false

  async function refresh() {
    if (!authStore.uid) { unreadCount.value = 0; return }
    try {
      const res = await listConversations()
      unreadCount.value = res.data.reduce((s, c) => s + (c.unreadCount || 0), 0)
    } catch { /* API 调用失败保持原值 */ }
  }

  function startPolling() {
    if (started) return
    started = true
    refresh()
    timer = setInterval(refresh, 6000)
  }

  // 等待 uid 就绪后开始轮询
  watch(() => authStore.uid, (uid) => {
    if (uid) startPolling()
  }, { immediate: true })

  onUnmounted(() => {
    if (timer) { clearInterval(timer); timer = null }
    started = false
  })
}
