<template>
  <div class="app-container">
    <!-- 1. 提交反馈区域 -->
    <el-card class="mb-20">
      <template #header><span style="font-weight:bold;">提交问题反馈</span></template>
      <el-form label-width="80px" style="max-width: 800px;">
        <el-form-item label="反馈标题">
          <el-input v-model="newFeedback.title" placeholder="请简述您遇到的问题" />
        </el-form-item>
        <el-form-item label="详细内容">
          <el-input v-model="newFeedback.content" type="textarea" rows="4" placeholder="请详细描述问题，以便管理员尽快核实处理..." />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Promotion" @click="handleSubmit">提 交 反 馈</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 2. 历史反馈记录区域 -->
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-weight:bold;">我的反馈记录</span>
          <!-- 时间范围查询 -->
          <el-form :inline="true" style="margin-bottom: -18px;">
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
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </template>

      <!-- 表格 -->
      <el-table :data="pagedData" border>
        <el-table-column label="编号" width="80" align="center">
          <template #default="scope">
            {{ (page - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="title" label="反馈标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="反馈时间" width="180" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '已回复' ? 'success' : 'warning'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <el-button type="primary" link icon="View" @click="viewDetail(scope.row)">详情</el-button>
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

    <!-- 3. 详情与回复弹窗 -->
    <el-dialog v-model="dialogVisible" title="反馈详情" width="550px">
      <h3 style="margin-top: 0;">{{ currentFeedback.title }}</h3>
      <p style="color: #999; font-size: 13px;">反馈时间：{{ currentFeedback.createTime }}</p>
      <div style="background: #f5f7fa; padding: 15px; border-radius: 4px; color: #606266; line-height: 1.6;">
        {{ currentFeedback.content }}
      </div>

      <el-divider border-style="dashed" />

      <!-- 回复区域 -->
      <div v-if="currentFeedback.status === '已回复'">
        <div style="color: #67C23A; font-weight: bold; margin-bottom: 10px;">
          <el-icon><Check /></el-icon> 管理员回复：
        </div>
        <div style="background: #f0f9eb; padding: 15px; border-radius: 4px; color: #67C23A; line-height: 1.6;">
          {{ currentFeedback.replyContent }}
        </div>
      </div>
      <div v-else style="text-align: center; color: #909399; padding: 20px 0;">
        <el-icon size="24"><Clock /></el-icon>
        <p>管理员正在快马加鞭处理中，请耐心等待...</p>
      </div>

      <template #footer><el-button @click="dialogVisible = false">关 闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const newFeedback = ref({ title: '', content: '' })
const queryDate = ref([])
const page = ref(1)
const pageSize = ref(5)
const dialogVisible = ref(false)
const currentFeedback = ref({})

const feedbackList = ref([
  { title: '工时认证出现延迟', createTime: '2026-04-20 10:00:00', content: '我参加的校园清扫活动已经结束三天了，工时还没有到账。', status: '已回复', replyContent: '您好，系统数据已同步，请重新登录查看，为您带来的不便敬请谅解。' },
  { title: '建议增加活动地点导航', createTime: '2026-04-25 15:30:00', content: '有些社区活动地点不好找，希望在详情里加上地图导航链接。', status: '待处理', replyContent: '' }
])

// 提交反馈
const handleSubmit = () => {
  if (!newFeedback.value.title || !newFeedback.value.content) return ElMessage.warning('请填写完整反馈信息')
  ElMessage.success('反馈提交成功！')
  newFeedback.value = { title: '', content: '' } // 清空表单
}

// 过滤与分页
const filteredList = computed(() => {
  return feedbackList.value.filter(item => {
    if (!queryDate.value || queryDate.value.length !== 2) return true
    const itemDate = item.createTime.slice(0, 10)
    return itemDate >= queryDate.value[0] && itemDate <= queryDate.value[1]
  })
})
const pagedData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

const handleSearch = () => { page.value = 1 }
const handleReset = () => { queryDate.value = []; page.value = 1 }

const viewDetail = (row) => { currentFeedback.value = row; dialogVisible.value = true }
</script>

<style scoped>
.mb-20 { margin-bottom: 20px; }
.pagination-wrap { display: flex; justify-content: space-between; align-items: center; margin-top: 18px; }
.page-size-tip { font-size: 14px; color: #606266; }
</style>