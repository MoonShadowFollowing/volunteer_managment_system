<template>
  <div class="login-container">
    <div class="login-box">
      <div class="title-box">
        <h2>门户统一登录认证</h2><p>请输入学校统一身份认证账号</p>
      </div>
      <el-form :model="loginForm" size="large">
        <el-form-item><el-input v-model="loginForm.account" placeholder="学号/工号" prefix-icon="User" /></el-form-item>
        <el-form-item><el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin"/></el-form-item>
        <el-button type="primary" class="login-btn" :loading="submitting" @click="handleLogin">门 户 登 录</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login as loginApi } from '../../api/auth'

const router = useRouter()
const loginForm = ref({ account: '', password: '' })
const submitting = ref(false)

const handleLogin = async () => {
  if (!loginForm.value.account) return ElMessage.warning('请输入账号')
  if (!loginForm.value.password) return ElMessage.warning('请输入密码')

  submitting.value = true
  try {
    const data = await loginApi(loginForm.value.account.trim(), loginForm.value.password)
    // data = { token, user: { userId, username, name, role, isOrganizerQualified, isAdmin } }
    const { token, user } = data
    localStorage.setItem('token', token)
    localStorage.setItem('isLoggedIn', 'true')
    localStorage.setItem('userRole', user.role)
    localStorage.setItem('isOrganizerQualified', String(user.isOrganizerQualified))
    localStorage.setItem('isAdmin', String(user.isAdmin))
    localStorage.setItem('userName', user.name)
    localStorage.setItem('userId', String(user.userId))

    ElMessage.success(`欢迎，${user.name}`)
    if (user.role === 'superadmin') {
      router.push('/sys/super-add')
    } else if (user.role === 'admin') {
      router.push('/sys/dashboard-admin')
    } else {
      // 志愿者/组织者 → 走原有角色选择页（让用户在双身份间挑当前进入身份）
      router.push('/role-select')
    }
  } catch (e) {
    // 拦截器已弹 Message
  } finally {
    submitting.value = false
  }
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
