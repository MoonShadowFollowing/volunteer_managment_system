<template>
  <div class="app-container">
    <!-- 1. 活动列表主界面 -->
    <div v-if="!currentActivity">
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
        <el-table :data="pagedActivities" border>

          <!-- 编号 -->
          <el-table-column label="编号" width="80" align="center">
            <template #default="scope">
              {{ (activityPage - 1) * activityPageSize + scope.$index + 1 }}
            </template>
          </el-table-column>

          <el-table-column prop="actNo" label="活动编号" width="140" align="center" />

          <el-table-column prop="name" label="活动名称" />

          <!-- 时间拆分 -->
          <el-table-column prop="startTime" label="开始时间" width="180" align="center"/>
          <el-table-column prop="endTime" label="结束时间" width="180" align="center"/>

          <el-table-column prop="limitNum" label="招募人数" width="90" align="center" />

          <el-table-column prop="status" label="活动状态" width="120" align="center">
            <template #default>
              <el-tag type="success">已发布</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="150" align="center">
            <template #default="scope">
              <el-button type="primary" @click="enterManage(scope.row)">进入管理</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrap">
          <div>每页显示 {{ activityPageSize }} 条</div>
          <el-pagination
            v-model:current-page="activityPage"
            v-model:page-size="activityPageSize"
            :page-sizes="[5, 10, 20, 50]"
            :total="filteredActivities.length"
            layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 2. 具体活动的管理详情页 -->
    <div v-else>
      <el-card class="mb-20">
        <template #header>
          <el-button icon="ArrowLeft" @click="currentActivity=null" style="margin-right: 15px">返回活动列表</el-button>
          <strong style="font-size: 16px;">当前管理活动：{{ currentActivity.name }}</strong>
        </template>
        
        <el-tabs v-model="activeTab">

          <!-- 报名审核 -->
          <el-tab-pane label="报名审核" name="audit">

            <div>
              <el-form :inline="true">
                <el-form-item label="志愿者编号">
                  <el-input v-model="queryVolIdHours" placeholder="志愿者编号" clearable />
                </el-form-item>
                
                <el-form-item label="志愿者姓名">
                  <el-input v-model="queryVolNameHours" placeholder="志愿者姓名" clearable />
                </el-form-item>
                
                <el-form-item>
                  <el-button type="primary" @click="handleSearchHours">查询</el-button>
                  <el-button @click="handleResetHours">重置</el-button>
                </el-form-item>
              </el-form>
            </div>

            <el-table :data="pagedVolunteers" border>
              <!-- 编号 -->
              <el-table-column label="编号" width="80" align="center">
                <template #default="scope">
                  {{ (volPage - 1) * volPageSize + scope.$index + 1 }}
                </template>
              </el-table-column>

              <el-table-column prop="volId" label="志愿者编号" width="200" align="center" />

              <el-table-column prop="name" label="志愿者姓名" />

              <el-table-column prop="status" label="审核状态">
                <template #default="scope">
                  <el-tag :type="scope.row.status==='待审核'?'warning':(scope.row.status==='通过'?'success':'danger')">
                    {{scope.row.status}}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="操作" width="200" align="center">
                <template #default="scope">
                  <el-button v-if="scope.row.status==='待审核'" type="success" size="small" @click="handleAudit(scope.row, '通过')">通过</el-button>
                  <el-button v-if="scope.row.status==='待审核'" type="danger" size="small" @click="handleAudit(scope.row, '拒绝')">拒绝</el-button>
                  <span v-else style="color:#999; font-size: 12px;">已处理</span>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="pagination-wrap">
              <div>每页显示 {{ volPageSize }} 条</div>
              <el-pagination
                v-model:current-page="volPage"
                v-model:page-size="volPageSize"
                :page-sizes="[5,10,20]"
                :total="sortedVolunteers.length"
                layout="total, sizes, prev, pager, next, jumper"
                background
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </el-tab-pane>

          <!-- 志愿时 -->
          <el-tab-pane label="志愿时管理" name="hours">

            <div>
              <el-form :inline="true">
                <el-form-item label="志愿者编号">
                  <el-input v-model="queryVolIdHours" placeholder="志愿者编号" clearable />
                </el-form-item>
                
                <el-form-item label="志愿者姓名">
                  <el-input v-model="queryVolNameHours" placeholder="志愿者姓名" clearable />
                </el-form-item>
                
                <el-form-item>
                  <el-button type="primary" @click="handleSearchHours">查询</el-button>
                  <el-button @click="handleResetHours">重置</el-button>
                </el-form-item>
              </el-form>
            </div>

            <el-table :data="pagedSignRecords" border>
              <!-- 编号 -->
              <el-table-column label="编号" width="80" align="center">
                <template #default="scope">
                  {{ (signPage - 1) * signPageSize + scope.$index + 1 }}
                </template>
              </el-table-column>
              
              <el-table-column prop="volId" label="志愿者编号" width="200" align="center" />

              <el-table-column prop="name" label="志愿者姓名" />

              <el-table-column prop="signStatus" label="签到状态">
                <template #default="scope">
                  <el-tag :type="scope.row.signStatus==='正常'?'success':'danger'">
                    {{scope.row.signStatus}}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="志愿时长" align="center">
                <template #default="scope">
                  <span v-if="scope.row.signStatus==='正常'">
                    {{ scope.row.hours }} 小时 {{ scope.row.minutes }} 分钟
                  </span>
                  <span v-else style="color:#e63946">异常，暂无工时</span>
                </template>
              </el-table-column>

              <el-table-column label="操作" width="200" align="center">
                <template #default="scope">
                  <el-button v-if="scope.row.signStatus!=='正常'" type="warning" size="small" @click="handleSignFix(scope.row)">手动补签</el-button>
                  <el-button v-else type="primary" size="small" @click="openHourDialog(scope.row)">修改志愿时</el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="pagination-wrap">
              <div>每页显示 {{ signPageSize }} 条</div>
              <el-pagination
                v-model:current-page="signPage"
                v-model:page-size="signPageSize"
                :page-sizes="[5,10,20]"
                :total="signRecords.length"
                layout="total, sizes, prev, pager, next, jumper"
                background
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
              />
            </div>
          </el-tab-pane>

        </el-tabs>
      </el-card>
    </div>

    <!-- 弹窗 -->
    <el-dialog v-model="hourDialogVisible" title="修改志愿时" width="400px">
      <p style="color: #666; margin-bottom: 20px;">
        提示：当修改为 0小时0分钟 时，该志愿者的证书将自动失效！
      </p>
      <el-form label-width="80px">
        <el-form-item label="小时">
          <el-input-number v-model="tempHours.h" :min="0" />
        </el-form-item>
        <el-form-item label="分钟">
          <el-input-number v-model="tempHours.m" :min="0" :max="59" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="hourDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmHourChange">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

