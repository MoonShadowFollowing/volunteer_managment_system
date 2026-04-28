<template>
  <div class="app-container">

    <!-- 列表视图 -->
    <div v-if="!showDetail">
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

      <el-card>
        <el-table :data="pagedList" border>

          <!-- 编号 -->
          <el-table-column label="编号" width="80" align="center">
            <template #default="scope">
              {{ (page - 1) * pageSize + scope.$index + 1 }}
            </template>
          </el-table-column>

          <el-table-column prop="actNo" label="活动编号" width="140" align="center" />

          <el-table-column prop="name" label="活动名称" />

          <!-- 时间拆分 -->
          <el-table-column prop="startTime" label="开始时间" width="180" align="center"/>
          <el-table-column prop="endTime" label="结束时间" width="180" align="center"/>
                    
          <el-table-column prop="limitNum" label="招募人数" width="90" align="center" />

          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.status==='可报名'?'success':'info'">
                {{scope.row.status}}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120" align="center">
            <template #default="scope">
              <el-button type="primary" link @click="goDetail(scope.row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页（统一标准） -->
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

          <el-descriptions-item label="活动状态">
            <el-tag type="success">{{ currentAct.status }}</el-tag>
          </el-descriptions-item>

          <el-descriptions-item label="报名情况">
            <strong style="color: #e63946;">{{ currentAct.enrolledNum }} / {{ currentAct.limitNum }}</strong> 人
          </el-descriptions-item>

          <!-- 时间拆分（保持统一规范） -->
          <el-descriptions-item label="开始时间">
            {{ currentAct.startTime }}
          </el-descriptions-item>

          <el-descriptions-item label="结束时间">
            {{ currentAct.endTime }}
          </el-descriptions-item>

          <el-descriptions-item label="活动地点">中心广场</el-descriptions-item>

          <el-descriptions-item label="活动简介" :span="2">
            这是一场非常有意义的校园清洁志愿服务活动。
          </el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 30px; text-align: center;">
          <el-button type="primary" size="large" style="width: 200px" @click="apply">
            报 名
          </el-button>
        </div>
      </el-card>
    </div>

  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

/* 查询 */
const queryName = ref('')

/* 分页 */
const page = ref(1)
const pageSize = ref(5)

/* 数据（补全+覆盖情况） */
const list = ref([
  {
    actNo: '202605010001',
    name: '校园清扫',
    startTime: '2026-05-01 08:00:00',
    endTime: '2026-05-01 11:00:00',
    status: '可报名',
    limitNum: 15,
    enrolledNum: 8
  },
  {
    actNo: '202605100001',
    name: '社区敬老服务',
    startTime: '2026-05-10 09:00:00',
    endTime: '2026-05-10 12:00:00',
    status: '已结束',
    limitNum: 10,
    enrolledNum: 10
  }
])

/* 查询过滤 */
const filteredList = computed(()=>{
  return list.value.filter(item =>
    !queryName.value || item.name.includes(queryName.value)
  )
})

/* 分页数据 */
const pagedList = computed(()=>{
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const handleSearch = ()=>{ page.value = 1 }

const handleReset = ()=>{
  queryName.value = ''
  page.value = 1
}

/* 原逻辑 */
const showDetail = ref(false)
const currentAct = ref({})

const goDetail = (row) => {
  currentAct.value = row
  showDetail.value = true
}

const apply = () => {
  ElMessage.success('报名申请已提交！')
  showDetail.value = false
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