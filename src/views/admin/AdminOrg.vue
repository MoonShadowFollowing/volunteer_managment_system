<template>
  <div class="app-container">
    <el-card class="mb-20">
      <el-form :inline="true">
        <el-form-item label="申请人编号">
          <el-input v-model="queryId" placeholder="模糊查询编号" clearable />
        </el-form-item>
        <el-form-item label="申请人姓名">
          <el-input v-model="queryName" placeholder="模糊查询姓名" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryStatus" style="width: 140px" @change="loadData">
            <el-option label="待审核" value="待审核" />
            <el-option label="已通过" value="已通过" />
            <el-option label="已拒绝" value="已拒绝" />
            <el-option label="全部" value="" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="list" border v-loading="loading">
        <el-table-column label="编号" width="80" align="center">
          <template #default="scope">
            {{ (page - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="applicantNo" label="申请人编号" width="200" align="center" />
        <el-table-column prop="applicantName" label="申请人姓名" align="center" />
        <el-table-column prop="submittedAt" label="申请时间" width="180" align="center" />
        <el-table-column prop="auditStatus" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.auditStatus==='已通过'?'success':(scope.row.auditStatus==='已拒绝'?'danger':'warning')">
              {{ scope.row.auditStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center">
          <template #default="scope">
            <el-button type="primary" link icon="Document" @click="viewMaterial(scope.row)">查看材料</el-button>
            <el-button v-if="scope.row.auditStatus==='待审核'" type="success" size="small" @click="handleAudit(scope.row, true)">通过</el-button>
            <el-button v-if="scope.row.auditStatus==='待审核'" type="danger" size="small" @click="handleAudit(scope.row, false)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <div class="page-size-tip">共 {{ total }} 条</div>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="组织者申请材料详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="申请人编号">{{ currentApply.applicantNo }}</el-descriptions-item>
        <el-descriptions-item label="申请人姓名">{{ currentApply.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentApply.applicantPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请理由">{{ currentApply.reason }}</el-descriptions-item>
        <el-descriptions-item label="证明材料">
          <template v-if="currentApply.materialUrl">
            <el-image
              v-if="isImage(currentApply.materialUrl)"
              style="width: 120px; height: 120px; border-radius: 4px"
              :src="resolveFileUrl(currentApply.materialUrl)"
              :preview-src-list="[resolveFileUrl(currentApply.materialUrl)]"
              fit="cover"
            />
            <a v-else :href="resolveFileUrl(currentApply.materialUrl)" target="_blank">在新窗口打开材料（PDF）</a>
          </template>
          <span v-else>无</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="dialogVisible = false">返回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 管理员审组织者资质申请，可以预览证明材料；通过后用户立刻有 organizer 资格
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApplications, auditApplication } from '../../api/organizer'
import { resolveFileUrl } from '../../api/file'

const queryName = ref('')
const queryId = ref('')
const queryStatus = ref('待审核')
const page = ref(1)
const pageSize = ref(5)
const total = ref(0)
const dialogVisible = ref(false)
const currentApply = ref({})
const list = ref([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await listApplications({
      status: queryStatus.value || undefined,
      name: queryName.value || undefined,
      userNo: queryId.value || undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    list.value = res.rows || []
    total.value = res.total || 0
  } catch (_) { /* 403 拦截器已处理 */ } finally {
    loading.value = false
  }
}
onMounted(loadData)

const isImage = (url) => /\.(jpg|jpeg|png|gif|webp)$/i.test(url || '')

const viewMaterial = (row) => {
  currentApply.value = row
  dialogVisible.value = true
}

const handleAudit = (row, approve) => {
  ElMessageBox.confirm(`确定要${approve ? '通过' : '拒绝'}该组织者申请？`, '审核确认', {
    type: approve ? 'success' : 'warning'
  }).then(async () => {
    await auditApplication(row.appId, approve)
    ElMessage.success('操作成功')
    dialogVisible.value = false
    await loadData()
  }).catch(() => {})
}

const handleSearch = () => { page.value = 1; loadData() }
const handleReset = () => { queryName.value = ''; queryId.value = ''; queryStatus.value = '待审核'; loadData() }
const handleSizeChange = (v) => { pageSize.value = v; page.value = 1; loadData() }
const handleCurrentChange = (v) => { page.value = v; loadData() }
</script>

<style scoped>
.mb-20 { margin-bottom: 20px; }
.pagination-wrap { display: flex; justify-content: space-between; align-items: center; margin-top: 18px; }
.page-size-tip { font-size: 14px; color: #606266; }
</style>
