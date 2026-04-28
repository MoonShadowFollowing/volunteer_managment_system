const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false  // 👉 加上这一行，直接关闭恶心的 ESLint 语法检查
})