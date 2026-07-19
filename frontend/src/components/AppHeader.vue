<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isLoggedIn = computed(() => authStore.isLoggedIn)
const userName = computed(() => authStore.userInfo?.name)
const isInheritor = computed(() => authStore.userInfo?.role === 'INHERITOR')
const isAdmin = computed(() => authStore.userInfo?.role === 'ADMIN')

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
    <div class="logo" @click="router.push('/')">码上非遗</div>

    <nav class="nav-links">
      <a
        v-for="item in navItems"
        :key="item.label"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
        @click="item.hash ? router.push(item.path + item.hash) : router.push(item.path)"
      >
        {{ item.label }}
      </a>

      <!-- 传承人专属入口 -->
      <template v-if="isInheritor">
        <span class="nav-separator"></span>
        <a class="nav-item nav-inheritor" @click="router.push('/inheritor')">我的工坊</a>
      </template>
      <!-- 管理员入口 -->
      <template v-if="isAdmin">
        <span class="nav-separator"></span>
        <a class="nav-item nav-admin" @click="router.push('/admin')">管理后台</a>
      </template>
    </nav>

    <!-- 右侧用户区 -->
    <div class="header-right">
      <template v-if="isLoggedIn">
        <span class="user-role-tag" :class="isInheritor ? 'inheritor-tag' : isAdmin ? 'admin-tag' : 'user-tag'">
          {{ isInheritor ? '非遗传承人' : isAdmin ? '管理员' : '用户' }}
        </span>
        <span class="user-name">{{ userName }}</span>
        <button class="header-chat-btn" @click="router.push('/chat')">💬 消息</button>
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
  background-color: #c41e3a;
  color: #fff;
  padding: 0 5%;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
}
.logo { font-size: 20px; font-weight: bold; cursor: pointer; white-space: nowrap; }
.nav-links { display: flex; align-items: center; gap: 6px; }
.nav-item {
  color: rgba(255,255,255,0.8); text-decoration: none; font-size: 14px;
  cursor: pointer; padding: 8px 16px; border-radius: 6px; transition: all 0.2s; white-space: nowrap;
}
.nav-item:hover { color: #fff; background: rgba(255,255,255,0.12); }
.nav-item.active { color: #fff; background: rgba(255,255,255,0.2); font-weight: bold; }
.nav-separator { width: 1px; height: 22px; background: rgba(255,255,255,0.2); margin: 0 4px; }
.nav-inheritor { background: rgba(255,215,0,0.15) !important; color: #ffd700 !important; }
.nav-inheritor:hover { background: rgba(255,215,0,0.25) !important; }
.nav-inheritor.active { background: #ffd700 !important; color: #8b4513 !important; }
.nav-admin { background: rgba(255,255,255,0.12) !important; }
.nav-admin:hover { background: rgba(255,255,255,0.25) !important; }

.header-right { display: flex; align-items: center; gap: 10px; }
.user-role-tag { font-size: 11px; padding: 2px 10px; border-radius: 10px; font-weight: bold; white-space: nowrap; }
.inheritor-tag { background: #ffd700; color: #8b4513; }
.admin-tag { background: #fff; color: #c41e3a; }
.user-tag { background: rgba(255,255,255,0.2); color: #fff; }
.user-name { font-size: 14px; white-space: nowrap; }
.header-chat-btn {
  padding: 6px 14px; font-size: 12px; color: #fff; background: rgba(255,255,255,0.2);
  border: 1px solid rgba(255,255,255,0.5); border-radius: 4px; cursor: pointer; transition: 0.2s;
}
.header-chat-btn:hover { background: rgba(255,255,255,0.35); }
.logout-btn {
  padding: 6px 14px; font-size: 12px; color: #fff; background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.4); border-radius: 4px; cursor: pointer; transition: 0.2s; white-space: nowrap;
}
.logout-btn:hover { background: rgba(255,255,255,0.3); }
.login-top-btn { padding: 8px 22px; font-size: 14px; color: #fff; background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.4); border-radius: 4px; cursor: pointer; }

@media (max-width: 768px) {
  .nav-links { gap: 2px; }
  .nav-item { padding: 6px 10px; font-size: 12px; }
  .page-header { height: auto; padding: 10px 16px; flex-wrap: wrap; gap: 8px; }
}
</style>
