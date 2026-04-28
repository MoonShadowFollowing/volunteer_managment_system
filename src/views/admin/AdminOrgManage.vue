<template>
  <div class="app-container">

    <!-- 查询 -->
    <el-card class="mb-20">
      <el-form :inline="true">

        <el-form-item label="组织者编号">
          <el-input placeholder="查询组织者编号" clearable />
        </el-form-item>

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
      <el-table :data="pagedList" border>

        <!-- 编号 -->
        <el-table-column label="编号" width="80" align="center">
          <template #default="scope">
            {{ (page - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="orgNo" label="组织者编号" width="140" align="center" />

        <el-table-column prop="name" label="组织者姓名" />

        <el-table-column prop="actCount" label="已发活动数" align="center" />

        <el-table-column label="操作" width="150" align="center">
          <template #default>
            <el-button type="danger" size="small" @click="remove">
              移除资质
            </el-button>
          </template>
        </el-table-column>

      </el-table>

      <!-- 分页 -->
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

  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

/* 查询 */
const queryName = ref('')
const queryOrg = ref('')

/* 分页 */
const page = ref(1)
const pageSize = ref(5)

/* 数据 */
const list = ref([
  {
    orgNo: 'ORG-12345', 
    name: '李四',
    actCount: 5
  },
  {
    orgNo: 'ORG-54321', 
    name: '王小明',
    actCount: 2
  }
])

/* 过滤 */
const filteredList = computed(() => {
  return list.value.filter(item => {
    const matchName =
      !queryName.value || item.name.includes(queryName.value)

    const matchOrg =
      !queryOrg.value || item.org.includes(queryOrg.value)

    return matchName && matchOrg
  })
})

/* 分页 */
const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const handleSearch = () => {
  page.value = 1
}

const handleReset = () => {
  queryName.value = ''
  queryOrg.value = ''
  page.value = 1
}

const remove = () => {
  ElMessageBox.confirm(
    '移除后该人员将失去组织者权限，是否继续？',
    '警告',
    { type: 'warning' }
  )
    .then(() => {
      ElMessage.success('移除成功，系统已自动发送通知告知该用户。')
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