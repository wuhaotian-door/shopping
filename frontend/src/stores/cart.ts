import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ProductVO } from '@/types/api'

export interface CartItem {
  product: ProductVO
  quantity: number
}

const STORAGE_PREFIX = 'code1st_cart_'

function getStorageKey(uid: string) {
  return STORAGE_PREFIX + uid
}

function loadFromStorage(uid: string): CartItem[] {
  try {
    const raw = localStorage.getItem(getStorageKey(uid))
    return raw ? JSON.parse(raw) : []
  } catch { return [] }
}

function saveToStorage(uid: string, items: CartItem[]) {
  localStorage.setItem(getStorageKey(uid), JSON.stringify(items))
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])

  const count = computed(() => items.value.reduce((s, i) => s + i.quantity, 0))
  const total = computed(() => items.value.reduce((s, i) => s + i.product.price * i.quantity, 0))

  function loadForUser(uid: string) {
    items.value = loadFromStorage(uid)
  }

  function clear() {
    items.value = []
  }

  function add(product: ProductVO, uid: string) {
    const exist = items.value.find(i => i.product.id === product.id)
    if (exist) {
      if (exist.quantity < product.stock) exist.quantity++
    } else {
      items.value.push({ product, quantity: 1 })
    }
    saveToStorage(uid, items.value)
  }

  function remove(index: number, uid: string) {
    items.value.splice(index, 1)
    saveToStorage(uid, items.value)
  }

  function updateQty(index: number, qty: number, uid: string) {
    if (qty < 1) { remove(index, uid); return }
    if (qty > items.value[index].product.stock) qty = items.value[index].product.stock
    items.value[index].quantity = qty
    saveToStorage(uid, items.value)
  }

  return { items, count, total, loadForUser, clear, add, remove, updateQty }
})
