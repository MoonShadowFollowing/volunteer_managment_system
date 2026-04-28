<template>
  <div class="app-container">

    <!-- 查询 -->
    <el-card class="mb-20">
      <el-form :inline="true">
        <el-form-item label="活动编号">
          <el-input placeholder="查询活动编号" clearable />
        </el-form-item>

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

    <!-- 表格 -->
    <el-card>
      <el-table :data="pagedCertList" border>

        <!-- 编号 -->
        <el-table-column label="编号" width="80" align="center">
          <template #default="scope">
            {{ (page - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>

        <el-table-column prop="certName" label="证书标题" min-width="180" />
        <el-table-column prop="activityName" label="对应活动" min-width="160" />
        <el-table-column prop="actNo" label="对应活动编号" width="140" align="center" />

        <!-- 时间 -->
        <el-table-column prop="startTime" label="开始时间" width="180" align="center"/>
        <el-table-column prop="endTime" label="结束时间" width="180" align="center"/>

        <!-- 工时结构化 -->
        <el-table-column label="认证工时" width="160" align="center">
          <template #default="scope">
            <strong style="color: #e63946">
              {{ scope.row.hours }} 小时 {{ scope.row.minutes }} 分钟
            </strong>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="证书状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '已失效' ? 'danger' : 'success'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <el-button
              size="small"
              type="primary"
              link
              :disabled="scope.row.status === '已失效'"
              @click="previewCert(scope.row)"
            >
              预览
            </el-button>

            <el-button
              size="small"
              type="primary"
              link
              :disabled="scope.row.status === '已失效'"
              @click="downloadCert"
            >
              下载
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
          :total="filteredCertList.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>

    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const queryName = ref('')
const page = ref(1)
const pageSize = ref(5)

/* hours + minutes 结构 */
const certList = ref([
  {
    certName: '优秀青年志愿者证明',
    activityName: '校园环境清扫',
    actNo: '202605001001',
    startTime: '2026-05-01 08:00:00',
    endTime: '2026-05-01 11:00:00',
    hours: 4,
    minutes: 0,
    status: '有效'
  },
  {
    certName: '马拉松后勤服务证明',
    activityName: '市马拉松后勤',
    actNo: '202606100001',
    startTime: '2026-06-10 05:30:00',
    endTime: '2026-06-10 12:00:00',
    hours: 0,
    minutes: 0,
    status: '已失效'
  },
  {
    certName: '防诈骗宣讲先锋',
    activityName: '社区防诈骗宣讲',
    actNo: '202605100002',
    startTime: '2026-05-10 14:00:00',
    endTime: '2026-05-10 16:30:00',
    hours: 2,
    minutes: 30,
    status: '有效'
  }
])

const filteredCertList = computed(() => {
  return certList.value.filter(item => {
    return !queryName.value || item.activityName.includes(queryName.value)
  })
})

const pagedCertList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredCertList.value.slice(start, start + pageSize.value)
})

const handleSearch = () => {
  page.value = 1
}

const handleReset = () => {
  queryName.value = ''
  page.value = 1
}

const previewCert = (row) => {
  ElMessage.success(`正在预览: ${row.certName}`)
}

const downloadCert = () => {
  ElMessage.success('证书已开始下载！')
}
</script>

<style scoped>
.mb-20 { margin-bottom: 20px; }

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