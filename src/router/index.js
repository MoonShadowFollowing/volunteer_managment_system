import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'

const routes = [
  { path: '/', redirect: '/login' }, // 默认跳角色选择（模拟已登录门户）
  { path: '/login', component: () => import('../views/login/Login.vue') },
  { path: '/role-select', component: () => import('../views/login/RoleSelect.vue') }, // 角色选择页
  {
    path: '/sys',
    component: MainLayout,
    children: [
      // --- 工作台 ---
      { path: 'dashboard-volun', component: () => import('../views/dashboard/VolunteerDash.vue'), meta: { title: '首页', icon: 'HomeFilled', roles: ['volunteer'] } },
      { path: 'dashboard-org', component: () => import('../views/dashboard/OrgDash.vue'), meta: { title: '首页', icon: 'HomeFilled', roles: ['organizer'] } },
      { path: 'dashboard-admin', component: () => import('../views/dashboard/AdminDash.vue'), meta: { title: '首页', icon: 'HomeFilled', roles: ['admin'] } },

      // --- 志愿者模块 ---
      { path: 'vol-activity', component: () => import('../views/vol/VolActivity.vue'), meta: { title: '志愿活动', icon: 'List', roles: ['volunteer'] } },
      { path: 'vol-applied', component: () => import('../views/vol/VolApplied.vue'), meta: { title: '已报名活动', icon: 'Checked', roles: ['volunteer'] } },
      { path: 'vol-cert', component: () => import('../views/vol/VolCert.vue'), meta: { title: '证书查询', icon: 'Medal', roles: ['volunteer'] } },
      { path: 'vol-msg', component: () => import('../views/vol/VolMsg.vue'), meta: { title: '我的消息', icon: 'Message', roles: ['volunteer'] } },
      { path: 'vol-apply-org', component: () => import('../views/vol/VolApplyOrg.vue'), meta: { title: '申请成为组织者', roles: ['volunteer'] } },

      // --- 组织者模块 ---
      { path: 'org-activity', component: () => import('../views/org/OrgActivity.vue'), meta: { title: '志愿活动', icon: 'Edit', roles: ['organizer'] } },
      { path: 'org-manage', component: () => import('../views/org/OrgManage.vue'), meta: { title: '活动管理', icon: 'Management', roles: ['organizer'] } },
      { path: 'org-msg', component: () => import('../views/org/OrgMsg.vue'), meta: { title: '我的消息', icon: 'Message', roles: ['organizer'] } },

      // --- 管理员模块 ---
      { path: 'admin-org', component: () => import('../views/admin/AdminOrg.vue'), meta: { title: '组织者审核', icon: 'User', roles: ['admin'] } },
      { path: 'admin-org-manage', component: () => import('../views/admin/AdminOrgManage.vue'), meta: { title: '组织者管理', icon: 'Coordinate', roles: ['admin'] } },
      { path: 'admin-act', component: () => import('../views/admin/AdminAct.vue'), meta: { title: '活动审核', icon: 'DocumentChecked', roles: ['admin'] } },
      {
        path: 'admin-notice',
        component: () => import('../views/admin/AdminNotice.vue'),
        meta: { title: '公告管理', icon: 'Notification', roles: ['admin'] } // 统一为公告管理
      },

      { path: 'super-add', component: () => import('../views/super/SuperAdd.vue'), meta: { title: '添加管理员', icon: 'User', roles: ['superadmin'] } },
      { path: 'super-delete', component: () => import('../views/super/SuperDelete.vue'), meta: { title: '移除管理员', icon: 'Delete', roles: ['superadmin'] } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(process.env.BASE_URL), routes })

// 路由守卫：模拟门户登录拦截
router.beforeEach((to, from, next) => {
  const isLogin = localStorage.getItem('isLoggedIn') === 'true'
  if (to.path !== '/login' && !isLogin) {
    next('/login') // 未登录打回登录页
  } else {
    next()
  }
})

export default router