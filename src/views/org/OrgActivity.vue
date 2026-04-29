<template>
  <div class="app-container">
    <el-card class="mb-20">
      <div class="toolbar">
        <el-form :inline="true" class="search-form">
          <el-form-item label="活动编号">
            <el-input placeholder="查询活动编号" clearable />
          </el-form-item>
          <el-form-item label="活动名称">
            <el-input v-model="queryForm.name" placeholder="模糊查询" clearable />
          </el-form-item>

          <el-form-item label="活动日期">
            <el-date-picker
              v-model="queryForm.dateRange"
              type="daterange"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD"
              style="width: 250px"
              clearable
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="toolbar-right">
          <el-button type="primary" icon="Plus" @click="openDialog('add')">发布新活动</el-button>
        </div>
      </div>
    </el-card>

    <el-card>
      <el-table :data="pagedTableData" border style="width: 100%">
        <el-table-column label="编号" width="80" align="center">
          <template #default="scope">
            {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="actNo" label="活动编号" width="140" align="center" />

        <el-table-column prop="name" label="活动名称" min-width="180" />

        <el-table-column prop="startTime" label="开始时间" width="170" align="center">
          <template #default="scope">
            {{ scope.row.startTime }}
          </template>
        </el-table-column>

        <el-table-column prop="endTime" label="结束时间" width="170" align="center">
          <template #default="scope">
            {{ scope.row.endTime }}
          </template>
        </el-table-column>

        <el-table-column prop="location" label="活动地点" min-width="160" />

        <el-table-column prop="limitNum" label="招募人数" width="90" align="center" />

        <el-table-column prop="adminStatus" label="管理员审核" width="110" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.adminStatus)">
              {{ scope.row.adminStatus }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="前台启停控制" width="150" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.isPublished"
              active-text="发布"
              inactive-text="停止"
              :disabled="scope.row.adminStatus !== '审核通过'"
              @change="(val) => handleToggle(val, scope.row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="130" align="center">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              link
              icon="Edit"
              @click="openDialog('edit', scope.row)"
            >
              修改
            </el-button>
            <el-button
              size="small"
              type="danger"
              link
              icon="Delete"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <div class="page-size-tip">每页显示 {{ pageSize }} 条数据</div>

        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="filteredTableData.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '申请发布新活动' : '修改活动'"
      width="650px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="活动名称">
          <el-input v-model="form.name" />
        </el-form-item>

        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="活动地点">
          <el-input v-model="form.location" />
        </el-form-item>

        <el-form-item label="招募人数">
          <el-input-number v-model="form.limitNum" :min="1" />
        </el-form-item>

        <el-form-item label="活动简介">
          <el-input v-model="form.desc" type="textarea" rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const dialogVisible = ref(false)
const dialogType = ref('add')

const currentPage = ref(1)
const pageSize = ref(5)

const queryForm = ref({
  name: '',
  dateRange: []
})

const form = ref({
  name: '',
  startTime: '',
  endTime: '',
  location: '',
  limitNum: 10, // 默认10人
  desc: ''
})

const tableData = ref([
  {
    actNo: '202605010001',
    name: '南校区清扫活动',
    startTime: '2026-05-01 08:30:00',
    endTime: '2026-05-01 11:30:00',
    location: '南校区主干道',
    desc: '组织志愿者进行校园清洁，维护公共环境。',
    limitNum: '10',
    adminStatus: '审核通过',
    isPublished: true
  },
  {
    actNo: '202605015001',
    name: '防诈骗宣讲先锋',
    startTime: '2026-05-15 14:00:00',
    endTime: '2026-05-15 16:00:00',
    location: '图书馆报告厅',
    desc: '面向新生开展防诈骗知识宣传。',
    limitNum: '10',
    adminStatus: '审核通过',
    isPublished: false
  },
  {
    actNo: '202609010001',
    name: '迎新后勤志愿',
    startTime: '2026-09-01 07:00:00',
    endTime: '2026-09-01 12:00:00',
    location: '北门集合点',
    desc: '协助新生报到、搬运行李和现场引导。',
    limitNum: '10',
    adminStatus: '待审核',
    isPublished: false
  },
  {
    actNo: '202606200015',
    name: '社区敬老陪伴',
    startTime: '2026-06-20 09:00:00',
    endTime: '2026-06-20 11:00:00',
    location: '星光社区服务中心',
    desc: '陪伴老人聊天、做简单互动活动。',
    limitNum: '10',
    adminStatus: '审核通过',
    isPublished: true
  },
  {
    actNo: '202607120123',
    name: '河岸环保巡查',
    startTime: '2026-07-12 15:00:00',
    endTime: '2026-07-12 17:30:00',
    location: '滨河步道',
    desc: '进行河岸垃圾巡查与环保宣传。',
    limitNum: '10',
    adminStatus: '审核不通过',
    isPublished: false
  }
])

const filteredTableData = computed(() => {
  const nameKeyword = queryForm.value.name.trim().toLowerCase()
  const dateRange = queryForm.value.dateRange || []

  return tableData.value.filter((item) => {
    const matchName = !nameKeyword || item.name.toLowerCase().includes(nameKeyword)

    let matchDate = true
    if (dateRange.length === 2) {
      const start = dateRange[0]
      const end = dateRange[1]
      const itemDate = item.startTime.slice(0, 10)
      matchDate = itemDate >= start && itemDate <= end
    }

    return matchName && matchDate
  })
})

const pagedTableData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredTableData.value.slice(start, end)
})

