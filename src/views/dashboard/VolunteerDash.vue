<template>
  <div v-loading="loading">
    <el-row :gutter="20" class="mb-20">
      <el-col :span="8">
        <el-card class="stat-card">
          <h3>我的累计工时</h3>
          <p class="num">{{ m.totalHours || 0 }} <span>小时 &nbsp; </span>{{ m.totalMinutes || 0 }} <span>分钟</span></p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <h3>已获证书</h3>
          <p class="num">{{ m.certificateCount || 0 }} <span>张</span></p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <h3>已通过报名</h3>
          <p class="num">{{ m.approvedRegCount || 0 }} <span>个</span></p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
// 志愿者首页：累计工时 + 证书数 + 已通过报名数。后端按角色返不同 metrics
import { onMounted, ref } from 'vue'
import { dashboard } from '../../api/stat'

const m = ref({})
const loading = ref(false)
onMounted(async () => {
  loading.value = true
  try {
    const res = await dashboard('volunteer')
    m.value = res.metrics || {}
  } catch (_) {
    // 401 等错误已由拦截器统一处理
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.mb-20 { margin-bottom: 20px; }
.stat-card { text-align: center; color: #e63946; }
.stat-card h3 { color: #666; margin-top: 0; font-size: 16px; }
.num { font-size: 32px; font-weight: bold; margin: 10px 0 0; }
.num span { font-size: 14px; color: #999; font-weight: normal; }
</style>
