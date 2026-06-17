<template>
  <div class="app-container">
    <el-card>
      <template #header><span style="font-weight:bold;">申请成为组织者</span></template>

      <div style="max-width: 640px; margin: 20px auto;">
        <el-form label-width="120px">
          <el-form-item label="编号">
            <el-input :value="volNo" disabled />
          </el-form-item>
          <el-form-item label="申请人姓名">
            <el-input :value="userName" disabled />
          </el-form-item>
          <el-form-item label="申请理由">
            <el-input v-model="reason" type="textarea" rows="4" placeholder="请简述申请成为组织者的理由..." />
          </el-form-item>
          <el-form-item label="证明材料上传">
            <el-upload
              :action="uploadAction"
              :headers="uploadHeaders()"
              name="file"
              :limit="1"
              :on-success="onUploadSuccess"
              :on-error="onUploadError"
              :before-upload="beforeUpload"
            >
              <el-button type="primary">选择文件上传</el-button>
              <template #tip>
                <div style="color: #999; font-size: 12px; margin-top: 5px;">支持 jpg/png/gif/webp/pdf，≤5MB（学生证或社团盖章证明）</div>
              </template>
            </el-upload>
            <div v-if="materialUrl" style="margin-top:8px;">
              ✅ 已上传：<a :href="resolveFileUrl(materialUrl)" target="_blank">查看</a>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="submitting" @click="submitApply">提交申请</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card style="margin-top:20px;">
      <template #header><span style="font-weight:bold;">我的申请记录</span></template>
      <el-table :data="myApps" border v-loading="loading">
        <el-table-column label="编号" width="70" align="center">
          <template #default="scope">{{ scope.$index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="申请理由" show-overflow-tooltip />
        <el-table-column label="证明材料" width="100" align="center">
          <template #default="scope">
            <a v-if="scope.row.materialUrl" :href="resolveFileUrl(scope.row.materialUrl)" target="_blank">查看</a>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" width="180" align="center" />
        <el-table-column prop="auditStatus" label="审核状态" width="110" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.auditStatus==='已通过'?'success':(scope.row.auditStatus==='已拒绝'?'danger':'warning')">
              {{ scope.row.auditStatus }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadAction, uploadHeaders, resolveFileUrl } from '../../api/file'
import { submitApplication, myApplications } from '../../api/organizer'

const userName = ref(localStorage.getItem('userName') || '')
const formatUserNo = () => {
  const role = localStorage.getItem('userRole') || 'volunteer'
  const username = localStorage.getItem('userUsername') || ''
  const prefix =
    role === 'superadmin' ? 'SUP' :
    role === 'admin' ? 'ADM' :
    role === 'organizer' ? 'ORG' : 'VOL'
  return prefix + '-' + username
}
const volNo = formatUserNo()

const reason = ref('')
const materialUrl = ref('')
const submitting = ref(false)
const loading = ref(false)
const myApps = ref([])

const loadApps = async () => {
  loading.value = true
  try {
    const res = await myApplications({ page: 1, pageSize: 100 })
    myApps.value = res.rows || []
  } finally {
    loading.value = false
  }
}
onMounted(loadApps)

const beforeUpload = (file) => {
  const okType = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'application/pdf'].includes(file.type)
  if (!okType) { ElMessage.error('只支持 jpg/png/gif/webp/pdf'); return false }
  if (file.size / 1024 / 1024 > 5) { ElMessage.error('文件不能超过 5MB'); return false }
  return true
}

const onUploadSuccess = (response) => {
  if (response && response.code === 0 && response.data) {
    materialUrl.value = response.data.url
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response?.msg || '上传失败')
  }
}
const onUploadError = () => ElMessage.error('上传失败，请重试')

const submitApply = async () => {
  if (!reason.value.trim()) return ElMessage.warning('请填写申请理由')
  if (!materialUrl.value) return ElMessage.warning('请先上传证明材料')
  submitting.value = true
  try {
    await submitApplication({ reason: reason.value.trim(), materialUrl: materialUrl.value })
    ElMessage.success('申请已提交，等待管理员审核！')
    reason.value = ''
    materialUrl.value = ''
    await loadApps()
  } catch (e) {
    /* 拦截器已提示（如重复提交） */
  } finally {
    submitting.value = false
  }
}
</script>