const getStatusType = (status) => {
  if (status === '审核通过') return 'success'
  if (status === '待审核') return 'warning'
  if (status === '审核不通过') return 'danger'
  return 'info'
}

const handleToggle = (val, row) => {
  if (val) {
    ElMessage.success(`【${row.name}】已重新发布，志愿者现在可以报名了！`)
  } else {
    ElMessage.warning(`【${row.name}】已停止发布，志愿者端已被隐藏！`)
  }
}

const openDialog = (type, row = null) => {
  dialogType.value = type
  if (type === 'edit' && row) {
    form.value = {
      name: row.name,
      limitNum: row.limitNum || 10, // 回显
      startTime: row.startTime,
      endTime: row.endTime,
      location: row.location,
      desc: row.desc
    }
  } else {
    form.value = { name: '', limitNum: 10, startTime: '', endTime: '', location: '', desc: '' }
  }
  dialogVisible.value = true
}

const submitForm = () => {
  ElMessage.success('已提交给管理员审核！')
  dialogVisible.value = false
}

const handleSearch = () => {
  currentPage.value = 1
}

const handleReset = () => {
  queryForm.value = {
    name: '',
    dateRange: []
  }
  currentPage.value = 1
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val) => {
  currentPage.value = val
}

// --- 删除活动逻辑 ---
const handleDelete = (row) => {
  // 危险操作确认弹窗
  ElMessageBox.confirm(
    `确定要删除活动【${row.name}】吗？删除后不可恢复。`,
    '危险操作确认',
    {
      type: 'error',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    }
  )
    .then(() => {
      // 模拟前端删除：将该行从数据源中过滤掉
      tableData.value = tableData.value.filter(item => item.actNo !== row.actNo)
      ElMessage.success(`【${row.name}】删除成功！`)
      
      // 如果删除了当前页的最后一条数据，自动退回上一页
      const totalPages = Math.ceil(filteredTableData.value.length / pageSize.value)
      if (currentPage.value > totalPages && currentPage.value > 1) {
        currentPage.value--
      }
    })
    .catch(() => {
      // 捕获取消操作，防止控制台报错
    })
}
</script>

<style scoped>
.mb-20 {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.search-form {
  flex: 1;
}

.toolbar-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 120px;
}

.pagination-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 18px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-size-tip {
  font-size: 14px;
  color: #606266;
}
</style>