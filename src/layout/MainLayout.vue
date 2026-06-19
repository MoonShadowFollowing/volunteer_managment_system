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
          <template #title>
            {{ route.meta.title }}
            <span v-if="route.meta.showBadge && msgUnread > 0" class="unread-dot">{{ msgUnread }}</span>
          </template>
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
            <span>{{ displayIdTag }}</span>
          </div>

          <el-button v-if="currentRole === 'volunteer' && !hasOrgAuth" type="primary" plain size="small" @click="$router.push('/sys/vol-apply-org')" style="margin-right: 15px;">申请成为组织者</el-button>
          <el-button v-if="currentRole === 'volunteer' && hasOrgAuth" type="success" plain size="small" @click="switchRole('organizer')" style="margin-right: 15px;">切换为组织者</el-button>
          <el-button v-if="currentRole === 'organizer'" type="warning" plain size="small" @click="switchRole('volunteer')" style="margin-right: 15px;">切换为志愿者</el-button>

          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar :size="32" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
              <span class="username">{{ userName || (currentRole === 'superadmin' ? '超级管理员' : (currentRole === 'admin' ? '管理员' : '志愿者')) }}</span>
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
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Trophy, Fold, Expand, Ticket } from '@element-plus/icons-vue' // 确保导入图标
import { fetchMe, logout as logoutApi } from '../api/auth'
import { unreadCount, markAllRead } from '../api/message'

const isCollapse = ref(false)
const router = useRouter()
const route = useRoute()

const currentRole = ref(localStorage.getItem('userRole') || 'volunteer')
const hasOrgAuth = ref(localStorage.getItem('isOrganizerQualified') === 'true')
const userName = ref(localStorage.getItem('userName') || '')
const userUsername = ref(localStorage.getItem('userUsername') || '')

const msgUnread = ref(0)

const menus = computed(() => {
  const sysRoutes = router.options.routes.find(r => r.path === '/sys').children
  return sysRoutes.filter(r => r.meta && r.meta.roles && r.meta.roles.includes(currentRole.value) && r.path !== 'vol-apply-org')
})

const toggleCollapse = () => { isCollapse.value = !isCollapse.value }

const fetchUnread = async () => {
  const role = currentRole.value
  const scope = role === 'volunteer' || role === 'organizer' ? role : null
  try {
    const res = await unreadCount({ role: scope })
    msgUnread.value = res ?? 0
  } catch (_) { msgUnread.value = 0 }
}

const switchRole = (targetRole) => {
  currentRole.value = targetRole
  localStorage.setItem('userRole', targetRole)
  fetchUnread()
  router.push(targetRole === 'organizer' ? '/sys/dashboard-org' : '/sys/dashboard-volun')
}

// 计算编号标签
const displayIdTag = computed(() => {
  const roleMap = {
    superadmin: '超级管理员',
    admin: '管理员',
    organizer: '组织者',
    volunteer: '志愿者'
  }
  const prefixMap = {
    admin: 'ADM-',
    organizer: 'ORG-',
    volunteer: 'VOL-'
  }
  const roleName = roleMap[currentRole.value] || '用户'
  if (currentRole.value === 'superadmin') {
    return '超级管理员'
  }
  const username = userUsername.value || ''
  return `${roleName}编号：${prefixMap[currentRole.value] || ''}${username}`
})
const logout = async () => {
  try { await logoutApi() } catch (e) { /* 后端 stub，失败也无所谓 */ }
  localStorage.removeItem('token')
  localStorage.removeItem('isLoggedIn')
  localStorage.removeItem('userRole')
  localStorage.removeItem('isOrganizerQualified')
  localStorage.removeItem('isAdmin')
  localStorage.removeItem('userName')
  localStorage.removeItem('userUsername')
  localStorage.removeItem('userId')
  router.push('/login')
}

// 刷新场景：token 存在但本地缓存丢了 → 调 /auth/me 回填
const onOrgAuthUpdated = () => {
  hasOrgAuth.value = localStorage.getItem('isOrganizerQualified') === 'true'
}

onMounted(async () => {
  if (localStorage.getItem('token')) {
    try {
      const me = await fetchMe()
      if (!localStorage.getItem('userRole')) {
        localStorage.setItem('userRole', me.role)
      }
      localStorage.setItem('isOrganizerQualified', String(me.isOrganizerQualified))
      localStorage.setItem('isAdmin', String(me.isAdmin))
      localStorage.setItem('userName', me.name)
      localStorage.setItem('userUsername', me.username)
      localStorage.setItem('userId', String(me.userId))
      currentRole.value = localStorage.getItem('userRole')
      hasOrgAuth.value = me.isOrganizerQualified
      userName.value = me.name
      userUsername.value = me.username
    } catch (e) {
      // 401 由拦截器统一处理
    }
  }
  fetchUnread()
  window.addEventListener('org-auth-updated', onOrgAuthUpdated)
})

onUnmounted(() => {
  window.removeEventListener('org-auth-updated', onOrgAuthUpdated)
})

watch(() => route.path, async () => {
  hasOrgAuth.value = localStorage.getItem('isOrganizerQualified') === 'true'
  currentRole.value = localStorage.getItem('userRole') || 'volunteer'
  userName.value = localStorage.getItem('userName') || userName.value
  userUsername.value = localStorage.getItem('userUsername') || userUsername.value
  if (route.path.endsWith('-msg')) {
    const scope = currentRole.value === 'volunteer' || currentRole.value === 'organizer' ? currentRole.value : null
    try { await markAllRead({ role: scope }) } catch (_) {}
    msgUnread.value = 0
  }
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
.unread-dot {
  display: inline-block;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  border-radius: 9px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  text-align: center;
  margin-left: 6px;
  vertical-align: middle;
  padding: 0 4px;
  box-sizing: border-box;
}
.main-content { padding: 20px; box-sizing: border-box; }
</style>