<template>
  <div v-loading="loading">
    <el-row :gutter="20" class="mb-20">
      <el-col :span="8">
        <el-card class="stat-card">
          <h3>我管理的活动</h3><p class="num">{{ m.myActivityCount || 0 }} <span>个</span></p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <h3>发布中的活动</h3><p class="num">{{ m.publishedActivityCount || 0 }} <span>个</span></p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card">
          <h3>待审核报名</h3><p class="num">{{ m.pendingRegCount || 0 }} <span>人</span></p>
        </el-card>
      </el-col>
    </el-row>
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
    const res = await dashboard('organizer')
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
.stat-card h3 { color: #666; margin-top: 0; }
.num { font-size: 32px; font-weight: bold; margin: 10px 0 0; }
.num span { font-size: 14px; font-weight: normal; }
</style>
