<template>
  <div class="app-container">
    <el-card>
      <template #header><span style="font-weight:bold;">申请成为组织者</span></template>
      
      <div style="max-width: 600px; margin: 20px auto;">
        <el-form label-width="120px">
          <el-form-item label="编号">
            <el-input value="VOL-13421" disabled />
          </el-form-item>
          <el-form-item label="申请人姓名">
            <el-input value="张三" disabled />
          </el-form-item>
          <el-form-item label="所属组织">
            <el-input placeholder="请输入您代表的组织名称" />
          </el-form-item>
          <el-form-item label="申请理由">
            <el-input type="textarea" rows="4" placeholder="请简述申请成为组织者的理由..." />
          </el-form-item>
          <el-form-item label="证明材料上传">
            <el-upload action="#" :auto-upload="false" list-type="picture-card">
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div style="color: #999; font-size: 12px; margin-top: 5px;">请上传学生证或社团盖章证明扫描件</div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" @click="submitApply">提交申请</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const submitApply = () => {
  // 模拟表单数据
  const applyData = {
    name: '张三',
    orgName: '爱心社团', // 实际应绑定 v-model
    time: new Date().toLocaleString(),
    phone: '13812345678',
    orgIntro: '申请成为组织者以发布更多校园公益活动。',
    materialImg: 'https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg'
  }
  
  // 模拟将申请存入本地缓存，供管理员查看
  const currentApplies = JSON.parse(localStorage.getItem('pendingApplies') || '[]')
  currentApplies.push(applyData)
  localStorage.setItem('pendingApplies', JSON.stringify(currentApplies))

  ElMessage.success('申请资料已提交，请等待管理员审核！')
  
  // 模拟申请提交并秒通过
  ElMessage.success('申请资料已提交！模拟系统已自动为您开通组织者权限。')
  
  // 核心：修改资格标记，右上角的按钮会自动变为“切换为组织者”
  localStorage.setItem('isOrganizerQualified', 'true')
  
  // 返回首页
  setTimeout(() => {
    router.push('/sys/dashboard-volun')
  }, 1000)
}
</script>