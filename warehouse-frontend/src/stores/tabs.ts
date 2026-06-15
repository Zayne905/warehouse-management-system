import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

export interface Tab {
  path: string
  title: string
  icon?: string
  closable: boolean
}

export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref<Tab[]>([
    {
      path: '/dashboard',
      title: '首页',
      icon: 'HomeFilled',
      closable: false,
    },
  ])
  const activeTab = ref('/dashboard')

  function addTab(route: RouteLocationNormalized) {
    const existing = tabs.value.find((t) => t.path === route.path)
    if (existing) {
      activeTab.value = existing.path
      return
    }
    tabs.value.push({
      path: route.path,
      title: (route.meta?.title as string) || (route.name as string),
      icon: route.meta?.icon as string,
      closable: true,
    })
    activeTab.value = route.path
  }

  function removeTab(path: string) {
    const idx = tabs.value.findIndex((t) => t.path === path)
    if (idx === -1) return
    tabs.value.splice(idx, 1)
    if (activeTab.value === path) {
      const next = tabs.value[Math.min(idx, tabs.value.length - 1)]
      activeTab.value = next?.path || '/dashboard'
    }
  }

  return { tabs, activeTab, addTab, removeTab }
})
