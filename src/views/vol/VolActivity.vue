<template>
  <div class="app-container">

    <!-- 列表视图 -->
    <div v-if="!showDetail">
      <el-card class="mb-20">
        <el-form :inline="true">
          <el-form-item label="活动名称">
            <el-input v-model="queryName" placeholder="模糊查询" clearable />
          </el-form-item>
          <el-form-item label="活动日期">
            <el-date-picker
              v-model="queryDate"
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
      </el-card>

      <el-card>
        <el-table :data="list" border v-loading="loading">

          <el-table-column label="编号" width="80" align="center">
            <template #default="scope">
              {{ (page - 1) * pageSize + scope.$index + 1 }}
            </template>
          </el-table-column>

          <el-table-column prop="actNo" label="活动编号" width="140" align="center" />

          <el-table-column prop="name" label="活动名称" />

          <el-table-column prop="startTime" label="开始时间" width="180" align="center"/>
          <el-table-column prop="endTime" label="结束时间" width="180" align="center"/>

          <el-table-column label="报名情况" width="110" align="center">
            <template #default="scope">
              {{ scope.row.enrolledNum }} / {{ scope.row.limitNum }}
            </template>
          </el-table-column>

          <el-table-column label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="isFull(scope.row) ? 'info' : 'success'">
                {{ isFull(scope.row) ? '已满' : '可报名' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120" align="center">
            <template #default="scope">
              <el-button type="primary" link @click="goDetail(scope.row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>

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
    </div>

    <!-- 详情视图 -->
    <div v-else>
      <el-card>
        <template #header>
          <el-button icon="ArrowLeft" @click="showDetail=false">返回列表</el-button>
          <span style="margin-left: 20px; font-weight: bold;">活动详情</span>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="活动编号">{{ currentAct.actNo }}</el-descriptions-item>

          <el-descriptions-item label="活动名称">{{ currentAct.name }}</el-descriptions-item>

          <el-descriptions-item label="组织者">{{ currentAct.organizerName }}</el-descriptions-item>

          <el-descriptions-item label="报名情况">
            <strong style="color: #e63946;">{{ currentAct.enrolledNum }} / {{ currentAct.limitNum }}</strong> 人
          </el-descriptions-item>

          <el-descriptions-item label="开始时间">
            {{ currentAct.startTime }}
          </el-descriptions-item>

          <el-descriptions-item label="结束时间">
            {{ currentAct.endTime }}
          </el-descriptions-item>

          <el-descriptions-item label="活动地点">{{ currentAct.location }}</el-descriptions-item>

          <el-descriptions-item label="活动简介" :span="2">
            {{ currentAct.desc || '暂无简介' }}
          </el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 30px; text-align: center;">
          <el-button
            type="primary"
            size="large"
            style="width: 200px"
            :loading="applying"
            :disabled="isFull(currentAct)"
            @click="apply"
          >
            {{ isFull(currentAct) ? '名额已满' : '报 名' }}
          </el-button>
        </div>
      </el-card>
    </div>

  </div>
</template>

<script setup>
// 志愿者看可报名活动 + 报名。列表传 volunteerView=true，让后端只返"审核通过+发布中"的
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listActivities } from '../../api/activity'
import { register } from '../../api/registration'

const queryName = ref('')
const queryDate = ref([])

const page = ref(1)
const pageSize = ref(5)
const total = ref(0)
const list = ref([])
const loading = ref(false)
const applying = ref(false)

const isFull = (row) => row && row.limitNum != null && row.enrolledNum >= row.limitNum

const loadData = async () => {
  loading.value = true
  try {
    const range = queryDate.value || []
    const res = await listActivities({
      volunteerView: true,
      name: queryName.value || undefined,
      startDate: range.length === 2 ? range[0] : undefined,
      endDate: range.length === 2 ? range[1] : undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    const rows = res.rows || []
    // 报名人数不可超过招募人数，超出则截断
    rows.forEach(row => {
      if (row.limitNum != null && row.enrolledNum > row.limitNum) {
        row.enrolledNum = row.limitNum
      }
    })
    list.value = rows
    total.value = res.total || 0
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const handleSearch = () => { page.value = 1; loadData() }
const handleReset = () => { queryName.value = ''; queryDate.value = []; page.value = 1; loadData() }
const handleSizeChange = (val) => { pageSize.value = val; page.value = 1; loadData() }
const handleCurrentChange = (val) => { page.value = val; loadData() }

const showDetail = ref(false)
const currentAct = ref({})

const goDetail = (row) => {
  currentAct.value = row
  showDetail.value = true
}

const apply = () => {
  ElMessageBox.confirm(`确定报名【${currentAct.value.name}】吗？`, '报名确认', { type: 'info' })
    .then(async () => {
      applying.value = true
      try {
        await register(currentAct.value.activityId)
        ElMessage.success('报名申请已提交，等待组织者审核！')
        showDetail.value = false
        await loadData()
      } catch (e) {
        /* 拦截器已提示 */
      } finally {
        applying.value = false
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
