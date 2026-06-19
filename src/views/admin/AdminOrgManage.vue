<template>
  <div class="app-container">

    <!-- 查询 -->
    <el-card class="mb-20">
      <el-form :inline="true">
        <el-form-item label="组织者姓名">
          <el-input v-model="queryName" placeholder="模糊查询姓名" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="list" border v-loading="loading">

        <el-table-column label="编号" width="80" align="center">
          <template #default="scope">
            {{ (page - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="userNo" label="组织者编号" width="200" align="center" />

        <el-table-column prop="name" label="组织者姓名" align="center" />

        <el-table-column prop="actCount" label="已发活动数" align="center" />

        <el-table-column label="操作" width="150" align="center">
          <template #default="scope">
            <el-button type="danger" size="small" @click="remove(scope.row)">
              移除资质
            </el-button>
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

  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listOrganizers, revokeOrganizer } from '../../api/organizer'

const queryName = ref('')
const page = ref(1)
const pageSize = ref(5)
const total = ref(0)
const list = ref([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await listOrganizers({
      name: queryName.value || undefined,
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
const handleReset = () => { queryName.value = ''; page.value = 1; loadData() }
const handleSizeChange = (v) => { pageSize.value = v; page.value = 1; loadData() }
const handleCurrentChange = (v) => { page.value = v; loadData() }

const remove = (row) => {
  ElMessageBox.confirm(
    `移除后【${row.name}】将失去组织者权限，是否继续？`,
    '警告',
    { type: 'warning' }
  )
    .then(async () => {
      try {
        await revokeOrganizer(row.userId)
        ElMessage.success('移除成功，系统已通知该用户。')
        await loadData()
      } catch (err) {
        // 后端返回错误码 3003：管理员身份不可被撤销组织者资质
        const msg = err?.response?.data?.message || err?.message || '移除失败'
        if (msg.includes('系统管理员') || msg.includes('管理员身份')) {
          ElMessage.error('该用户是系统管理员，无法撤销其组织者身份！')
        } else {
          ElMessage.error(msg)
        }
      }
    })
    .catch(() => {})
}
</script>

<style scoped>
.mb-20{margin-bottom:20px;}

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
