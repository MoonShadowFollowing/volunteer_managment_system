<template>
  <div class="app-container">
    <el-card class="mb-20">
      <template #header><span style="font-weight:bold;">发布新公告</span></template>
      <el-form label-width="100px" style="max-width: 800px;">
        <el-form-item label="公告标题">
          <el-input v-model="newNotice.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="通知范围">
          <el-checkbox-group v-model="newNotice.targets">
            <el-checkbox label="全体志愿者" />
            <el-checkbox label="全体组织者" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="公告正文">
          <el-input v-model="newNotice.content" type="textarea" rows="4" placeholder="请输入通知正文..." />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Promotion" @click="handleSend">发 送 公 告</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header><span style="font-weight:bold;">历史公告记录</span></template>
      <el-table :data="pagedData" border>
        <el-table-column label="编号" width="80" align="center">
          <template #default="scope">
            {{ (page - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="title" label="公告标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="targets" label="通知对象" width="180">
          <template #default="scope">
            <el-tag v-for="tag in scope.row.targets" :key="tag" size="small" style="margin-right: 5px">
              {{ tag }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="180" align="center" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <el-button type="primary" link icon="View" @click="viewNotice(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <div class="page-size-tip">每页显示 {{ pageSize }} 条数据</div>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="noticeList.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="公告详情" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">{{ currentNotice.title }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ currentNotice.createTime }}</el-descriptions-item>
        <el-descriptions-item label="正文">{{ currentNotice.content }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const page = ref(1)
const pageSize = ref(5)
const dialogVisible = ref(false)
const currentNotice = ref({})

const newNotice = ref({
  title: '',
  targets: ['全体志愿者', '全体组织者'],
  content: ''
})

const noticeList = ref([
  { title: '关于五一假期志愿活动的通知', targets: ['全体志愿者'], createTime: '2026-04-20 10:00:00', content: '请各位志愿者注意假期安全。' },
  { title: '组织者后台系统升级公告', targets: ['全体组织者'], createTime: '2026-04-18 15:30:00', content: '系统将于今晚凌晨进行维护。' }
])

const pagedData = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return noticeList.value.slice(start, start + pageSize.value)
})

const handleSend = () => {
  if (!newNotice.value.title || !newNotice.value.content) return ElMessage.warning('请填写完整信息')
  ElMessage.success('公告已推送至消息中心！')
}

const viewNotice = (row) => {
  currentNotice.value = row
  dialogVisible.value = true
}
</script>

<style scoped>
.mb-20 { margin-bottom: 20px; }
.pagination-wrap { display: flex; justify-content: space-between; align-items: center; margin-top: 18px; }
.page-size-tip { font-size: 14px; color: #606266; }
</style>