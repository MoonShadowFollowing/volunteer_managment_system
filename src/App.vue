<template>
  <router-view />
</template>

<style>
/* 全局浅红/灰白 主题定制 */
:root {
  --el-color-primary: #e63946; /* 主色调：柔和的红色 */
  --el-color-primary-light-3: #ef7a83;
  --el-color-primary-light-5: #f4a5ab;
  --el-color-primary-light-7: #f8cdd0;
  --el-color-primary-light-9: #fdebee;
  --el-bg-color-page: #fff7f7; /* 页面整体背景：极浅的红色 */
}

html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', Arial, sans-serif;
  background-color: var(--el-bg-color-page);
}

/* 让所有卡片取消边框，显得更干净 */
.el-card {
  border: none !important;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05) !important;
}
</style>