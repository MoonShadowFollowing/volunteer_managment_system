<template>
  <div>
    <el-row :gutter="20" class="mb-20">
      <el-col :span="12">
        <el-card class="stat-card">
          <h3>我管理的活动</h3><p class="num">5 <span>个</span></p>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="stat-card">
          <h3>待审核报名</h3><p class="num">12 <span>人</span></p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
<!-- 样式同上复用 -->
<style scoped>
.mb-20 { margin-bottom: 20px; }
.stat-card { text-align: center; color: #e63946; }
.stat-card h3 { color: #666; margin-top: 0; }
.num { font-size: 32px; font-weight: bold; margin: 10px 0 0; }
.num span { font-size: 14px; font-weight: normal; }
</style>