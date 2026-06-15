<template>
  <div class="tab-bar">
    <el-tabs
      v-model="tabsStore.activeTab"
      type="card"
      class="tab-tabs"
      @tab-click="handleTabClick"
      @tab-remove="handleTabRemove"
    >
      <el-tab-pane
        v-for="tab in tabsStore.tabs"
        :key="tab.path"
        :name="tab.path"
        :closable="tab.closable"
      >
        <template #label>
          <span class="tab-label">
            <el-icon v-if="tab.icon" class="tab-icon">
              <component :is="tab.icon" />
            </el-icon>
            {{ tab.title }}
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div class="tab-actions">
      <el-dropdown trigger="click" @command="handleDropdownCommand">
        <span class="dropdown-trigger">
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="closeCurrent">关闭当前</el-dropdown-item>
            <el-dropdown-item command="closeOthers">关闭其它</el-dropdown-item>
            <el-dropdown-item command="closeAll">关闭所有</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useTabsStore } from '@/stores/tabs'
import { ArrowDown } from '@element-plus/icons-vue'
import type { TabsPaneContext } from 'element-plus'

const router = useRouter()
const tabsStore = useTabsStore()

function handleTabClick(pane: TabsPaneContext) {
  const path = pane.paneName as string
  router.push(path)
}

function handleTabRemove(path: string) {
  tabsStore.removeTab(path)
  if (tabsStore.activeTab !== path) {
    router.push(tabsStore.activeTab)
  }
}

function handleDropdownCommand(command: string) {
  const currentPath = tabsStore.activeTab

  switch (command) {
    case 'closeCurrent':
      if (currentPath !== '/dashboard') {
        tabsStore.removeTab(currentPath)
        router.push(tabsStore.activeTab)
      }
      break
    case 'closeOthers':
      tabsStore.tabs = tabsStore.tabs.filter(
        (t) => !t.closable || t.path === currentPath
      )
      tabsStore.activeTab = currentPath
      break
    case 'closeAll':
      tabsStore.tabs = tabsStore.tabs.filter((t) => !t.closable)
      tabsStore.activeTab = '/dashboard'
      router.push('/dashboard')
      break
  }
}
</script>

<style scoped>
.tab-bar {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding-right: 8px;
}
.tab-tabs {
  flex: 1;
}
.tab-tabs :deep(.el-tabs__header) {
  margin: 0;
  border-bottom: none;
}
.tab-tabs :deep(.el-tabs__nav) {
  border: none !important;
}
.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.tab-icon {
  font-size: 14px;
}
.tab-actions {
  flex-shrink: 0;
  padding: 0 8px;
}
.dropdown-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  cursor: pointer;
  border-radius: 4px;
}
.dropdown-trigger:hover {
  background-color: #f0f2f5;
}
</style>
