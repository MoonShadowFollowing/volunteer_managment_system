<template>
  <div class="app-container">
    <el-card class="mb-20">
      <el-form :inline="true">
        <el-form-item label="管理员编号">
          <el-input v-model="queryId" placeholder="模糊查询编号" clearable />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="queryName" placeholder="模糊查询姓名" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="pagedData" border>
        <el-table-column label="序号" width="80" align="center">
          <template #default="scope">{{ (page - 1) * pageSize + scope.$index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="adminNo" label="管理员编号" align="center" />
        <el-table-column prop="name" label="姓名" align="center" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <el-button type="danger" size="small" icon="Delete" @click="handleDelete(scope.row)">
              移除管理员
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <div class="page-size-tip">每页显示 {{ pageSize }} 条数据</div>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20]"
          :total="filteredList.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const queryId = ref('')
const queryName = ref('')
const page = ref(1)
const pageSize = ref(10)

// 模拟现有管理员数据
const adminList = ref([
  { adminNo: 'ADM-10001', name: '赵主管' },
  { adminNo: 'ADM-10002', name: '钱干事' },
  { adminNo: 'ADM-10003', name: '孙老师' }
])

const filteredList = computed(() => {
  return adminList.value.filter(item => {
    const matchId = !queryId.value || item.adminNo.includes(queryId.value)
    const matchName = !queryName.value || item.name.includes(queryName.value)
    return matchId && matchName
  })
})

const pagedData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const handleSearch = () => { page.value = 1 }
const handleReset = () => { queryId.value = ''; queryName.value = ''; page.value = 1 }

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定移除【${row.name}】的管理员权限吗？移除后不可恢复。`, '危险操作确认', { 
    type: 'error',
    confirmButtonText: '确定移除',
    cancelButtonText: '取消'
  })
    .then(() => {
      ElMessage.success(`已成功撤销【${row.name}】的管理员权限。`)
      adminList.value = adminList.value.filter(item => item.adminNo !== row.adminNo)
    })
    .catch(() => {}) // 防止取消报错
}
</script>

<style scoped>
.mb-20 { margin-bottom: 20px; }
.pagination-wrap { display: flex; justify-content: space-between; align-items: center; margin-top: 18px; }
.page-size-tip { font-size: 14px; color: #606266; }
</style>