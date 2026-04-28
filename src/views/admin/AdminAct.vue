<template>
  <div class="app-container">

    <!-- 查询栏 -->
    <el-card class="mb-20">
      <el-form :inline="true">

        <el-form-item label="活动编号">
          <el-input
            placeholder="模糊查询活动编号"
            style="width: 150px"
            clearable
          />
        </el-form-item>

        <el-form-item label="活动名称">
          <el-input
            v-model="queryForm.actName"
            placeholder="模糊查询活动名称"
            style="width: 150px"
            clearable
          />
        </el-form-item>

        <el-form-item label="发布组织">
          <el-input
            v-model="queryForm.actName"
            placeholder="模糊查询发布组织"
            style="width: 150px"
            clearable
          />
        </el-form-item>

        <el-form-item label="活动时间">
          <el-date-picker
            v-model="queryForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 300px"
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
      <el-table :data="pagedData" border>

        <!-- 编号 -->
        <el-table-column label="编号" width="80" align="center">
          <template #default="scope">
            {{ (page - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="actNo" label="活动编号" width="140" align="center" />
        <el-table-column prop="actName" label="活动名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="org" label="发布组织" width="160" />

        <!-- 时间双列结构（规范保留） -->
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

        <el-table-column prop="status" label="审核状态" width="130" align="center">
          <template #default="scope">
            <el-tag
              :type="
                scope.row.status === '已发布'
                  ? 'success'
                  : scope.row.status === '已驳回'
                  ? 'danger'
                  : 'warning'
              "
            >
              {{ scope.row.status }}
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
              v-if="scope.row.status === '待审核'"
              type="success"
              size="small"
              @click="handleAction(scope.row, 'approve')"
            >
              批准
            </el-button>

            <el-button
              v-if="scope.row.status === '待审核'"
              type="danger"
              size="small"
              @click="handleAction(scope.row, 'reject')"
            >
              驳回
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
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>

    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="dialogVisible" title="志愿活动详情审核" width="600px">
      <el-descriptions :column="1" border>

        <el-descriptions-item label="活动编号">{{ currentAct.actNo }}</el-descriptions-item>
        <el-descriptions-item label="活动名称">{{ currentAct.actName }}</el-descriptions-item>
        <el-descriptions-item label="发布组织">{{ currentAct.org }}</el-descriptions-item>

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
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

/* 查询 */
const queryForm = ref({
  actName: '',
  timeRange: []
})

/* 分页 */
const page = ref(1)
const pageSize = ref(5)

/* 数据 */
const actList = ref([
  {
    actNo: '202605200001',
    actName: '南门指引服务',
    org: '青年志愿者协会',
    startTime: '2026-05-20 08:00:00',
    endTime: '2026-05-20 12:00:00',
    location: '学校南门',
    limitNum: 15,
    desc: '在校门指引外来人员，维护交通秩序。',
    status: '待审核'
  },
  {
    actNo: '202605200002',
    actName: '校园清扫行动',
    org: '环境保护协会',
    startTime: '2026-05-21 09:00:00',
    endTime: '2026-05-21 11:30:00',
    location: '全校范围',
    limitNum: 10,
    desc: '校园卫生清洁志愿活动。',
    status: '已驳回'
  }
])

/* 过滤 */
const filteredList = computed(() => {
  return actList.value.filter(item => {
    const nameMatch =
      !queryForm.value.actName ||
      item.actName.includes(queryForm.value.actName)

    let timeMatch = true
    if (queryForm.value.timeRange?.length === 2) {
      const start = new Date(queryForm.value.timeRange[0]).getTime()
      const end = new Date(queryForm.value.timeRange[1]).getTime()
      const actTime = new Date(item.startTime).getTime()
      timeMatch = actTime >= start && actTime <= end
    }

    return nameMatch && timeMatch
  })
})

/* 分页数据 */
const pagedData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const handleSearch = () => {
  page.value = 1
  ElMessage.success('查询完成')
}

const resetSearch = () => {
  queryForm.value = {
    actName: '',
    timeRange: []
  }
  page.value = 1
}

/* 分页事件 */
const handleSizeChange = (val) => {
  pageSize.value = val
  page.value = 1
}

const handleCurrentChange = (val) => {
  page.value = val
}

/* 详情 */
const dialogVisible = ref(false)
const currentAct = ref({})

const viewDetail = (row) => {
  currentAct.value = row
  dialogVisible.value = true
}

/* 审核逻辑 */
const handleAction = (row, type) => {
  if (type === 'reject') {
    row.status = '已驳回'
    ElMessage.warning('已驳回该活动！')
  } else {
    ElMessageBox.confirm(
      '确认批准该活动上线？批准后状态自动变为已发布。',
      '确认操作',
      { type: 'success' }
    )
      .then(() => {
        row.status = '已发布'
        ElMessage.success('活动已批准上线！通知已送达。')
      })
      .catch(() => {})
  }
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