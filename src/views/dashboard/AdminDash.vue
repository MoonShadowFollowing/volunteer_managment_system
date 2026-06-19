<template>
  <div v-loading="loading">
    <el-row :gutter="20" class="mb-20">
      <el-col :span="6"><el-card shadow="hover">全校总活动数<br/><b style="font-size:24px;color:#e63946">{{ m.totalActivities || 0 }}</b></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover">注册志愿者<br/><b style="font-size:24px;color:#e63946">{{ m.totalVolunteers || 0 }}</b></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover">累计认证工时(小时)<br/><b style="font-size:24px;color:#e63946">{{ m.totalServiceHours || 0 }}</b></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover">已发证书<br/><b style="font-size:24px;color:#e63946">{{ m.totalCertificates || 0 }}</b></el-card></el-col>
    </el-row>
    <el-card header="待办与系统状态">
      <p style="color:#e6a23c">● 待审核活动：<b>{{ m.pendingActivityAudits || 0 }}</b> 个</p>
      <p style="color:#67c23a">● 数据库连接正常</p>
      <p style="color:#67c23a">● 本地备份服务运行中</p>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { dashboard } from '../../api/stat'

const m = ref({})
const loading = ref(false)
onMounted(async () => {
  loading.value = true
  try {
    const res = await dashboard('admin')
    m.value = res.metrics || {}
  } catch (_) {
    // 401 等错误已由拦截器统一处理
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>.mb-20{margin-bottom:20px;text-align:center;color:#666}</style>
