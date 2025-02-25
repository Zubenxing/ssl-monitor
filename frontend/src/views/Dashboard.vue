<template>
  <div class="dashboard">
    <h2>仪表盘</h2>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="dashboard-card">
          <template #header>
            <div class="card-header">
              <span>总域名数</span>
            </div>
          </template>
          <div class="card-content">
            <h1>{{ stats.totalDomains }}</h1>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="dashboard-card">
          <template #header>
            <div class="card-header">
              <span>即将过期</span>
            </div>
          </template>
          <div class="card-content warning">
            <h1>{{ stats.expiringDomains }}</h1>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="dashboard-card">
          <template #header>
            <div class="card-header">
              <span>已过期</span>
            </div>
          </template>
          <div class="card-content danger">
            <h1>{{ stats.expiredDomains }}</h1>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: 'Dashboard',
  data() {
    return {
      stats: {
        totalDomains: 0,
        expiringDomains: 0,
        expiredDomains: 0
      }
    }
  },
  mounted() {
    // TODO: 从后端获取统计数据
    this.stats = {
      totalDomains: 10,
      expiringDomains: 2,
      expiredDomains: 1
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.dashboard-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-content {
  text-align: center;
  padding: 20px 0;
}

.card-content h1 {
  margin: 0;
  font-size: 36px;
  color: #1a73e8;
}

.warning h1 {
  color: #e6a23c;
}

.danger h1 {
  color: #f56c6c;
}
</style> 