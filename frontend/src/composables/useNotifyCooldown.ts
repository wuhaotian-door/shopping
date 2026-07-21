/**
 * 消息邮件通知冷却系统
 * 当 A 给 B 发消息时，如果距上次通知 B 超过 60 秒，则调用后端发送邮件
 * 冷却结束后再有新消息才触发下一次邮件
 */

const STORAGE_KEY = 'code1st_notify_cooldown'
const COOLDOWN_MS = 60_000

interface CooldownMap { [convId: string]: number }  // convId → 上次通知时间戳

function load(): CooldownMap {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') } catch { return {} }
}
function save(m: CooldownMap) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(m))
}

/** 检查是否可以发送通知，如果可以则记录时间戳。返回 true 表示应该发送邮件 */
export function tryNotify(convId: number): boolean {
  const map = load()
  const key = String(convId)
  const last = map[key] || 0
  const now = Date.now()
  if (now - last >= COOLDOWN_MS) {
    map[key] = now
    save(map)
    return true
  }
  return false
}

/** 获取剩余冷却秒数（用于 UI 提示） */
export function cooldownRemaining(convId: number): number {
  const map = load()
  const last = map[String(convId)] || 0
  const remaining = COOLDOWN_MS - (Date.now() - last)
  return Math.max(0, Math.ceil(remaining / 1000))
}
