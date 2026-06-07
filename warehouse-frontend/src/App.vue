<template>
  <router-view />
</template>

<script setup lang="ts">
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html,
body,
#app {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', Arial, sans-serif;
}
.page-container {
  padding: 0;
}
.page-container h2 {
  font-size: 20px;
  margin-bottom: 12px;
}
.page-container p {
  color: #909399;
}
</style>
