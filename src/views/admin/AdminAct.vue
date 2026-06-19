<template>
  <div class="app-container">

    <!-- 查询栏 -->
    <el-card class="mb-20">
      <el-form :inline="true">

        <el-form-item label="活动名称">
          <el-input
            v-model="queryForm.actName"
            placeholder="模糊查询活动名称"
            style="width: 150px"
            clearable
          />
        </el-form-item>

        <el-form-item label="审核状态">
          <el-select v-model="queryForm.auditStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="待审核" value="待审核" />
            <el-option label="审核通过" value="审核通过" />
            <el-option label="审核不通过" value="审核不通过" />
          </el-select>
        </el-form-item>

        <el-form-item label="活动日期">
          <el-date-picker
            v-model="queryForm.timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 250px"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">
            查询
          </el-button>
          <el-button @click="resetSearch">
            重置
          </el-button>
        </el-form-item>

      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="tableData" border v-loading="loading">

        <!-- 编号 -->
        <el-table-column label="编号" width="80" align="center">
          <template #default="scope">
            {{ (page - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="actNo" label="活动编号" width="140" align="center" />
        <el-table-column prop="name" label="活动名称" min-width="180" show-overflow-tooltip />

        <el-table-column label="活动开始时间" width="180" align="center">
          <template #default="scope">
            {{ scope.row.startTime }}
          </template>
        </el-table-column>

        <el-table-column label="活动结束时间" width="180" align="center">
          <template #default="scope">
            {{ scope.row.endTime }}
          </template>
        </el-table-column>

        <el-table-column prop="limitNum" label="招募人数" width="100" align="center" />

        <el-table-column prop="auditStatus" label="审核状态" width="130" align="center">
          <template #default="scope">
            <el-tag
              :type="
                scope.row.auditStatus === '审核通过'
                  ? 'success'
                  : scope.row.auditStatus === '审核不通过'
                  ? 'danger'
                  : 'warning'
              "
            >
              {{ scope.row.auditStatus }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="审核与操作" width="220" align="center" fixed="right">
          <template #default="scope">

            <el-button
              type="primary"
              size="small"
              link
              icon="View"
              @click="viewDetail(scope.row)"
            >
              详情
            </el-button>

            <el-button
              v-if="scope.row.auditStatus === '待审核'"
              type="success"
              size="small"
              @click="handleAction(scope.row, true)"
            >
              批准
            </el-button>

            <el-button
              v-if="scope.row.auditStatus === '待审核'"
              type="danger"
              size="small"
              @click="handleAction(scope.row, false)"
            >
              驳回
            </el-button>

          </template>
        </el-table-column>

      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <div class="page-size-tip">共 {{ total }} 条数据</div>

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

    <!-- 详情弹窗 -->
    <el-dialog v-model="dialogVisible" title="志愿活动详情审核" width="600px">
      <el-descriptions :column="1" border>

        <el-descriptions-item label="活动编号">{{ currentAct.actNo }}</el-descriptions-item>
        <el-descriptions-item label="活动名称">{{ currentAct.name }}</el-descriptions-item>
        <el-descriptions-item label="组织者">{{ currentAct.organizerName }}</el-descriptions-item>

        <el-descriptions-item label="开始时间">{{ currentAct.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentAct.endTime }}</el-descriptions-item>

        <el-descriptions-item label="活动地点">{{ currentAct.location }}</el-descriptions-item>
        <el-descriptions-item label="招募人数">{{ currentAct.limitNum }}</el-descriptions-item>
        <el-descriptions-item label="活动简介">{{ currentAct.desc }}</el-descriptions-item>

      </el-descriptions>

      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listActivities, auditActivity } from '../../api/activity'

const queryForm = ref({
  actName: '',
  auditStatus: '',
  timeRange: []
})

const page = ref(1)
const pageSize = ref(5)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const range = queryForm.value.timeRange || []
    const res = await listActivities({
      name: queryForm.value.actName || undefined,
      auditStatus: queryForm.value.auditStatus || undefined,
      startDate: range.length === 2 ? range[0] : undefined,
      endDate: range.length === 2 ? range[1] : undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    const rows = res.rows || []
    rows.sort((a, b) => {
      if (a.auditStatus === '待审核' && b.auditStatus !== '待审核') return -1
      if (a.auditStatus !== '待审核' && b.auditStatus === '待审核') return 1
      return 0
    })
    tableData.value = rows
    total.value = res.total || 0
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const handleSearch = () => {
  page.value = 1
  loadData()
}

const resetSearch = () => {
  queryForm.value = { actName: '', auditStatus: '', timeRange: [] }
  page.value = 1
  loadData()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  page.value = 1
  loadData()
}

const handleCurrentChange = (val) => {
  page.value = val
  loadData()
}

const dialogVisible = ref(false)
const currentAct = ref({})

const viewDetail = (row) => {
  currentAct.value = row
  dialogVisible.value = true
}

const handleAction = (row, approve) => {
  const tip = approve ? '确认批准该活动？批准后组织者即可开启前台发布。' : '确认驳回该活动？'
  ElMessageBox.confirm(tip, '审核确认', { type: approve ? 'success' : 'warning' })
    .then(async () => {
      await auditActivity(row.activityId, approve)
      ElMessage.success(approve ? '活动已批准！' : '已驳回该活动！')
      await loadData()
    })
    .catch(() => {})
}
</script>

<style scoped>
.mb-20 {
  margin-bottom: 20px;
}

.pagination-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 18px;
  flex-wrap: wrap;
}

.page-size-tip {
  font-size: 14px;
  color: #606266;
}
</style>
