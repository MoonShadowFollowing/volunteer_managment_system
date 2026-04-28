<template>
  <div class="app-container">
    <el-card class="mb-20">
      <el-form :inline="true">
        <el-form-item label="申请人编号">
          <el-input placeholder="编号查询" clearable />
        </el-form-item>
        <el-form-item label="申请人">
          <el-input v-model="queryName" placeholder="姓名查询" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="pagedList" border>
        <el-table-column label="编号" width="80" align="center">
          <template #default="scope">
            {{ (page - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="id" label="申请人编号" width="120" align="center" />
        <el-table-column prop="name" label="申请人姓名" />
        <el-table-column prop="time" label="申请时间" width="180" align="center" />
        <el-table-column label="操作" width="250" align="center">
          <template #default="scope">
            <el-button type="primary" link icon="Document" @click="viewMaterial(scope.row)">查看材料</el-button>
            <el-button type="success" size="small" @click="handleAudit(scope.row, 'pass')">通过</el-button>
            <el-button type="danger" size="small" @click="handleAudit(scope.row, 'reject')">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <div class="page-size-tip">每页显示 {{ pageSize }} 条数据</div>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="filteredList.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="组织者申请材料详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="申请人编号">{{ currentApply.id }}</el-descriptions-item>
        <el-descriptions-item label="申请人姓名">{{ currentApply.name }}</el-descriptions-item>
        <el-descriptions-item label="申请理由">
          {{ currentApply.orgIntro }}
        </el-descriptions-item>
        <el-descriptions-item label="证明材料">
          <el-image 
            style="width: 100px; height: 100px; border-radius: 4px" 
            :src="currentApply.materialImg" 
            :preview-src-list="[currentApply.materialImg]"
            fit="cover"
          />
          <p style="font-size: 12px; color: #909399;">点击图片可预览</p>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="dialogVisible = false">返回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const queryName = ref('')
const queryOrg = ref('')
const page = ref(1)
const pageSize = ref(5)
const dialogVisible = ref(false)
const currentApply = ref({})

const orgList = ref([
  {
    id: 'VOL-12345',
    name: '张老三',
    time: '2026-04-01 10:00:00',
    phone: '13800138000',
    orgIntro: '需要发布学校大型活动的宣传与物料设计志愿活动',
    materialImg: 'https://via.placeholder.com/150'
  },
  {
    id: 'VOL-12231',
    name: '李小明',
    time: '2026-04-02 09:20:00',
    phone: '13912345678',
    orgIntro: '需要统筹全校志愿时数录入与志愿者注册',
    materialImg: 'https://via.placeholder.com/150'
  }
])

const filteredList = computed(() => {
  return orgList.value.filter(item => 
    (!queryName.value || item.name.includes(queryName.value)) &&
    (!queryOrg.value || item.orgName.includes(queryOrg.value))
  )
})

const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const viewMaterial = (row) => {
  currentApply.value = row
  dialogVisible.value = true
}

const handleAudit = (row, result) => {
  const actionText = result === 'pass' ? '通过该组织者申请？' : '拒绝该申请？'
  ElMessageBox.confirm(`确定要${actionText}`, '审核确认', {
    type: result === 'pass' ? 'success' : 'warning'
  }).then(() => {
    ElMessage.success('操作成功')
    dialogVisible.value = false
    // 实际应在此处调用API删除或更新状态
  }).catch(() => {})
}

const handleSearch = () => { page.value = 1 }
const handleReset = () => { queryName.value = ''; queryOrg.value = ''; page.value = 1 }
</script>

<style scoped>
.mb-20 { margin-bottom: 20px; }
.pagination-wrap { display: flex; justify-content: space-between; align-items: center; margin-top: 18px; }
.page-size-tip { font-size: 14px; color: #606266; }
</style>