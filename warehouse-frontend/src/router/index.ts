import { createRouter, createWebHistory } from 'vue-router'
import { useTabsStore } from '@/stores/tabs'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { title: '首页', icon: 'HomeFilled' },
        },
        {
          path: 'warehouse/list',
          name: 'WarehouseList',
          component: () => import('@/views/warehouse/List.vue'),
          meta: { title: '仓库列表', icon: 'List' },
        },
        {
          path: 'warehouse/area',
          name: 'WarehouseArea',
          component: () => import('@/views/warehouse/Area.vue'),
          meta: { title: '库区管理', icon: 'Grid' },
        },
        {
          path: 'inventory/inbound',
          name: 'Inbound',
          component: () => import('@/views/inventory/Inbound.vue'),
          meta: { title: '入库管理', icon: 'Download' },
        },
        {
          path: 'inventory/stock',
          name: 'InventoryStock',
          component: () => import('@/views/inventory/Stock.vue'),
          meta: { title: '库存总览', icon: 'DataAnalysis' },
        },
        {
          path: 'inventory/inbound/create',
          name: 'InboundCreate',
          component: () => import('@/views/inventory/inbound/InboundForm.vue'),
          meta: { title: '新增入库单', icon: 'Download', hidden: true },
        },
        {
          path: 'inventory/inbound/edit/:id',
          name: 'InboundEdit',
          component: () => import('@/views/inventory/inbound/InboundForm.vue'),
          meta: { title: '编辑入库单', icon: 'Download', hidden: true },
        },
        {
          path: 'inventory/inbound/detail/:id',
          name: 'InboundDetail',
          component: () => import('@/views/inventory/inbound/InboundDetail.vue'),
          meta: { title: '入库单详情', icon: 'Download', hidden: true },
        },
        {
          path: 'inventory/outbound',
          name: 'Outbound',
          component: () => import('@/views/inventory/Outbound.vue'),
          meta: { title: '出库管理', icon: 'Upload' },
        },
        {
          path: 'inventory/repack',
          name: 'Repack',
          component: () => import('@/views/inventory/repack/RepackList.vue'),
          meta: { title: '转包管理', icon: 'Connection' },
        },
        {
          path: 'inventory/outbound/create',
          name: 'OutboundCreate',
          component: () => import('@/views/inventory/outbound/OutboundForm.vue'),
          meta: { title: '新增出库单', icon: 'Upload', hidden: true },
        },
        {
          path: 'inventory/outbound/edit/:id',
          name: 'OutboundEdit',
          component: () => import('@/views/inventory/outbound/OutboundForm.vue'),
          meta: { title: '编辑出库单', icon: 'Upload', hidden: true },
        },
        {
          path: 'inventory/outbound/detail/:id',
          name: 'OutboundDetail',
          component: () => import('@/views/inventory/outbound/OutboundDetail.vue'),
          meta: { title: '出库单详情', icon: 'Upload', hidden: true },
        },
        {
          path: 'inventory/repack/create',
          name: 'RepackCreate',
          component: () => import('@/views/inventory/repack/RepackForm.vue'),
          meta: { title: '新增转包单', icon: 'Connection', hidden: true },
        },
        {
          path: 'inventory/repack/edit/:id',
          name: 'RepackEdit',
          component: () => import('@/views/inventory/repack/RepackForm.vue'),
          meta: { title: '编辑转包单', icon: 'Connection', hidden: true },
        },
        {
          path: 'inventory/repack/detail/:id',
          name: 'RepackDetail',
          component: () => import('@/views/inventory/repack/RepackDetail.vue'),
          meta: { title: '转包单详情', icon: 'Connection', hidden: true },
        },
        {
          path: 'system/user',
          name: 'SystemUser',
          component: () => import('@/views/system/User.vue'),
          meta: { title: '用户管理', icon: 'User' },
        },
        {
          path: 'system/part',
          name: 'SystemPart',
          component: () => import('@/views/system/PartList.vue'),
          meta: { title: '零件管理', icon: 'Coin' },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') {
    next('/login')
  } else if (token && to.path === '/login') {
    next('/')
  } else {
    if (token && to.matched.length > 1) {
      const tabsStore = useTabsStore()
      tabsStore.addTab(to)
    }
    next()
  }
})

export default router
