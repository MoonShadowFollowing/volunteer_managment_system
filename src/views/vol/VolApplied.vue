<template>
  <div class="app-container">

    <!-- 列表 -->
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

          <el-table-column prop="auditStatus" label="审核状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.auditStatus==='已通过'?'success':'warning'">
                {{scope.row.auditStatus}}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="200" align="center">
            <template #default="scope">
              <el-button type="primary" link @click="goDetail(scope.row)">查看活动详情</el-button>
              <el-button type="danger" link v-if="scope.row.auditStatus==='待审核'">取消报名</el-button>
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
    
    <!-- 详情 -->
    <div v-else>
      <el-card>
        <template #header>
          <el-button icon="ArrowLeft" @click="showDetail=false">返回列表</el-button>
          <span style="margin-left:15px; font-weight:bold;">已报名活动详细信息</span>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="活动编号">{{ currentAct.actNo }}</el-descriptions-item>
          <el-descriptions-item label="活动名称">{{ currentAct.name }}</el-descriptions-item>
          <el-descriptions-item label="活动地点">{{ currentAct.location }}</el-descriptions-item>

          <el-descriptions-item label="开始时间">{{ currentAct.startTime }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ currentAct.endTime }}</el-descriptions-item>

          <el-descriptions-item label="报名情况">
            <strong style="color: #e63946;">{{ currentAct.enrolledNum }} / {{ currentAct.limitNum }}</strong> 人
          </el-descriptions-item>

          <el-descriptions-item label="活动简介" :span="2">
            {{ currentAct.desc }}
          </el-descriptions-item>

          <el-descriptions-item label="当前报名状态">
            <el-tag :type="currentAct.auditStatus==='已通过'?'success':'warning'">
              {{ currentAct.auditStatus }}
            </el-tag>
          </el-descriptions-item>

          <el-descriptions-item label="认证志愿时长">
            <strong style="color:#e63946">
              {{ currentAct.hours }} 小时 {{ currentAct.minutes }} 分钟
            </strong>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

/* 查询 */
const queryStatus = ref('')

/* 分页 */
const page = ref(1)
const pageSize = ref(5)

/* 数据（补全时间格式） */
const list = ref([
  {
    actNo: '202605010001',
    name: '校园清扫',
    auditStatus: '已通过',
    location: '中心操场',
    startTime: '2026-05-01 08:00:00',
    endTime: '2026-05-01 12:00:00',
    limitNum: 10,
    enrolledNum: 9,
    desc: '校园内部大扫除活动。',
    hours: 4,
    minutes: 0
  },
  {
    actNo: '202605100002',
    name: '防诈骗宣传',
    auditStatus: '待审核',
    location: '阳光社区',
    startTime: '2026-05-10 14:00:00',
    endTime: '2026-05-10 16:30:00',
    limitNum: 10,
    enrolledNum: 10,
    desc: '进入社区进行反诈骗宣讲。',
    hours: 0,
    minutes: 0
  },
  {
    actNo: '202605200001',
    name: '社区敬老服务',
    auditStatus: '已通过',
    location: '幸福养老院',
    startTime: '2026-05-20 09:00:00',
    endTime: '2026-05-20 11:30:00',
    limitNum: 10,
    enrolledNum: 10,
    desc: '陪伴老人聊天互动。',
    hours: 2,
    minutes: 30
  }
])

/* 过滤 */
const filteredList = computed(()=>{
  return list.value.filter(item=>{
    return !queryStatus.value || item.auditStatus === queryStatus.value
  })
})

/* 分页 */
const pagedList = computed(()=>{
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const handleSearch = ()=>{ page.value = 1 }

const handleReset = ()=>{
  queryStatus.value = ''
  page.value = 1
}

/* 原逻辑 */
const showDetail = ref(false)
const currentAct = ref({})

const goDetail = (row) => {
  currentAct.value = row
  showDetail.value = true
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