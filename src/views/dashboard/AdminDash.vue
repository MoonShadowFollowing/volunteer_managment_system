<template>
  <div v-loading="loading">
    <el-row :gutter="20" class="mb-20">
      <el-col :span="6"><el-card shadow="hover">总活动数<br/><b style="font-size:24px;color:#e63946">{{ m.totalActivities || 0 }}</b></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover">注册志愿者<br/><b style="font-size:24px;color:#e63946">{{ m.totalVolunteers || 0 }}</b></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover">累计认证工时(小时)<br/><b style="font-size:24px;color:#e63946">{{ m.totalServiceHours || 0 }}</b></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover">已发证书<br/><b style="font-size:24px;color:#e63946">{{ m.totalCertificates || 0 }}</b></el-card></el-col>
    </el-row>
    <el-card header="待办与系统状态">
      <p style="color:#e6a23c">● 待审核活动：<b>{{ m.pendingActivityAudits || 0 }}</b> 个</p>
      <p style="color:#67c23a">● 数据库连接正常</p>
      <p style="color:#67c23a">● 本地备份服务运行中</p>
    </el-card>

    <el-card style="margin-top:20px" header="月度志愿工时报表">
      <el-form :inline="true">
        <el-form-item label="年份">
          <el-input-number v-model="reportYear" :min="2020" :max="2099" />
        </el-form-item>
        <el-form-item label="月份">
          <el-input-number v-model="reportMonth" :min="1" :max="12" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" icon="Download" :loading="exporting" @click="exportMonthly">导出月度汇总 Excel</el-button>
        </el-form-item>
      </el-form>
      <p style="color:#909399;font-size:13px;margin:8px 0 0">将按签退月份统计全部志愿者参与活动数与累计工时，可用于行政归档与综测对接。</p>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dashboard } from '../../api/stat'
import { downloadMonthlyXlsx } from '../../api/report'

const m = ref({})
const loading = ref(false)
const exporting = ref(false)
const now = new Date()
const reportYear = ref(now.getFullYear())
const reportMonth = ref(now.getMonth() + 1)

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

const exportMonthly = async () => {
  exporting.value = true
  try {
    await downloadMonthlyXlsx(reportYear.value, reportMonth.value)
    ElMessage.success(`${reportYear.value} 年 ${reportMonth.value} 月报表已导出`)
  } catch (_) { /* 拦截器已提示 */ } finally {
    exporting.value = false
  }
}
</script>

<style scoped>.mb-20{margin-bottom:20px;text-align:center;color:#666}</style>
