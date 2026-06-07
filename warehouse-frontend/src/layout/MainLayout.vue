<template>
  <el-container class="main-layout">
    <el-aside width="220px">
      <SideMenu />
    </el-aside>
    <el-container>
      <el-header height="auto" style="padding: 0">
        <TabBar />
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import SideMenu from './SideMenu.vue'
import TabBar from './TabBar.vue'
</script>

<style scoped>
.main-layout {
  height: 100vh;
}
.el-aside {
  background-color: #304156;
  overflow: hidden;
}
.el-main {
  background-color: #f0f2f5;
  padding: 16px;
}
</style>
