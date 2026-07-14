import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/HomeView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/register-success',
      name: 'RegisterSuccess',
      component: () => import('@/views/RegisterSuccessView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('@/views/AdminPlaceholderView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
  ],
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  // 需要管理员权限的路由
  if (to.meta.requiresAdmin) {
    if (!authStore.isLoggedIn) {
      next('/login')
    } else if (authStore.userInfo?.role !== 'ADMIN') {
      // 非管理员角色跳回首页
      next('/')
    } else {
      next()
    }
    return
  }

  // 需要登录的路由
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next('/login')
    return
  }

  // 已登录时访问登录页 → 按角色跳转
  if (to.path === '/login' && authStore.isLoggedIn) {
    if (authStore.userInfo?.role === 'ADMIN') {
      next('/admin')
    } else {
      next('/')
    }
    return
  }

  next()
})

export default router
