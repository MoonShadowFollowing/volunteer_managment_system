<template>
  <div class="app-container">
    <!-- 1. 活动列表主界面 -->
    <div v-if="!currentActivity">
      <el-card class="mb-20">
        <el-form :inline="true">
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
        <el-table :data="activities" border v-loading="actLoading">

          <el-table-column label="编号" width="80" align="center">
            <template #default="scope">
              {{ (activityPage - 1) * activityPageSize + scope.$index + 1 }}
            </template>
          </el-table-column>

          <el-table-column prop="actNo" label="活动编号" width="140" align="center" />

          <el-table-column prop="name" label="活动名称" />

          <el-table-column prop="startTime" label="开始时间" width="180" align="center"/>
          <el-table-column prop="endTime" label="结束时间" width="180" align="center"/>

          <el-table-column prop="limitNum" label="招募人数" width="90" align="center" />

          <el-table-column prop="auditStatus" label="审核状态" width="120" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.auditStatus==='审核通过'?'success':(scope.row.auditStatus==='审核不通过'?'danger':'warning')">
                {{ scope.row.auditStatus }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="150" align="center">
            <template #default="scope">
              <el-button type="primary" @click="enterManage(scope.row)">进入管理</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <div>共 {{ activityTotal }} 条</div>
          <el-pagination
            v-model:current-page="activityPage"
            v-model:page-size="activityPageSize"
            :page-sizes="[5, 10, 20, 50]"
            :total="activityTotal"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="handleActSizeChange"
            @current-change="handleActCurrentChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 2. 具体活动的管理详情页 -->
    <div v-else>
      <el-card class="mb-20">
        <template #header>
          <el-button icon="ArrowLeft" @click="exitManage" style="margin-right: 15px">返回活动列表</el-button>
          <strong style="font-size: 16px;">当前管理活动：{{ currentActivity.name }}</strong>
        </template>

        <el-tabs v-model="activeTab">

          <!-- 报名审核 -->
          <el-tab-pane label="报名审核" name="audit">
            <el-table :data="volunteers" border v-loading="regLoading">
              <el-table-column label="编号" width="80" align="center">
                <template #default="scope">{{ scope.$index + 1 }}</template>
              </el-table-column>

              <el-table-column prop="volId" label="志愿者编号" width="200" align="center" />
              <el-table-column prop="volName" label="志愿者姓名" />

              <el-table-column prop="auditStatus" label="审核状态">
                <template #default="scope">
                  <el-tag :type="scope.row.auditStatus==='待审核'?'warning':(scope.row.auditStatus==='审核通过'?'success':'danger')">
                    {{ scope.row.auditStatus }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="操作" width="200" align="center">
                <template #default="scope">
                  <el-button v-if="scope.row.auditStatus==='待审核'" type="success" size="small" @click="handleAudit(scope.row, true)">通过</el-button>
                  <el-button v-if="scope.row.auditStatus==='待审核'" type="danger" size="small" @click="handleAudit(scope.row, false)">拒绝</el-button>
                  <span v-else style="color:#999; font-size: 12px;">已处理</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 志愿时 -->
          <el-tab-pane label="志愿时管理" name="hours">
            <el-table :data="signRecords" border v-loading="attLoading">
              <el-table-column label="编号" width="80" align="center">
                <template #default="scope">{{ scope.$index + 1 }}</template>
              </el-table-column>

              <el-table-column prop="volId" label="志愿者编号" width="200" align="center" />
              <el-table-column prop="volName" label="志愿者姓名" />

              <el-table-column prop="signStatus" label="签到状态">
                <template #default="scope">
                  <el-tag :type="scope.row.signStatus==='正常'?'success':'danger'">
                    {{ scope.row.signStatus }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column prop="checkInTime" label="签到时间" width="180" align="center">
                <template #default="scope">{{ scope.row.checkInTime || '-' }}</template>
              </el-table-column>
              <el-table-column prop="checkOutTime" label="签退时间" width="180" align="center">
                <template #default="scope">{{ scope.row.checkOutTime || '-' }}</template>
              </el-table-column>

              <el-table-column label="志愿时长" align="center">
                <template #default="scope">
                  <span v-if="scope.row.signStatus==='正常'">
                    {{ scope.row.hours }} 小时 {{ scope.row.minutes }} 分钟
                  </span>
                  <span v-else style="color:#e63946">暂无工时</span>
                </template>
              </el-table-column>

              <el-table-column label="操作" width="200" align="center">
                <template #default="scope">
                  <el-button v-if="scope.row.signStatus!=='正常'" type="warning" size="small" @click="openSignDialog(scope.row)">手动补签</el-button>
                  <el-button v-else type="primary" size="small" @click="openHourDialog(scope.row)">修改志愿时</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

        </el-tabs>
      </el-card>
    </div>

    <!-- 修改志愿时弹窗 -->
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
        <el-button type="primary" :loading="saving" @click="confirmHourChange">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 手动补签弹窗 -->
    <el-dialog v-model="signDialogVisible" title="手动补签" width="460px">
      <el-form label-width="90px">
        <el-form-item label="签到时间">
          <el-date-picker v-model="signForm.checkInTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="签退时间">
          <el-date-picker v-model="signForm.checkOutTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="志愿小时">
          <el-input-number v-model="signForm.hours" :min="0" />
        </el-form-item>
        <el-form-item label="志愿分钟">
          <el-input-number v-model="signForm.minutes" :min="0" :max="59" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="signDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="confirmSign">确认补签</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listActivities } from '../../api/activity'
import { listRegistrations, auditRegistration } from '../../api/registration'
import { listAttendance, updateHours, manualSign } from '../../api/attendance'

const myId = Number(localStorage.getItem('userId')) || undefined

/* 活动列表 */
const queryName = ref('')
const queryDate = ref([])
const activityPage = ref(1)
const activityPageSize = ref(5)
const activityTotal = ref(0)
const activities = ref([])
const actLoading = ref(false)

const loadActivities = async () => {
  actLoading.value = true
  try {
    const range = queryDate.value || []
    const res = await listActivities({
      organizerId: myId,
      name: queryName.value || undefined,
      startDate: range.length === 2 ? range[0] : undefined,
      endDate: range.length === 2 ? range[1] : undefined,
      page: activityPage.value,
      pageSize: activityPageSize.value
    })
    activities.value = res.rows || []
    activityTotal.value = res.total || 0
  } finally {
    actLoading.value = false
  }
}
onMounted(loadActivities)

const handleSearch = () => { activityPage.value = 1; loadActivities() }
const handleReset = () => { queryName.value = ''; queryDate.value = []; activityPage.value = 1; loadActivities() }
const handleActSizeChange = (v) => { activityPageSize.value = v; activityPage.value = 1; loadActivities() }
const handleActCurrentChange = (v) => { activityPage.value = v; loadActivities() }

/* 进入某活动管理 */
const currentActivity = ref(null)
const activeTab = ref('audit')

const enterManage = (row) => {
  currentActivity.value = row
  activeTab.value = 'audit'
  loadRegs()
  loadAtts()
}
const exitManage = () => { currentActivity.value = null }

/* 报名审核 */
const volunteers = ref([])
const regLoading = ref(false)
const loadRegs = async () => {
  regLoading.value = true
  try {
    const res = await listRegistrations({ activityId: currentActivity.value.activityId, page: 1, pageSize: 500 })
    volunteers.value = res.rows || []
  } finally {
    regLoading.value = false
  }
}
const handleAudit = async (row, approve) => {
  try {
    await auditRegistration(row.regId, approve)
    ElMessage.success(approve ? '已通过！' : '已拒绝！')
    await loadRegs()
    await loadAtts() // 通过会自动建签到记录
  } catch (e) { /* 拦截器已提示 */ }
}

/* 志愿时管理 */
const signRecords = ref([])
const attLoading = ref(false)
const loadAtts = async () => {
  attLoading.value = true
  try {
    const res = await listAttendance({ activityId: currentActivity.value.activityId, page: 1, pageSize: 500 })
    signRecords.value = res.rows || []
  } finally {
    attLoading.value = false
  }
}

const saving = ref(false)

/* 修改志愿时 */
const hourDialogVisible = ref(false)
const targetRecord = ref(null)
const tempHours = ref({ h: 0, m: 0 })
const openHourDialog = (row) => {
  targetRecord.value = row
  tempHours.value = { h: row.hours || 0, m: row.minutes || 0 }
  hourDialogVisible.value = true
}
const confirmHourChange = async () => {
  saving.value = true
  try {
    await updateHours(targetRecord.value.recordId, { hours: tempHours.value.h, minutes: tempHours.value.m })
    if (tempHours.value.h === 0 && tempHours.value.m === 0) {
      ElMessage.warning('工时已改为 0，该志愿者相关证书已自动失效！')
    } else {
      ElMessage.success('志愿时修改成功，证书已生效！')
    }
    hourDialogVisible.value = false
    await loadAtts()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

/* 手动补签 */
const signDialogVisible = ref(false)
const signForm = ref({ checkInTime: '', checkOutTime: '', hours: 0, minutes: 0 })
const openSignDialog = (row) => {
  targetRecord.value = row
  signForm.value = {
    checkInTime: currentActivity.value.startTime || '',
    checkOutTime: currentActivity.value.endTime || '',
    hours: 0,
    minutes: 0
  }
  signDialogVisible.value = true
}
const confirmSign = async () => {
  const f = signForm.value
  if (!f.checkInTime || !f.checkOutTime) return ElMessage.warning('请选择签到和签退时间')
  if (f.checkOutTime <= f.checkInTime) return ElMessage.warning('签退时间必须晚于签到时间')
  saving.value = true
  try {
    await manualSign(targetRecord.value.recordId, {
      checkInTime: f.checkInTime,
      checkOutTime: f.checkOutTime,
      hours: f.hours,
      minutes: f.minutes
    })
    ElMessage.success('补签成功，状态已恢复正常')
    signDialogVisible.value = false
    await loadAtts()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
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
