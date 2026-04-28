<template>
  <div>
    <el-row :gutter="20" class="mb-20">
      <el-col :span="12">
        <el-card class="stat-card">
          <h3>我的累计工时</h3>
          <p class="num">36 <span>小时 &nbsp; </span>30 <span>分钟</span></p>
        </el-card>
      </el-col>
      <!-- <el-col :span="8">
        <el-card class="stat-card">
          <h3>我的积分</h3>
          <p class="num">36 <span>分</span></p>
        </el-card>
      </el-col> -->
      <el-col :span="12">
        <el-card class="stat-card">
          <h3>已获证书</h3>
          <p class="num">2 <span>张</span></p>
        </el-card>
      </el-col>
    </el-row>
    <!-- <el-card header="最新可报名活动">
      <el-empty description="暂无最新活动" />
    </el-card> -->
  </div>
</template>
<style scoped>
.mb-20 { margin-bottom: 20px; }
.stat-card { text-align: center; color: #e63946; }
.stat-card h3 { color: #666; margin-top: 0; font-size: 16px; }
.num { font-size: 32px; font-weight: bold; margin: 10px 0 0; }
.num span { font-size: 14px; color: #999; font-weight: normal; }
</style>