/* 查询 */
const queryName = ref('')
const queryDate = ref([])

/* 分页状态 */
const activityPage = ref(1)
const activityPageSize = ref(5)

const volPage = ref(1)
const volPageSize = ref(5)

const signPage = ref(1)
const signPageSize = ref(5)

/* 活动数据 */
const activities = ref([
  { actNo: '202605010001',name: '南校区清扫活动', startTime: '2026-05-01 08:00:00', endTime: '2026-05-01 11:00:00', limitNum: 10 },
  { actNo: '202606100001',name: '马拉松后勤支援', startTime: '2026-06-10 05:30:00', endTime: '2026-06-10 12:00:00', limitNum: 15 }
])

const filteredActivities = computed(()=>{
  return activities.value.filter(a=>{
    const matchName = !queryName.value || a.name.includes(queryName.value)

    let matchDate = true
    if(queryDate.value && queryDate.value.length===2){
      const d = a.startTime.slice(0,10)
      matchDate = d >= queryDate.value[0] && d <= queryDate.value[1]
    }

    return matchName && matchDate
  })
})

const pagedActivities = computed(()=>{
  const start=(activityPage.value-1)*activityPageSize.value
  return filteredActivities.value.slice(start,start+activityPageSize.value)
})

const handleSearch = ()=>{ activityPage.value=1 }

const handleReset = ()=>{
  queryName.value=''
  queryDate.value=[]
  activityPage.value=1
}

const currentActivity = ref(null)
const activeTab = ref('audit')
const enterManage = (row) => { currentActivity.value = row; activeTab.value = 'audit' }

const volunteers = ref([
  { id: 1, volId: 'VOL-12345', name: '张三', status: '待审核' },
  { id: 2, volId: 'VOL-12545', name: '李四', status: '通过' },
  { id: 3, volId: 'VOL-14345', name: '王五', status: '拒绝' }
])

const sortedVolunteers = computed(() => {
  return [...volunteers.value].sort((a, b) => {
    if (a.status === '待审核' && b.status !== '待审核') return -1;
    if (a.status !== '待审核' && b.status === '待审核') return 1;
    return 0;
  })
})

const pagedVolunteers = computed(()=>{
  const start=(volPage.value-1)*volPageSize.value
  return sortedVolunteers.value.slice(start,start+volPageSize.value)
})

const handleAudit = (row, res) => { row.status = res; ElMessage.success(`已${res}！`) }

const signRecords = ref([
  { id: 1, volId: 'VOL-22345', name: '李四', signStatus: '正常', hours: 4, minutes: 0 },
  { id: 2, volId: 'VOL-00345', name: '赵六', signStatus: '漏签退', hours: 0, minutes: 0 },
  { id: 3, volId: 'VOL-21345', name: '孙七', signStatus: '正常', hours: 2, minutes: 30 }
])

const pagedSignRecords = computed(()=>{
  const start=(signPage.value-1)*signPageSize.value
  return signRecords.value.slice(start,start+signPageSize.value)
})

const handleSignFix = (row) => {
  row.signStatus = '正常'
  row.hours = 4
  ElMessage.success('补签成功，状态已恢复正常')
}

const hourDialogVisible = ref(false)
const targetVolunteer = ref(null)
const tempHours = ref({ h: 0, m: 0 })

const openHourDialog = (row) => {
  targetVolunteer.value = row
  tempHours.value.h = row.hours
  tempHours.value.m = row.minutes
  hourDialogVisible.value = true
}

const confirmHourChange = () => {
  targetVolunteer.value.hours = tempHours.value.h
  targetVolunteer.value.minutes = tempHours.value.m
  if(tempHours.value.h === 0 && tempHours.value.m === 0) {
    ElMessage.warning('工时已修改为 0，该志愿者相关证书已自动失效！')
  } else {
    ElMessage.success('志愿时修改成功，证书生效！')
  }
  hourDialogVisible.value = false
}
</script>

<style scoped>
.mb-20{margin-bottom:20px;}
.pagination-wrap{
  display:flex;
  justify-content:space-between;
  align-items:center;
  margin-top:15px;
}
</style>