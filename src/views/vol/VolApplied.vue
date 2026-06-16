<template>
  <div class="app-container">

    <!-- 列表 -->
    <div v-if="!showDetail">
      <el-card class="mb-20">
        <el-form :inline="true">
          <el-form-item label="活动名称">
            <el-input v-model="queryName" placeholder="模糊查询" clearable />
          </el-form-item>
          <el-form-item label="审核状态">
            <el-select v-model="queryStatus" placeholder="全部" clearable style="width: 140px">
              <el-option label="待审核" value="待审核" />
              <el-option label="审核通过" value="审核通过" />
              <el-option label="审核拒绝" value="审核拒绝" />
              <el-option label="已取消" value="已取消" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card>
        <el-table :data="pagedList" border v-loading="loading">

          <el-table-column label="编号" width="80" align="center">
            <template #default="scope">
              {{ (page - 1) * pageSize + scope.$index + 1 }}
            </template>
          </el-table-column>

          <el-table-column prop="actNo" label="活动编号" width="140" align="center" />

          <el-table-column prop="activityName" label="活动名称" />

          <el-table-column prop="startTime" label="开始时间" width="180" align="center"/>
          <el-table-column prop="endTime" label="结束时间" width="180" align="center"/>

          <el-table-column prop="auditStatus" label="审核状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="statusType(scope.row.auditStatus)">
                {{ scope.row.auditStatus }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="200" align="center">
            <template #default="scope">
              <el-button type="primary" link @click="goDetail(scope.row)">查看活动详情</el-button>
              <el-button type="danger" link v-if="scope.row.auditStatus==='待审核'" @click="handleCancel(scope.row)">取消报名</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <div class="page-size-tip">共 {{ filteredList.length }} 条</div>
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

    <!-- 详情 -->
    <div v-else>
      <el-card>
        <template #header>
          <el-button icon="ArrowLeft" @click="showDetail=false">返回列表</el-button>
          <span style="margin-left:15px; font-weight:bold;">已报名活动详细信息</span>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="活动编号">{{ currentAct.actNo }}</el-descriptions-item>
          <el-descriptions-item label="活动名称">{{ currentAct.activityName }}</el-descriptions-item>
          <el-descriptions-item label="活动地点">{{ currentAct.location }}</el-descriptions-item>

          <el-descriptions-item label="报名情况">
            <strong style="color: #e63946;">{{ currentAct.enrolledNum }} / {{ currentAct.limitNum }}</strong></el-descriptions-item>

          <el-descriptions-item label="开始时间">{{ currentAct.startTime }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ currentAct.endTime }}</el-descriptions-item>
          <el-descriptions-item label="签到时间">{{ currentAct.checkInTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="签退时间">{{ currentAct.checkOutTime || '-' }}</el-descriptions-item>

          <el-descriptions-item label="活动简介" :span="2">
            {{ currentAct.desc }}
          </el-descriptions-item>

          <el-descriptions-item label="当前报名状态">
            <el-tag :type="statusType(currentAct.auditStatus)">
              {{ currentAct.auditStatus }}
            </el-tag>
          </el-descriptions-item>

          <el-descriptions-item label="认证志愿时长">
            <strong style="color:#e63946">
              {{ currentAct.hours || 0 }} 小时 {{ currentAct.minutes || 0 }} 分钟
            </strong>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myRegistrations, cancelRegistration } from '../../api/registration'

const queryName = ref('')
const queryStatus = ref('')

const page = ref(1)
const pageSize = ref(5)
const list = ref([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await myRegistrations({ page: 1, pageSize: 500 })
    list.value = res.rows || []
  } finally {
    loading.value = false
  }
}
onMounted(loadData)

const statusType = (s) => {
  if (s === '审核通过') return 'success'
  if (s === '审核拒绝') return 'danger'
  if (s === '已取消') return 'info'
  return 'warning'
}

const filteredList = computed(() => {
  return list.value.filter(item => {
    const matchName = !queryName.value || (item.activityName || '').includes(queryName.value)
    const matchStatus = !queryStatus.value || item.auditStatus === queryStatus.value
    return matchName && matchStatus
  })
})

const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const handleSearch = () => { page.value = 1 }
const handleReset = () => { queryName.value = ''; queryStatus.value = ''; page.value = 1 }

const showDetail = ref(false)
const currentAct = ref({})
const goDetail = (row) => { currentAct.value = row; showDetail.value = true }

const handleCancel = (row) => {
  ElMessageBox.confirm(`确定取消报名【${row.activityName}】吗？`, '取消报名', { type: 'warning' })
    .then(async () => {
      await cancelRegistration(row.regId)
      ElMessage.success('已取消报名')
      await loadData()
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
