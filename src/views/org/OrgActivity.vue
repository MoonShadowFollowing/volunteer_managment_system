<template>
  <div class="app-container">
    <el-card class="mb-20">
      <div class="toolbar">
        <el-form :inline="true" class="search-form">
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
      <el-table :data="tableData" border style="width: 100%" v-loading="loading">
        <el-table-column label="编号" width="80" align="center">
          <template #default="scope">
            {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="actNo" label="活动编号" width="140" align="center" />

        <el-table-column prop="name" label="活动名称" min-width="180" />

        <el-table-column prop="startTime" label="开始时间" width="170" align="center" />

        <el-table-column prop="endTime" label="结束时间" width="170" align="center" />

        <el-table-column prop="location" label="活动地点" min-width="160" />

        <el-table-column prop="limitNum" label="招募人数" width="90" align="center" />

        <el-table-column prop="auditStatus" label="管理员审核" width="110" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.auditStatus)">
              {{ scope.row.auditStatus }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="前台启停控制" width="150" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.isPublished"
              active-text="发布"
              inactive-text="停止"
              :disabled="scope.row.auditStatus !== '审核通过'"
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
        <div class="page-size-tip">共 {{ total }} 条数据</div>

        <el-pagination
          v-model:current-page="currentPage"
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
          <el-input-number v-model="form.limitNum" :min="1" :max="1000" />
        </el-form-item>

        <el-form-item label="活动简介">
          <el-input v-model="form.desc" type="textarea" rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="dialogType === 'edit' &amp;&amp; !isFormModified" @click="submitForm">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 组织者的活动 CRUD：新建/改/删/前台启停。改完会自动回到"待审核"
import { onMounted, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listActivities,
  createActivity,
  updateActivity,
  deleteActivity,
  publishActivity
} from '../../api/activity'

const dialogVisible = ref(false)
const dialogType = ref('add')
const submitting = ref(false)
const loading = ref(false)

const currentPage = ref(1)
const pageSize = ref(5)
const total = ref(0)
const tableData = ref([])

const myId = Number(localStorage.getItem('userId')) || undefined

const queryForm = ref({
  name: '',
  dateRange: []
})

const form = ref({
  activityId: null,
  name: '',
  startTime: '',
  endTime: '',
  location: '',
  limitNum: 10,
  desc: ''
})

// 存储原始数据用于检测是否修改
const originalForm = ref({
  name: '',
  startTime: '',
  endTime: '',
  location: '',
  limitNum: 10,
  desc: ''
})

// 检测表单是否被修改（仅用于编辑模式）
const isFormModified = computed(() => {
  if (dialogType.value !== 'edit') return true // 新增模式始终可提交
  const f = form.value
  const o = originalForm.value
  return f.name !== o.name ||
         f.startTime !== o.startTime ||
         f.endTime !== o.endTime ||
         f.location !== o.location ||
         f.limitNum !== o.limitNum ||
         f.desc !== o.desc
})

const loadData = async () => {
  loading.value = true
  try {
    const range = queryForm.value.dateRange || []
    const res = await listActivities({
      organizerId: myId,
      name: queryForm.value.name || undefined,
      startDate: range.length === 2 ? range[0] : undefined,
      endDate: range.length === 2 ? range[1] : undefined,
      page: currentPage.value,
      pageSize: pageSize.value
    })
    tableData.value = res.rows || []
    total.value = res.total || 0
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const getStatusType = (status) => {
  if (status === '审核通过') return 'success'
  if (status === '待审核') return 'warning'
  if (status === '审核不通过') return 'danger'
  return 'info'
}

const handleToggle = async (val, row) => {
  try {
    await publishActivity(row.activityId, val)
    ElMessage.success(val ? `【${row.name}】已发布，志愿者可报名了！` : `【${row.name}】已停止发布`)
  } catch (e) {
    row.isPublished = !val // 失败回滚开关
  }
}

const openDialog = (type, row = null) => {
  dialogType.value = type
  if (type === 'edit' && row) {
    form.value = {
      activityId: row.activityId,
      name: row.name,
      limitNum: Number(row.limitNum) || 10,
      startTime: row.startTime,
      endTime: row.endTime,
      location: row.location,
      desc: row.desc
    }
    // 保存原始数据用于检测是否修改
    originalForm.value = {
      name: row.name,
      startTime: row.startTime,
      endTime: row.endTime,
      location: row.location,
      limitNum: Number(row.limitNum) || 10,
      desc: row.desc
    }
  } else {
    form.value = { activityId: null, name: '', limitNum: 10, startTime: '', endTime: '', location: '', desc: '' }
    originalForm.value = { name: '', startTime: '', endTime: '', location: '', limitNum: 10, desc: '' }
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  const f = form.value
  if (!f.name || !f.startTime || !f.endTime || !f.location) {
    return ElMessage.warning('请填写活动名称、时间和地点')
  }
  if (f.endTime <= f.startTime) {
    return ElMessage.warning('结束时间必须晚于开始时间')
  }
  const body = {
    name: f.name,
    desc: f.desc,
    location: f.location,
    startTime: f.startTime,
    endTime: f.endTime,
    limitNum: f.limitNum
  }
  submitting.value = true
  try {
    if (dialogType.value === 'edit') {
      await updateActivity(f.activityId, body)
      ElMessage.success('修改成功，已重新提交管理员审核！')
    } else {
      await createActivity(body)
      ElMessage.success('已提交给管理员审核！')
    }
    dialogVisible.value = false
    await loadData()
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const handleReset = () => {
  queryForm.value = { name: '', dateRange: [] }
  currentPage.value = 1
  loadData()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadData()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadData()
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除活动【${row.name}】吗？删除后不可恢复。`,
    '危险操作确认',
    { type: 'error', confirmButtonText: '确定删除', cancelButtonText: '取消' }
  )
    .then(async () => {
      await deleteActivity(row.activityId)
      ElMessage.success(`【${row.name}】删除成功！`)
      const totalPages = Math.ceil((total.value - 1) / pageSize.value)
      if (currentPage.value > totalPages && currentPage.value > 1) currentPage.value--
      await loadData()
    })
    .catch(() => {})
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
