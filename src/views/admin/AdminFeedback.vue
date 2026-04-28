<template>
  <div class="app-container">
    <!-- 1. 查询区域 -->
    <el-card class="mb-20">
      <el-form :inline="true">
        <el-form-item label="反馈时间">
          <el-date-picker
            v-model="queryDate"
            type="daterange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD"
            style="width: 250px"
            clearable
          />
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="queryStatus" placeholder="全 部" clearable style="width: 120px">
            <el-option label="待处理" value="待处理" />
            <el-option label="已回复" value="已回复" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 2. 表格区域 -->
    <el-card>
      <el-table :data="pagedData" border>
        <el-table-column label="编号" width="80" align="center">
          <template #default="scope">{{ (page - 1) * pageSize + scope.$index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="submitterId" label="反馈人编号" width="120" align="center" />
        <el-table-column prop="title" label="反馈标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="反馈时间" width="180" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '已回复' ? 'success' : 'danger'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理" width="120" align="center">
          <template #default="scope">
            <el-button type="primary" link icon="EditPen" @click="viewDetail(scope.row)">处理反馈</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <div class="page-size-tip">每页显示 {{ pageSize }} 条数据</div>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20]"
          :total="filteredList.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>

    <!-- 3. 详情与回复操作弹窗 -->
    <el-dialog v-model="dialogVisible" title="反馈处理中心" width="600px">
      <!-- 详情区域 -->
      <el-descriptions :column="2" border>
        <el-descriptions-item label="反馈人编号">{{ currentFeedback.submitterId }}</el-descriptions-item>
        <el-descriptions-item label="反馈时间">{{ currentFeedback.createTime }}</el-descriptions-item>
        <el-descriptions-item label="反馈标题" :span="2">{{ currentFeedback.title }}</el-descriptions-item>
        <el-descriptions-item label="反馈正文" :span="2">
          <div style="line-height: 1.6; color: #e63946;">{{ currentFeedback.content }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 回复区域 -->
      <div style="margin-top: 20px;">
        <div v-if="currentFeedback.status === '已回复'">
          <h4 style="margin-bottom: 10px;">管理员回复内容：</h4>
          <el-input type="textarea" :rows="4" disabled v-model="currentFeedback.replyContent" />
        </div>
        <div v-else>
          <h4 style="margin-bottom: 10px;">填写回复：</h4>
          <el-input type="textarea" :rows="4" placeholder="请输入回复内容，回复后用户端将直接可见..." v-model="replyText" />
          <div style="text-align: right; margin-top: 15px;">
            <el-button @click="dialogVisible = false">暂不处理</el-button>
            <el-button type="primary" @click="submitReply">提 交 回 复</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const queryDate = ref([])
const queryStatus = ref('')
const page = ref(1)
const pageSize = ref(5)
const dialogVisible = ref(false)
const currentFeedback = ref({})
const replyText = ref('') // 管理员输入的回复

// 模拟数据
const feedbackList = ref([
  { submitterId: 'VOL-20241', title: '工时认证出现延迟', createTime: '2026-04-20 10:00:00', content: '我参加的校园清扫活动已经结束三天了，工时还没有到账。', status: '待处理', replyContent: '' },
  { submitterId: 'ORG-20252', title: '建议增加活动地点导航', createTime: '2026-04-25 15:30:00', content: '有些社区活动地点不好找，希望在详情里加上地图导航链接。', status: '已回复', replyContent: '感谢您的建议，下个版本将规划该功能。' }
])

// 过滤与分页
const filteredList = computed(() => {
  return feedbackList.value.filter(item => {
    const matchStatus = !queryStatus.value || item.status === queryStatus.value
    let matchDate = true
    if (queryDate.value && queryDate.value.length === 2) {
      const itemDate = item.createTime.slice(0, 10)
      matchDate = itemDate >= queryDate.value[0] && itemDate <= queryDate.value[1]
    }
    return matchStatus && matchDate
  })
})
const pagedData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const handleSearch = () => { page.value = 1 }
const handleReset = () => { queryDate.value = []; queryStatus.value = ''; page.value = 1 }

// 打开详情，清空回复框
const viewDetail = (row) => { 
  currentFeedback.value = row
  replyText.value = ''
  dialogVisible.value = true 
}

// 提交回复
const submitReply = () => {
  if (!replyText.value) return ElMessage.warning('请输入回复内容')
  
  // 核心逻辑：修改数据源中的状态
  currentFeedback.value.status = '已回复'
  currentFeedback.value.replyContent = replyText.value
  
  ElMessage.success('回复成功！用户已可见。')
}
</script>

<style scoped>
.mb-20 { margin-bottom: 20px; }
.pagination-wrap { display: flex; justify-content: space-between; align-items: center; margin-top: 18px; }
.page-size-tip { font-size: 14px; color: #606266; }
</style>