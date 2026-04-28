<template>
  <div class="app-container">
    <el-card class="mb-20">
      <el-form :inline="true">
        <el-form-item label="消息类型">
          <el-select v-model="queryType" placeholder="请选择" clearable style="width:150px">
            <el-option label="活动通知" value="活动通知" />
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
            <el-tag :type="scope.row.type === '系统公告' ? 'danger' : 'success'">{{ scope.row.type }}</el-tag>
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
        <h3 style="margin-top: 0;">{{ currentMsg.title }}</h3>
        <div style="font-size: 13px; color: #909399; margin-bottom: 15px;">
          <span>类型：{{ currentMsg.type }}</span>
          <span style="margin-left: 20px;">时间：{{ currentMsg.time }}</span>
        </div>
        <el-divider />
        <div style="line-height: 1.6; color: #606266; white-space: pre-wrap;">{{ currentMsg.content }}</div>
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
    time: '2026-04-01 10:00:00',
    type: '活动通知',
    title: '您的志愿活动已通过审核',
    content: '您提交的【图书馆整理服务】已通过管理员审核，活动已正式发布至客户端，请及时关注报名人数。'
  },
  {
    time: '2026-04-02 09:30:00',
    type: '系统公告',
    title: '系统维护通知',
    content: '平台将于本周六凌晨进行系统升级维护，届时组织者后台将暂时无法访问，请提前做好工作安排。'
  }
])

const filteredMsgs = computed(() => {
  return msgs.value.filter(item => {
    const matchType = !queryType.value || item.type === queryType.value
    const itemDate = item.time?.split(' ')[0]
    const matchDate = !queryDate.value || queryDate.value.length !== 2 ||
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
</style>