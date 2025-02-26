<template>
  <div class="dashboard">
    <el-row :gutter="24">
      <el-col :span="8">
        <el-card class="stat-card primary" shadow="hover">
          <div class="card-body">
            <div class="stat-icon">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">总域名数</div>
              <div class="stat-value">{{ totalDomains }}</div>
              <div class="stat-label">个域名</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="stat-card warning" shadow="hover">
          <div class="card-body">
            <div class="stat-icon">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">即将过期</div>
              <div class="stat-value">{{ expiringDomains }}</div>
              <div class="stat-label">个域名</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="stat-card danger" shadow="hover">
          <div class="card-body">
            <div class="stat-icon">
              <el-icon><WarningFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">异常状态</div>
              <div class="stat-value">{{ errorDomains }}</div>
              <div class="stat-label">个域名</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { InfoFilled, Connection, Timer, WarningFilled } from '@element-plus/icons-vue'

const props = defineProps({
  domains: {
    type: Array,
    required: true
  }
})

const totalDomains = computed(() => props.domains.length)

const expiringDomains = computed(() => {
  const thirtyDaysFromNow = new Date()
  thirtyDaysFromNow.setDate(thirtyDaysFromNow.getDate() + 30)
  
  return props.domains.filter(domain => {
    if (!domain.certificateExpiryDate) return false
    const expiryDate = new Date(domain.certificateExpiryDate)
    return expiryDate <= thirtyDaysFromNow
  }).length
})

const errorDomains = computed(() => {
  return props.domains.filter(domain => domain.certificateStatus === 'ERROR').length
})
</script>

<style scoped>
.dashboard {
  margin-bottom: 30px;
}

.stat-card {
  height: 180px;
  transition: all 0.3s;
  border-radius: 16px;
  border: none;
  overflow: hidden;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.card-body {
  height: 100%;
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-right: 20px;
}

.primary .stat-icon {
  background-color: rgba(110, 69, 226, 0.1);
  color: #6e45e2;
}

.warning .stat-icon {
  background-color: rgba(230, 162, 60, 0.1);
  color: #E6A23C;
}

.danger .stat-icon {
  background-color: rgba(245, 108, 108, 0.1);
  color: #F56C6C;
}

.stat-info {
  flex: 1;
}

.stat-title {
  font-size: 16px;
  color: #606266;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 36px;
  font-weight: 600;
  line-height: 1.2;
  margin-bottom: 5px;
}

.primary .stat-value {
  color: #6e45e2;
}

.warning .stat-value {
  color: #E6A23C;
}

.danger .stat-value {
  color: #F56C6C;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}
</style> 