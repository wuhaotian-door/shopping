import { reactive } from 'vue'

export interface ToastItem {
  id: number
  message: string
  type: 'success' | 'error' | 'warning'
}

let nextId = 0
const state = reactive<{ items: ToastItem[] }>({ items: [] })

export function useToast() {
  const show = (message: string, type: 'success' | 'error' | 'warning' = 'success') => {
    const id = ++nextId
    state.items.push({ id, message, type })
    setTimeout(() => {
      const idx = state.items.findIndex(t => t.id === id)
      if (idx >= 0) state.items.splice(idx, 1)
    }, 3000)
  }
  return { toasts: state.items, showToast: show }
}
