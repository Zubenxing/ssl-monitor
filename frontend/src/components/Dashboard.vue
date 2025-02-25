<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="stat-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>总域名数</span>
            </div>
          </template>
          <div class="stat-value">
            <span class="number">{{ totalDomains }}</span>
            <span class="label">个域名</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card warning" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>即将过期</span>
              <el-tooltip
                content="30天内即将过期的证书数量"
                placement="top"
              >
                <el-icon><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <div class="stat-value">
            <span class="number">{{ expiringDomains }}</span>
            <span class="label">个域名</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card error" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>异常状态</span>
            </div>
          </template>
          <div class="stat-value">
            <span class="number">{{ errorDomains }}</span>
            <span class="label">个域名</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'

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
  margin-bottom: 20px;
}

.stat-card {
  height: 160px;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-value {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 80px;
}

.number {
  font-size: 36px;
  font-weight: bold;
  color: #409EFF;
}

.warning .number {
  color: #E6A23C;
}

.error .number {
  color: #F56C6C;
}

.label {
  margin-top: 8px;
  color: #909399;
  font-size: 14px;
}

.el-icon {
  cursor: help;
  color: #909399;
}
</style> 