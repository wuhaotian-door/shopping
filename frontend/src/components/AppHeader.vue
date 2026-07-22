<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { unreadCount } from '@/composables/useUnread'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isLoggedIn = computed(() => authStore.isLoggedIn)
const userName = computed(() => authStore.userInfo?.name || '')
const isInheritor = computed(() => authStore.userInfo?.role === 'INHERITOR')
const isAdmin = computed(() => authStore.userInfo?.role === 'ADMIN')
const isChatPage = computed(() => route.path === '/chat')
const isProfilePage = computed(() => route.path === '/profile')
const badgeCount = computed(() => unreadCount.value)

// 多账户切换
const showAccountMenu = ref(false)
const switcherRef = ref<HTMLElement | null>(null)
const accounts = computed(() => authStore.accounts)
const activeIndex = computed(() => authStore.activeIndex)
const avatarUrl = computed(() => authStore.activeAccount?.avatarUrl || '')
function roleLabel(r: string) {
  return r === 'INHERITOR' ? '传承人' : r === 'ADMIN' ? '管理员' : '用户'
}
function toggleAccountMenu() {
  showAccountMenu.value = !showAccountMenu.value
}
function doSwitchAccount(index: number) {
  authStore.switchAccount(index)
  showAccountMenu.value = false
}
function addAccount() {
  showAccountMenu.value = false
  router.push('/login?add=1')
}
function onDocumentClick(e: MouseEvent) {
  if (switcherRef.value && !switcherRef.value.contains(e.target as Node)) {
    showAccountMenu.value = false
  }
}
onMounted(() => document.addEventListener('click', onDocumentClick))
onUnmounted(() => document.removeEventListener('click', onDocumentClick))

const navItems = [
  { path: '/', label: '首页' },
  { path: '/news', label: '非遗资讯' },
  { path: '/inheritors', label: '传承人' },
  { path: '/store', label: '文创周边' },
  { path: '/stats', label: '数据统计' },
  { path: '/rag', label: '智能问答' },
]

function isActive(path: string) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}
</script>

<template>
  <header class="page-header">
    <div class="logo" @click="router.push('/')">
      <img src="/logo.png" alt="码上非遗" class="logo-img" />
      <span class="logo-text">码上非遗</span>
    </div>

    <nav class="nav-links">
      <a
        v-for="item in navItems"
        :key="item.label"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
        @click="router.push(item.path)"
      >
        {{ item.label }}
      </a>

      <template v-if="isInheritor">
        <span class="nav-separator"></span>
        <a class="nav-item nav-inheritor" @click="router.push('/inheritor')">我的工坊</a>
      </template>
      <template v-if="isAdmin">
        <span class="nav-separator"></span>
        <a class="nav-item nav-admin" @click="router.push('/admin')">管理后台</a>
      </template>
    </nav>

    <div class="header-right">
      <template v-if="isLoggedIn">
        <span class="user-role-tag" :class="isInheritor ? 'inheritor-tag' : isAdmin ? 'admin-tag' : 'user-tag'">
          {{ isInheritor ? '非遗传承人' : isAdmin ? '管理员' : '用户' }}
        </span>
        <!-- 多账户切换 -->
        <div class="account-switcher" ref="switcherRef" v-if="accounts.length > 0">
          <button class="account-btn" @click.stop="toggleAccountMenu" :class="{ open: showAccountMenu }">
            <img v-if="avatarUrl" :src="avatarUrl" class="header-avatar" />
            <span v-else class="header-avatar-placeholder">{{ userName.charAt(0) }}</span>
            <span class="user-name">{{ userName }}</span>
            <span class="account-arrow" :class="{ flipped: showAccountMenu }">&#9662;</span>
          </button>
          <Transition name="menu-fade">
          <div class="account-menu" v-if="showAccountMenu">
            <div
              v-for="(acc, idx) in accounts"
              :key="idx"
              class="account-item"
              :class="{ active: idx === activeIndex }"
              @click.stop="doSwitchAccount(idx)"
            >
              <span class="account-avatar">{{ acc.name.charAt(0) }}</span>
              <span class="account-info">
                <span class="account-name">{{ acc.name }}</span>
                <span class="account-detail">{{ acc.username }} · {{ roleLabel(acc.role) }}</span>
              </span>
              <span v-if="idx === activeIndex" class="account-check-icon">&#10003;</span>
            </div>
            <div class="account-divider"></div>
            <div class="account-item add-account" @click.stop="addAccount">
              <span class="account-add-icon">+</span>
              <span class="account-name">添加其他账号</span>
            </div>
          </div>
          </Transition>
        </div>
        <button v-if="!isInheritor && !isAdmin" class="header-btn" :class="{ 'header-btn-active': isProfilePage }" @click="router.push('/profile')">个人中心</button>
        <button class="header-btn msg-btn" :class="{ 'header-btn-active': isChatPage }" @click="router.push('/chat')">
          消息
          <span v-if="badgeCount > 0" class="msg-badge">{{ badgeCount > 99 ? '99+' : badgeCount }}</span>
        </button>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </template>
      <template v-else>
        <button class="login-top-btn" @click="router.push('/login')">登录</button>
      </template>
    </div>
  </header>
