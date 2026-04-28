<template>
  <div class="app-container">
    <el-card class="mb-20">
      <el-form :inline="true">
        <el-form-item label="人员编号">
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
        <el-table-column prop="userNo" label="人员编号" align="center" />
        <el-table-column prop="name" label="姓名" align="center" />
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <el-button type="success" size="small" icon="Plus" @click="handleAdd(scope.row)">
              设为管理员
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

// 模拟普通人员/志愿者数据库
const userList = ref([
  { userNo: 'VOL-20241', name: '王大锤' },
  { userNo: 'VOL-20242', name: '李梅梅' },
  { userNo: 'VOL-20243', name: '张小强' }
])

const filteredList = computed(() => {
  return userList.value.filter(item => {
    const matchId = !queryId.value || item.userNo.includes(queryId.value)
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

const handleAdd = (row) => {
  ElMessageBox.confirm(`确定将【${row.name}】提升为系统管理员吗？`, '授权确认', { type: 'warning' })
    .then(() => {
      ElMessage.success('设置成功！该用户已获取管理员权限。')
      // 模拟前端移除该行
      userList.value = userList.value.filter(item => item.userNo !== row.userNo)
    })
    .catch(() => {}) // 防止取消报错
}
</script>

<style scoped>
.mb-20 { margin-bottom: 20px; }
.pagination-wrap { display: flex; justify-content: space-between; align-items: center; margin-top: 18px; }
.page-size-tip { font-size: 14px; color: #606266; }
</style>