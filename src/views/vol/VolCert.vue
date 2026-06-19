<template>
  <div class="app-container">

    <!-- 查询 -->
    <el-card class="mb-20">
      <el-form :inline="true">
        <el-form-item label="活动名称">
          <el-input v-model="queryName" placeholder="查询活动名称" style="width: 180px" clearable />
        </el-form-item>

        <el-form-item label="证书状态">
          <el-select v-model="queryStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="有效" value="有效" />
            <el-option label="已失效" value="已失效" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="pagedCertList" border v-loading="loading">

        <el-table-column label="编号" width="55" align="center">
          <template #default="scope">
            {{ (page - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="certNo" label="证书编号" min-width="205" align="center" />
        <el-table-column prop="certName" label="证书标题" min-width="180" />
        <el-table-column prop="activityName" label="对应活动" min-width="170" />
        <el-table-column prop="actNo" label="对应活动编号" width="120" align="center" />

        <el-table-column prop="startTime" label="开始时间" width="160" align="center"/>
        <el-table-column prop="endTime" label="结束时间" width="160" align="center"/>

        <el-table-column label="认证工时" width="120" align="center">
          <template #default="scope">
            <strong style="color: #e63946">
              {{ scope.row.hours }} 小时 {{ scope.row.minutes }} 分钟
            </strong>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="证书状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '已失效' ? 'danger' : 'success'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="90" align="center">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              link
              :disabled="scope.row.status === '已失效'"
              :loading="downloadingId === scope.row.certId"
              @click="downloadCert(scope.row)"
            >
              下载证书
            </el-button>
          </template>
        </el-table-column>

      </el-table>

      <div class="pagination-wrap">
        <div class="page-size-tip">共 {{ filteredCertList.length }} 条</div>

        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="filteredCertList.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>

    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { myCertificates, downloadCertPdf } from '../../api/certificate'

const queryName = ref('')
const queryStatus = ref('')
const page = ref(1)
const pageSize = ref(5)
const certList = ref([])
const loading = ref(false)
const downloadingId = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const res = await myCertificates({ page: 1, pageSize: 500 })
    certList.value = res.rows || []
  } catch (_) { /* 拦截器已处理 */ } finally {
    loading.value = false
  }
}
onMounted(loadData)

const filteredCertList = computed(() => {
  return certList.value.filter(item => {
    const matchName = !queryName.value || (item.activityName || '').includes(queryName.value)
    const matchStatus = !queryStatus.value || item.status === queryStatus.value
    return matchName && matchStatus
  })
})

const pagedCertList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredCertList.value.slice(start, start + pageSize.value)
})

const handleSearch = () => { page.value = 1 }
const handleReset = () => { queryName.value = ''; queryStatus.value = ''; page.value = 1 }

const downloadCert = async (row) => {
  if (row.hours === 0 && row.minutes === 0) {
    ElMessage.warning('认证工时为 0，该证书已自动失效，无法下载')
    return
  }
  downloadingId.value = row.certId
  try {
    await loadData()
    const cert = certList.value.find(c => c.certId === row.certId)
    if (!cert) {
      ElMessage.error('证书不存在')
      return
    }
    if (cert.status === '已失效' || (cert.hours === 0 && cert.minutes === 0)) {
      ElMessage.warning('该证书已失效，无法下载')
      return
    }
    await downloadCertPdf(cert.certId, `${cert.certName || '证书'}-${cert.certId}.pdf`)
    ElMessage.success('证书下载已开始')
  } catch (e) {
    ElMessage.error('下载失败，请确认证书有效')
  } finally {
    downloadingId.value = null
  }
}
</script>

<style scoped>
.mb-20 { margin-bottom: 20px; }

.pagination-wrap{
  display:flex;
  justify-content:space-between;
  align-items:center;
  margin-top:18px;
  flex-wrap:wrap;
}

.page-size-tip{
  font-size:14px;
  color:#606266;
}
</style>
