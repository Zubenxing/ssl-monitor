<template>
  <div class="domain-list">
    <Dashboard :domains="domains" />
    
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <div class="left">
            <span class="title">域名证书监控</span>
            <el-select
              v-model="filterStatus"
              placeholder="证书状态"
              clearable
              class="filter-select"
            >
              <el-option label="全部" value="" />
              <el-option label="正常" value="VALID" />
              <el-option label="即将过期" value="EXPIRING" />
              <el-option label="异常" value="ERROR" />
            </el-select>
          </div>
          <el-button type="primary" @click="showAddDomainDialog">添加域名</el-button>
        </div>
      </template>

      <el-table 
        :data="filteredDomains" 
        style="width: 100%" 
        v-loading="loading"
        :row-class-name="getRowClassName"
      >
        <el-table-column prop="domainName" label="域名" min-width="180" />
        <el-table-column prop="notificationEmail" label="通知邮箱" min-width="180" />
        <el-table-column prop="certificateStatus" label="证书状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.certificateStatus)">
              {{ scope.row.certificateStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="certificateExpiryDate" label="到期时间" width="180">
          <template #default="scope">
            <div :class="getExpiryClass(scope.row.certificateExpiryDate)">
              {{ formatDate(scope.row.certificateExpiryDate) }}
              <el-tag 
                v-if="isExpiringSoon(scope.row.certificateExpiryDate)" 
                size="small" 
                type="warning"
                class="expiry-tag"
              >
                {{ getDaysUntilExpiry(scope.row.certificateExpiryDate) }}天后过期
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="lastChecked" label="最后检查时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.lastChecked) }}
          </template>
        </el-table-column>
        <el-table-column prop="autoRenewal" label="自动续期" width="100">
          <template #default="scope">
            <el-switch
              v-model="scope.row.autoRenewal"
              @change="toggleAutoRenewal(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="280">
          <template #default="scope">
            <el-button size="small" @click="checkCertificate(scope.row)">
              检查证书
            </el-button>
            <el-button
              size="small"
              type="warning"
              @click="sendNotification(scope.row)"
              :disabled="!scope.row.certificateExpiryDate"
            >
              发送通知
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="deleteDomain(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="添加域名">
      <el-form :model="newDomain" label-width="120px">
        <el-form-item label="域名">
          <el-input v-model="newDomain.domainName" />
        </el-form-item>
        <el-form-item label="通知邮箱">
          <el-input v-model="newDomain.notificationEmail" placeholder="接收证书过期通知的邮箱" />
        </el-form-item>
        <el-form-item label="自动续期">
          <el-switch v-model="newDomain.autoRenewal" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addDomain">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import moment from 'moment'
import Dashboard from './Dashboard.vue'

const API_BASE_URL = 'http://localhost:8080/api'
const domains = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const filterStatus = ref('')
const addingDomain = ref(false)
const newDomain = ref({
  domainName: '',
  notificationEmail: '',
  autoRenewal: true
})

const loadDomains = async () => {
  loading.value = true
  try {
    const response = await axios.get(`${API_BASE_URL}/domains`)
    domains.value = response.data
  } catch (error) {
    ElMessage.error('加载域名列表失败')
  }
  loading.value = false
}

const filteredDomains = computed(() => {
  if (!filterStatus.value) return domains.value
  
  return domains.value.filter(domain => {
    if (filterStatus.value === 'EXPIRING') {
      return isExpiringSoon(domain.certificateExpiryDate)
    }
    return domain.certificateStatus === filterStatus.value
  })
})

const addDomain = async () => {
  if (addingDomain.value) return;
  
  try {
    if (!newDomain.value.domainName.trim()) {
      ElMessage.error('域名不能为空')
      return
    }
    
    if (newDomain.value.notificationEmail && !isValidEmail(newDomain.value.notificationEmail)) {
      ElMessage.error('请输入有效的邮箱地址')
      return
    }

    addingDomain.value = true
    const response = await axios.post(`${API_BASE_URL}/domains`, newDomain.value)
    ElMessage.success('添加域名成功')
    dialogVisible.value = false
    await loadDomains()
  } catch (error) {
    const errorMessage = error.response?.data?.error || '添加域名失败'
    ElMessage.error(errorMessage)
    if (errorMessage.includes('already exists')) {
      dialogVisible.value = false
    }
  } finally {
    addingDomain.value = false
  }
}

const isValidEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

const deleteDomain = async (domain) => {
  try {
    await axios.delete(`${API_BASE_URL}/domains/${domain.id}`)
    ElMessage.success('删除域名成功')
    loadDomains()
  } catch (error) {
    ElMessage.error('删除域名失败')
  }
}

const checkCertificate = async (domain) => {
  try {
    await axios.post(`${API_BASE_URL}/domains/${domain.id}/check`)
    ElMessage.success('检查证书成功')
    loadDomains()
  } catch (error) {
    ElMessage.error('检查证书失败')
  }
}

const toggleAutoRenewal = async (domain) => {
  try {
    await axios.put(`${API_BASE_URL}/domains/${domain.id}/auto-renewal`)
    ElMessage.success('更新自动续期设置成功')
    loadDomains()
  } catch (error) {
    ElMessage.error('更新自动续期设置失败')
  }
}

const formatDate = (date) => {
  return date ? moment(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

const getStatusType = (status) => {
  switch (status) {
    case 'VALID':
      return 'success'
    case 'ERROR':
      return 'danger'
    default:
      return 'info'
  }
}

const isExpiringSoon = (date) => {
  if (!date) return false
  const expiryDate = moment(date)
  const daysUntilExpiry = expiryDate.diff(moment(), 'days')
  return daysUntilExpiry <= 30 && daysUntilExpiry >= 0
}

const getDaysUntilExpiry = (date) => {
  if (!date) return 0
  return moment(date).diff(moment(), 'days')
}

const getExpiryClass = (date) => {
  if (!date) return ''
  return isExpiringSoon(date) ? 'expiring-soon' : ''
}

const getRowClassName = ({ row }) => {
  if (row.certificateStatus === 'ERROR') return 'error-row'
  if (isExpiringSoon(row.certificateExpiryDate)) return 'warning-row'
  return ''
}

const showAddDomainDialog = () => {
  newDomain.value = {
    domainName: '',
    notificationEmail: '',
    autoRenewal: true
  }
  dialogVisible.value = true
}

const sendNotification = async (domain) => {
  try {
    await axios.post(`${API_BASE_URL}/domains/${domain.id}/send-notification`)
    ElMessage.success('通知邮件发送成功')
  } catch (error) {
    const errorMessage = error.response?.data?.error || '发送通知失败'
    ElMessage.error(errorMessage)
  }
}

onMounted(() => {
  loadDomains()
})
</script>

<style scoped>
.domain-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title {
  font-size: 16px;
  font-weight: bold;
}

.filter-select {
  width: 120px;
}

.dialog-footer {
  margin-top: 20px;
}

.expiring-soon {
  color: #E6A23C;
}

.expiry-tag {
  margin-left: 8px;
}

:deep(.error-row) {
  --el-table-tr-bg-color: var(--el-color-danger-light-9);
}

:deep(.warning-row) {
  --el-table-tr-bg-color: var(--el-color-warning-light-9);
}
</style> 