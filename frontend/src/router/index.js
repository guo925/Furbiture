import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/home'
    },
    // 用户端路由
    {
      path: '/home',
      name: 'Home',
      component: () => import('../views/user/HomeView.vue'),
      meta: { title: '首页' }
    },
    {
      path: '/products',
      name: 'Products',
      component: () => import('../views/user/ProductsView.vue'),
      meta: { title: '商品列表' }
    },
    {
      path: '/product/:id',
      name: 'ProductDetail',
      component: () => import('../views/user/ProductDetailView.vue'),
      meta: { title: '商品详情' }
    },
    {
      path: '/cart',
      name: 'Cart',
      component: () => import('../views/user/CartView.vue'),
      meta: { title: '购物车', requiresAuth: true }
    },
    {
      path: '/checkout',
      name: 'Checkout',
      component: () => import('../views/user/CheckoutView.vue'),
      meta: { title: '结算', requiresAuth: true }
    },
    {
      path: '/orders',
      name: 'Orders',
      component: () => import('../views/user/OrdersView.vue'),
      meta: { title: '我的订单', requiresAuth: true }
    },
    {
      path: '/order/:id',
      name: 'OrderDetail',
      component: () => import('../views/user/OrderDetailView.vue'),
      meta: { title: '订单详情', requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('../views/user/ProfileView.vue'),
      meta: { title: '个人中心', requiresAuth: true }
    },
    {
      path: '/address',
      name: 'Address',
      component: () => import('../views/user/AddressView.vue'),
      meta: { title: '地址管理', requiresAuth: true }
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/user/LoginView.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/user/RegisterView.vue'),
      meta: { title: '注册' }
    },
    // 管理端路由
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: () => import('../views/admin/DashboardView.vue'),
          meta: { title: '控制台' }
        },
        {
          path: 'products',
          name: 'AdminProducts',
          component: () => import('../views/admin/ProductsView.vue'),
          meta: { title: '商品管理' }
        },
        {
          path: 'categories',
          name: 'AdminCategories',
          component: () => import('../views/admin/CategoriesView.vue'),
          meta: { title: '分类管理' }
        },
        {
          path: 'orders',
          name: 'AdminOrders',
          component: () => import('../views/admin/OrdersView.vue'),
          meta: { title: '订单管理' }
        },
        {
          path: 'users',
          name: 'AdminUsers',
          component: () => import('../views/admin/UsersView.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'file-upload-test',
          name: 'AdminFileUploadTest',
          component: () => import('../views/admin/FileUploadTest.vue'),
          meta: { title: '文件上传测试' }
        }
      ]
    },
    // 商家管理端路由
    {
      path: '/merchant',
      name: 'Merchant',
      component: () => import('../views/merchant/MerchantDashboard.vue'),
      meta: { title: '商家中心', requiresAuth: true, requiresMerchant: true }
    },
    {
      path: '/merchant/products',
      name: 'MerchantProducts',
      component: () => import('../views/merchant/MerchantProducts.vue'),
      meta: { title: '商品管理', requiresAuth: true, requiresMerchant: true }
    },
    {
      path: '/merchant/orders',
      name: 'MerchantOrders',
      component: () => import('../views/merchant/MerchantOrders.vue'),
      meta: { title: '订单管理', requiresAuth: true, requiresMerchant: true }
    },
    {
      path: '/merchant/categories',
      name: 'MerchantCategories',
      component: () => import('../views/merchant/MerchantCategories.vue'),
      meta: { title: '商品分类', requiresAuth: true, requiresMerchant: true }
    },
    {
      path: '/merchant/profile',
      name: 'MerchantProfile',
      component: () => import('../views/merchant/MerchantProfile.vue'),
      meta: { title: '个人中心', requiresAuth: true, requiresMerchant: true }
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 家具商城` : '家具商城'

  try {
    const userStore = useUserStore()
    const token = userStore.token
    const user = userStore.user

    if (to.matched.some(record => record.meta.requiresAuth)) {
      if (!token) {
        next({ name: 'Login' })
      } else if (to.matched.some(record => record.meta.requiresAdmin)) {
        if (user && user.role === 'ADMIN') {
          next()
        } else {
          next({ name: 'Home' })
        }
      } else if (to.matched.some(record => record.meta.requiresMerchant)) {
        if (user && user.role === 'MERCHANT') {
          next()
        } else {
          next({ name: 'Home' })
        }
      } else {
        next()
      }
    } else {
      next()
    }
  } catch (error) {
    console.error('路由守卫错误:', error)
    next()
  }
})

export default router