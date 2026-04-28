<template>
  <div>
    <el-row :gutter="20" class="mb-20">
      <el-col :span="6"><el-card shadow="hover">全校总活动数<br/><b style="font-size:24px;color:#e63946">128</b></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover">注册志愿者<br/><b style="font-size:24px;color:#e63946">4,021</b></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover">累计工时<br/><b style="font-size:24px;color:#e63946">15,302</b></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover">已发证书<br/><b style="font-size:24px;color:#e63946">850</b></el-card></el-col>
    </el-row>
    <el-card header="系统运行状态">
      <p style="color:#67c23a">● 数据库连接正常</p>
      <p style="color:#67c23a">● 本地备份服务运行中</p>
    </el-card>
  </div>
</template>
<style scoped>.mb-20{margin-bottom:20px;text-align:center;color:#666}</style>