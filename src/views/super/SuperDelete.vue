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
      <el-table :data="list" border v-loading="loading">
        <el-table-column label="序号" width="80" align="center">
          <template #default="scope">{{ (page - 1) * pageSize + scope.$index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="userNo" label="管理员编号" align="center" />
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
        <div class="page-size-tip">共 {{ total }} 条</div>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
// 撤销管理员；后端会把 role 降回 volunteer，is_admin 置 false。超管自己不能被撤
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { admins, revokeAdmin } from '../../api/user'

const queryId = ref('')
const queryName = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const list = ref([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await admins({
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

const handleSearch = () => { page.value = 1; loadData() }
const handleReset = () => { queryId.value = ''; queryName.value = ''; page.value = 1; loadData() }
const handleSizeChange = (v) => { pageSize.value = v; page.value = 1; loadData() }
const handleCurrentChange = (v) => { page.value = v; loadData() }

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定移除【${row.name}】的管理员权限吗？移除后不可恢复。`, '危险操作确认', {
    type: 'error',
    confirmButtonText: '确定移除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await revokeAdmin(row.userId)
      ElMessage.success(`已成功撤销【${row.name}】的管理员权限。`)
      await loadData()
    })
    .catch(() => {})
}
</script>

<style scoped>
.mb-20 { margin-bottom: 20px; }
.pagination-wrap { display: flex; justify-content: space-between; align-items: center; margin-top: 18px; }
.page-size-tip { font-size: 14px; color: #606266; }
</style>
