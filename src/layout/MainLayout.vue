<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside-menu">
      <div class="logo-box">
        <el-icon v-if="isCollapse" :size="24" color="#e63946"><Trophy /></el-icon>
        <span v-else class="logo-text">VMS 管理系统</span>
      </div>
      <el-menu active-text-color="#e63946" background-color="#ffffff" class="el-menu-vertical" :default-active="$route.path" text-color="#606266" :collapse="isCollapse" router>
        <el-menu-item v-for="route in menus" :key="route.path" :index="'/sys/' + route.path">
          <el-icon><component :is="route.meta.icon"></component></el-icon>
          <template #title>{{ route.meta.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left" @click="toggleCollapse">
          <el-icon :size="24" class="toggle-btn"><Fold v-if="!isCollapse"/><Expand v-else/></el-icon>
          <span class="page-title">{{ $route.meta.title }}</span>
        </div>
        <div class="header-right">
          <div class="user-id-tag">
            <span>{{ currentRole === 'admin' ? '管理员编号：ADM-00001' : currentRole === 'organizer' ? '组织者编号：ORG-13421' : '志愿者编号：VOL-13421' }}</span>
          </div>

          <el-button v-if="currentRole === 'volunteer' && !hasOrgAuth" type="primary" plain size="small" @click="$router.push('/sys/vol-apply-org')" style="margin-right: 15px;">申请成为组织者</el-button>
          <el-button v-if="currentRole === 'volunteer' && hasOrgAuth" type="success" plain size="small" @click="switchRole('organizer')" style="margin-right: 15px;">切换为组织者</el-button>
          <el-button v-if="currentRole === 'organizer'" type="warning" plain size="small" @click="switchRole('volunteer')" style="margin-right: 15px;">切换为志愿者</el-button>

          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar :size="32" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
              <span class="username">{{ currentRole === 'admin' ? '管理员' : '张三' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu><el-dropdown-item @click="logout">退出登录</el-dropdown-item></el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Trophy, Fold, Expand, Ticket } from '@element-plus/icons-vue' // 确保导入图标

const isCollapse = ref(false)
const router = useRouter()
const route = useRoute()

const currentRole = ref(localStorage.getItem('userRole') || 'volunteer')
const hasOrgAuth = ref(localStorage.getItem('isOrganizerQualified') === 'true')

const menus = computed(() => {
  const sysRoutes = router.options.routes.find(r => r.path === '/sys').children
  return sysRoutes.filter(r => r.meta && r.meta.roles && r.meta.roles.includes(currentRole.value) && r.path !== 'vol-apply-org')
})

const toggleCollapse = () => { isCollapse.value = !isCollapse.value }
const switchRole = (targetRole) => {
  currentRole.value = targetRole
  localStorage.setItem('userRole', targetRole)
  router.push(targetRole === 'organizer' ? '/sys/dashboard-org' : '/sys/dashboard-volun')
}
const logout = () => { localStorage.setItem('isLoggedIn', 'false'); router.push('/login') }

watch(() => route.path, () => {
  hasOrgAuth.value = localStorage.getItem('isOrganizerQualified') === 'true'
  currentRole.value = localStorage.getItem('userRole') || 'volunteer'
})
</script>

<style scoped>
.layout-container { height: 100vh; width: 100vw; }
.aside-menu { background-color: #fff; border-right: 1px solid #eee; transition: width 0.3s; display: flex; flex-direction: column; }
.logo-box { height: 60px; display: flex; align-items: center; justify-content: center; border-bottom: 1px solid #f0f0f0; }
.logo-text { font-size: 18px; font-weight: bold; color: #e63946; }
.el-menu-vertical { border-right: none; flex: 1; }
.header { background-color: #fff; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; box-shadow: 0 1px 4px rgba(0,0,0,.05); z-index: 10; }
.header-left, .header-right { display: flex; align-items: center; }
.toggle-btn { cursor: pointer; margin-right: 15px; color: #666; }

/* 编号标签样式 */
.user-id-tag {
  display: flex;
  align-items: center;
  background-color: #f4f4f5;
  color: #909399;
  padding: 0 12px;
  height: 28px;
  border-radius: 14px;
  font-size: 13px;
  margin-right: 20px;
  border: 1px solid #e9e9eb;
}

.user-info { display: flex; align-items: center; cursor: pointer; outline: none; }
.username { margin: 0 8px; color: #666; }
.main-content { padding: 20px; box-sizing: border-box; }
</style>