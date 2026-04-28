<template>
  <div class="login-container">
    <div class="login-box">
      <div class="title-box">
        <h2>门户统一登录认证</h2><p>请输入学校统一身份认证账号</p>
      </div>
      <el-form :model="loginForm" size="large">
        <el-form-item><el-input v-model="loginForm.account" placeholder="学号/工号" prefix-icon="User" /></el-form-item>
        <el-form-item><el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin"/></el-form-item>
        <el-button type="primary" class="login-btn" @click="handleLogin">门 户 登 录</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loginForm = ref({ account: '', password: '' })

const handleLogin = () => {
  if (!loginForm.value.account) return ElMessage.warning('请输入账号')
  // 模拟登录成功
  localStorage.setItem('isLoggedIn', 'true')
  // 模拟给特定账号赋管理员权限 (实际由后端返回)
  if (loginForm.value.account === 'admin') localStorage.setItem('isAdmin', 'true')
  
  ElMessage.success('登录成功！')
  router.push('/role-select') // 跳转角色选择页
}
</script>
<style scoped>
.login-container { height: 100vh; display: flex; justify-content: center; align-items: center; background: linear-gradient(45deg, #ffc5af 0%, #f5aeb4 100%); }
.login-box { width: 380px; background: #ffe8e8; padding: 40px; border-radius: 10px; box-shadow: 0 10px 25px rgba(230, 57, 70, 0.1); }
.title-box { text-align: center; margin-bottom: 30px; }
.title-box h2 { color: #e63946; margin: 0 0 10px; }
.title-box p { color: #999; font-size: 14px; margin: 0; }
.login-btn { width: 100%; border-radius: 20px; font-weight: bold; margin-top: 10px;}
</style>