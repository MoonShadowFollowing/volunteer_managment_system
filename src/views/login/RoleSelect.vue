<template>
  <div class="select-container">
    <div class="content-box">
      <h2 style="color: #e63946; text-align: center; margin-bottom: 40px;">请选择进入系统的身份</h2>
      <el-row :gutter="30" justify="center">
        <!-- 志愿者入口 (所有人都有) -->
        <el-col :span="7">
          <el-card class="role-card" shadow="hover" @click="enterSys('volunteer')">
            <el-icon size="50" color="#e63946"><User /></el-icon>
            <h3>志愿者</h3>
          </el-card>
        </el-col>
        <!-- 组织者入口 (需权限验证) -->
        <el-col :span="7">
          <el-card class="role-card" shadow="hover" @click="enterSys('organizer')">
            <el-icon size="50" color="#e63946"><Platform /></el-icon>
            <h3>组织者</h3>
          </el-card>
        </el-col>
        <!-- 管理员入口 (需权限验证) -->
        <el-col :span="7">
          <el-card class="role-card" shadow="hover" @click="enterSys('admin')">
            <el-icon size="50" color="#e63946"><DataBoard /></el-icon>
            <h3>系统管理员</h3>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const enterSys = (role) => {
  // 前端先挡一道，组织者/管理员要有相应资格才能进去
  // 后端的 @PreAuthorize 是真正的兜底
  if (role === 'organizer' && localStorage.getItem('isOrganizerQualified') !== 'true') {
    return ElMessage.error('无权限：您目前不是组织者，请先以志愿者身份进入系统申请！')
  }
  if (role === 'admin' && localStorage.getItem('isAdmin') !== 'true') {
    return ElMessage.error('无权限：仅平台管理员可进入！(测试请用admin账号登录)')
  }

  // 把选中身份写回 localStorage，下一步路由就按这个走
  localStorage.setItem('userRole', role)
  if (role === 'admin') router.push('/sys/dashboard-admin')
  else if (role === 'organizer') router.push('/sys/dashboard-org')
  else router.push('/sys/dashboard-volun')
}
</script>

<style scoped>
.select-container { height: 100vh; display: flex; justify-content: center; align-items: center; background: #fff7f7; }
.content-box { width: 900px; }
.role-card { text-align: center; padding: 30px 10px; cursor: pointer; transition: all 0.3s; border-radius: 10px;}
.role-card:hover { transform: translateY(-10px); box-shadow: 0 15px 30px rgba(230,57,70,0.15) !important; }
.role-card h3 { margin: 15px 0 10px; color: #333; }
.role-card p { color: #999; font-size: 13px; margin: 0; }
</style>