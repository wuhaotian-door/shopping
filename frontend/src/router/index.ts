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
      path: '/forgot-password',
      name: 'ForgotPassword',
      component: () => import('@/views/ForgotPasswordView.vue'),
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
    {
      path: '/inheritor',
      name: 'Inheritor',
      component: () => import('@/views/InheritorDashboard.vue'),
      meta: { requiresAuth: true, requiresInheritor: true },
    },
    {
      path: '/store',
      name: 'Store',
      component: () => import('@/views/StoreView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/cart',
      name: 'Cart',
      component: () => import('@/views/CartView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/orders',
      name: 'Orders',
      component: () => import('@/views/OrdersView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/order/:id',
      name: 'ProductOrderDetail',
      component: () => import('@/views/ProductOrderDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/chat',
      name: 'Chat',
      component: () => import('@/views/ChatView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'UserProfile',
      component: () => import('@/views/UserProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/inheritors',
      name: 'InheritorList',
      component: () => import('@/views/InheritorListView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/product/:id',
      name: 'ProductDetail',
      component: () => import('@/views/ProductDetailView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/news',
      name: 'NewsList',
      component: () => import('@/views/NewsListView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/news/:id',
      name: 'ArticleDetail',
      component: () => import('@/views/ArticleDetailView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/appointment/:inheritorId',
      name: 'Appointment',
      component: () => import('@/views/AppointmentView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/stats',
      name: 'Stats',
      component: () => import('@/views/StatsView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/rag',
      name: 'Rag',
      component: () => import('@/views/RagView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/ai-search',
      name: 'AiSearch',
      component: () => import('@/views/AiSearchView.vue'),
      meta: { requiresAuth: false },
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

  // 需要传承人权限的路由
  if (to.meta.requiresInheritor) {
    if (!authStore.isLoggedIn) {
      next('/login')
    } else if (authStore.userInfo?.role !== 'INHERITOR') {
      // 非传承人角色跳回首页
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
  if (to.path === '/login' && authStore.isLoggedIn && to.query.add !== '1') {
    if (authStore.userInfo?.role === 'ADMIN') {
      next('/admin')
    } else if (authStore.userInfo?.role === 'INHERITOR') {
      next('/inheritor')
    } else {
      next('/')
    }
    return
  }

  next()
})

export default router