</template>

<style scoped>
.page-header {
  background: linear-gradient(135deg, #8b1a2b 0%, #a01e35 40%, #c41e3a 100%);
  color: #fff;
  padding: 0 6%;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
  font-family: 'Noto Serif SC', 'STKaiti', 'KaiTi', '楷体', 'STSong', serif;
  letter-spacing: 1px;
  box-shadow: 0 2px 16px rgba(139, 26, 43, 0.35);
}

.logo { display: flex; align-items: center; gap: 10px; cursor: pointer; white-space: nowrap; }
.logo-img { height: 42px; width: auto; }
.logo-text { font-size: 22px; font-weight: 700; letter-spacing: 4px; color: #fff; }

.nav-links { display: flex; align-items: center; gap: 4px; }
.nav-item {
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none; font-size: 15px;
  cursor: pointer; padding: 8px 18px; border-radius: 6px;
  transition: all 0.25s; white-space: nowrap;
  letter-spacing: 2px; font-weight: 400;
}
.nav-item:hover { color: #fff; background: rgba(255, 255, 255, 0.12); }
.nav-item.active { color: #fff; background: rgba(255, 255, 255, 0.2); font-weight: 700; }
.nav-separator { width: 1px; height: 24px; background: rgba(255, 255, 255, 0.25); margin: 0 6px; }
.nav-inheritor { color: #ffd700 !important; background: rgba(255, 215, 0, 0.12) !important; }
.nav-inheritor:hover { background: rgba(255, 215, 0, 0.25) !important; }
.nav-inheritor.active { background: rgba(255, 215, 0, 0.28) !important; color: #fff !important; }
.nav-admin { color: #fff !important; background: rgba(255, 255, 255, 0.1) !important; }
.nav-admin:hover { background: rgba(255, 255, 255, 0.2) !important; }

.header-right { display: flex; align-items: center; gap: 10px; }
.user-role-tag { font-size: 11px; padding: 2px 10px; border-radius: 10px; font-weight: 700; white-space: nowrap; letter-spacing: 1px; }
.inheritor-tag { background: #ffd700; color: #8b4513; }
.admin-tag { background: #fff; color: #c41e3a; }
.user-tag { background: rgba(255, 255, 255, 0.2); color: #fff; }
.user-name { font-size: 14px; white-space: nowrap; }

.header-avatar { width: 28px !important; height: 28px !important; border-radius: 50%; object-fit: cover; flex-shrink: 0; border: 2px solid rgba(255,255,255,0.4); }
.header-avatar-placeholder { width: 28px; height: 28px; border-radius: 50%; background: rgba(255,255,255,0.2); color: #fff; display: inline-flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; flex-shrink: 0; }

/* 多账户切换器 */
.account-switcher { position: relative; z-index: 150; }
.account-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 12px; font-size: 14px;
  color: #fff; background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.3); border-radius: 6px;
  cursor: pointer; font-family: inherit; white-space: nowrap;
  transition: all 0.2s;
}
.account-btn:hover, .account-btn.open {
  background: rgba(255,255,255,0.22);
  border-color: rgba(255,255,255,0.5);
}
.account-arrow {
  font-size: 10px; transition: transform 0.25s;
}
.account-arrow.flipped { transform: rotate(180deg); }
.account-menu {
  position: absolute; top: calc(100% + 8px); right: 0;
  min-width: 240px;
  background: linear-gradient(160deg, #3d0a12 0%, #5c1522 40%, #7a1e2d 100%);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 10px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.35), 0 0 0 1px rgba(255,215,0,0.08);
  overflow: hidden;
  padding: 6px 0;
}
.account-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; cursor: pointer; transition: background 0.15s;
  color: rgba(255,255,255,0.9); font-size: 14px;
  position: relative;
}
.account-item:hover { background: rgba(255,255,255,0.1); }
.account-item.active {
  background: rgba(255,215,0,0.12);
  color: #ffd700;
}
.account-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: rgba(255,255,255,0.15); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700; flex-shrink: 0;
}
.account-item.active .account-avatar {
  background: #ffd700; color: #5c1522;
}
.account-info { display: flex; flex-direction: column; }
.account-name { font-weight: 600; }
.account-detail { font-size: 11px; color: rgba(255,255,255,0.5); }
.account-item.active .account-detail { color: rgba(255,215,0,0.7); }
.account-check-icon {
  margin-left: auto; color: #ffd700; font-size: 16px; font-weight: 700;
}
.account-divider {
  height: 1px; background: rgba(255,255,255,0.1); margin: 4px 0;
}
.add-account { color: rgba(255,255,255,0.7); }
.add-account:hover { color: #fff; background: rgba(255,255,255,0.1); }
.account-add-icon {
  width: 32px; height: 32px; border-radius: 50%;
  border: 1.5px dashed rgba(255,255,255,0.4); color: rgba(255,255,255,0.6);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; flex-shrink: 0;
}
.add-account:hover .account-add-icon {
  border-color: #ffd700; color: #ffd700;
}

/* 下拉动画 */
.menu-fade-enter-active { transition: all 0.2s ease-out; }
.menu-fade-leave-active { transition: all 0.15s ease-in; }
.menu-fade-enter-from { opacity: 0; transform: translateY(-6px); }
.menu-fade-leave-to { opacity: 0; transform: translateY(-4px); }

.header-btn {
  padding: 6px 14px; font-size: 12px;
  color: #fff; background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.3); border-radius: 4px;
  cursor: pointer; transition: 0.2s; white-space: nowrap; font-family: inherit;
}
.header-btn:hover { background: rgba(255, 255, 255, 0.25); }
.header-btn-active {
  background: #ffd700 !important;
  color: #c41e3a !important;
  font-weight: 700;
  border-color: #ffd700 !important;
}

.logout-btn {
  padding: 6px 14px; font-size: 12px;
  color: #fff; background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.3); border-radius: 4px;
  cursor: pointer; transition: 0.2s; white-space: nowrap; font-family: inherit;
}
.logout-btn:hover { background: rgba(255, 255, 255, 0.25); }

.login-top-btn {
  padding: 8px 24px; font-size: 14px;
  color: #c41e3a; background: #fff; border: none; border-radius: 4px;
  cursor: pointer; letter-spacing: 2px; font-weight: 600; font-family: inherit;
  transition: 0.2s;
}
.login-top-btn:hover { background: #ffe9ec; }

/* 消息按钮红点徽标 */
.msg-btn { position: relative; }
.msg-badge {
  position: absolute; top: -6px; right: -8px;
  min-width: 18px; height: 18px; border-radius: 9px;
  background: #ffd700; color: #c41e3a;
  font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  padding: 0 5px; line-height: 1;
  animation: badgePulse 2s ease-in-out infinite;
}
@keyframes badgePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

@media (max-width: 768px) {
  .nav-links { gap: 2px; }
  .nav-item { padding: 6px 10px; font-size: 13px; }
  .page-header { height: auto; padding: 10px 16px; flex-wrap: wrap; gap: 8px; }
  .logo-text { font-size: 18px; }
  .logo-img { height: 32px; }
}
</style>
