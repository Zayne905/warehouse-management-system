<template>
  <div class="side-menu">
    <div class="logo">📦 仓储管理系统</div>
    <el-menu
      :default-active="activeMenu"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409EFF"
      router
    >
      <el-menu-item index="/dashboard">
        <el-icon><HomeFilled /></el-icon>
        <span>首页</span>
      </el-menu-item>

      <el-sub-menu index="warehouse">
        <template #title>
          <el-icon><Box /></el-icon>
          <span>仓库管理</span>
        </template>
        <el-menu-item index="/warehouse/list">
          <el-icon><List /></el-icon>
          <span>仓库列表</span>
        </el-menu-item>
        <el-menu-item index="/warehouse/area">
          <el-icon><Grid /></el-icon>
          <span>库区管理</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="inventory">
        <template #title>
          <el-icon><Folder /></el-icon>
          <span>库存管理</span>
        </template>
        <el-menu-item index="/inventory/inbound">
          <el-icon><Download /></el-icon>
          <span>入库管理</span>
        </el-menu-item>
        <el-menu-item index="/inventory/stock">
          <el-icon><DataAnalysis /></el-icon>
          <span>库存总览</span>
        </el-menu-item>
        <el-menu-item index="/inventory/outbound">
          <el-icon><Upload /></el-icon>
          <span>出库管理</span>
        </el-menu-item>
        <el-menu-item index="/inventory/repack">
          <el-icon><Connection /></el-icon>
          <span>转包管理</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="system">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </template>
        <el-menu-item index="/system/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/system/part">
          <el-icon><Coin /></el-icon>
          <span>零件管理</span>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  HomeFilled,
  Box,
  List,
  Grid,
  Folder,
  Download,
  Upload,
  DataAnalysis,
  Connection,
  Setting,
  User,
  Coin,
} from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.side-menu {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  white-space: nowrap;
  overflow: hidden;
}
.el-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}
</style>
