<template>
  <div class="app-container">
    <el-card class="mb-20">
      <el-form :inline="true">
        <el-form-item label="消息类型">
          <el-select v-select v-model="queryType" placeholder="请选择" clearable style="width:150px">
            <el-option label="报名通知" value="报名通知" />
            <el-option label="资质审核" value="资质审核" />
            <el-option label="系统公告" value="系统公告" />
          </el-select>
        </el-form-item>

        <el-form-item label="消息日期">
          <el-date-picker v-model="queryDate" type="daterange" start-placeholder="开始时间" end-placeholder="结束时间"
            value-format="YYYY-MM-DD" style="width: 250px" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="pagedMsgs" border>
        <el-table-column label="编号" width="70" align="center">
          <template #default="scope">
            {{ (page - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.type === '系统公告' ? 'danger' : 'info'">{{ scope.row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="正文摘要" min-width="250" show-overflow-tooltip />
        <el-table-column prop="time" label="收到时间" width="180" align="center" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="scope">
            <el-button type="primary" link @click="viewDetail(scope.row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <div class="page-size-tip">每页显示 {{ pageSize }} 条</div>
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="filteredMsgs.length"
          layout="total, prev, pager, next, jumper" background />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="消息详情" width="500px">
      <div class="msg-detail">
        <h3>{{ currentMsg.title }}</h3>
        <div class="msg-meta">
          <span>类型：{{ currentMsg.type }}</span>
          <span style="margin-left: 20px;">时间：{{ currentMsg.time }}</span>
        </div>
        <el-divider />
        <p class="msg-content">{{ currentMsg.content }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const queryType = ref('')
const queryDate = ref([])
const page = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const currentMsg = ref({})

const msgs = ref([
  {
    time: '2026-04-28 10:00:00',
    type: '系统公告',
    title: '关于五一期间志愿活动规范管理的通知',
    content: '各位志愿者，五一期间请务必按照各活动点位要求进行签到签退，注意安全防护。详细规范请点击附件查看。'
  },
  {
    time: '2026-04-27 15:30:00',
    type: '资质审核',
    title: '组织者资质审核通过',
    content: '恭喜您！您提交的申请成为组织者的资料已通过管理员审核，现在您可以切换身份发布活动了。'
  },
  {
    time: '2026-04-26 09:00:00',
    type: '报名通知',
    title: '活动报名成功提醒',
    content: '您报名的【南门指引服务】已审核通过，请准时参加。'
  }
])

const filteredMsgs = computed(() => {
  return msgs.value.filter(item => {
    const matchType = !queryType.value || item.type === queryType.value
    const itemDate = item.time.split(' ')[0]
    const matchDate = !queryDate.value || queryDate.value.length === 0 || 
                     (itemDate >= queryDate.value[0] && itemDate <= queryDate.value[1])
    return matchType && matchDate
  })
})

const pagedMsgs = computed(() => {
  return filteredMsgs.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value)
})

const handleSearch = () => { page.value = 1 }
const handleReset = () => { queryType.value = ''; queryDate.value = []; page.value = 1 }
const viewDetail = (row) => { currentMsg.value = row; detailVisible.value = true }
</script>

<style scoped>
.mb-20 { margin-bottom: 20px; }
.pagination-wrap { display: flex; justify-content: space-between; align-items: center; margin-top: 18px; }
.page-size-tip { font-size: 14px; color: #606266; }
.msg-meta { font-size: 13px; color: #909399; margin-top: 10px; }
.msg-content { line-height: 1.6; color: #606266; white-space: pre-wrap; }
</style>