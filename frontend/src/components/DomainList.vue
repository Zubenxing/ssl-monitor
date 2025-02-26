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
        <el-table-column prop="domainName" label="域名" min-width="160" />
        <el-table-column prop="notificationEmail" label="通知邮箱" min-width="160" />
        <el-table-column prop="certificateStatus" label="证书状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.certificateStatus)">
              {{ getStatusText(scope.row.certificateStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="certificateExpiryDate" label="到期时间" width="160">
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
        <el-table-column prop="lastChecked" label="最后检查时间" width="160">
          <template #default="scope">
            {{ formatDate(scope.row.lastChecked) }}
          </template>
        </el-table-column>
        <el-table-column prop="autoRenewal" label="自动续期" width="80" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.autoRenewal"
              @change="toggleAutoRenewal(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="360" align="center">
          <template #default="scope">
            <div class="operation-buttons">
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
                type="primary"
                @click="showEditDialog(scope.row)"
              >
                编辑
              </el-button>
              <el-button
                size="small"
                type="danger"
                @click="deleteDomain(scope.row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑域名' : '添加域名'" :close-on-click-modal="false">
      <el-form :model="newDomain" label-width="120px">
        <el-form-item label="域名" required>
          <el-input 
            v-model="newDomain.domainName" 
            :placeholder="isEditing ? '' : '请输入域名（如：example.com）'"
            :disabled="isUpdating || addingDomain"
          />
        </el-form-item>
        <el-form-item label="通知邮箱">
          <el-input 
            v-model="newDomain.notificationEmail" 
            placeholder="接收证书过期通知的邮箱"
            :disabled="isUpdating || addingDomain"
          />
        </el-form-item>
        <el-form-item label="自动续期">
          <el-switch 
            v-model="newDomain.autoRenewal"
            :disabled="isUpdating || addingDomain"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false" :disabled="isUpdating || addingDomain">取消</el-button>
          <el-button 
            type="primary" 
            @click="isEditing ? updateDomain() : addDomain()"
            :loading="isUpdating || addingDomain"
          >
            {{ isUpdating || addingDomain ? '处理中...' : '确定' }}
          </el-button>
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
const MAX_RETRIES = 3
const RETRY_DELAY = 1000 // 1 second
const domains = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const filterStatus = ref('')
const addingDomain = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const isUpdating = ref(false)
const newDomain = ref({
  domainName: '',
  notificationEmail: '',
  autoRenewal: true
})

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

const loadDomains = async (retryCount = 0) => {
  loading.value = true
  try {
    const response = await axios.get(`${API_BASE_URL}/domains`)
    domains.value = response.data
  } catch (error) {
    console.error('Failed to load domains:', error)
    if (error.message.includes('Network Error') && retryCount < MAX_RETRIES) {
      ElMessage.warning(`连接服务器失败，${retryCount + 1}秒后重试...`)
      await sleep(RETRY_DELAY)
      return loadDomains(retryCount + 1)
    }
    ElMessage.error(error.message.includes('Network Error') ? 
      '无法连接到服务器，请确保后端服务已启动' : '加载域名列表失败')
  } finally {
    loading.value = false
  }
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
    try {
      const response = await axios.post(`${API_BASE_URL}/domains`, newDomain.value)
      ElMessage.success('添加域名成功')
      dialogVisible.value = false
      await loadDomains()
    } catch (error) {
      if (error.message.includes('Network Error')) {
        ElMessage.error('无法连接到服务器，请确保后端服务已启动')
      } else {
        const errorMessage = error.response?.data?.error || '添加域名失败'
        ElMessage.error(errorMessage)
        if (errorMessage.includes('already exists')) {
          dialogVisible.value = false
        }
      }
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
  return status === 'VALID' ? 'success' : 'danger';
}

const getStatusText = (status) => {
  return status === 'VALID' ? '正常' : '异常';
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
  return row.certificateStatus === 'ERROR' ? 'error-row' : '';
}

const showEditDialog = (domain) => {
  isEditing.value = true
  editingId.value = domain.id
  newDomain.value = {
    domainName: domain.domainName,
    notificationEmail: domain.notificationEmail,
    autoRenewal: domain.autoRenewal
  }
  dialogVisible.value = true
}

const showAddDomainDialog = () => {
  isEditing.value = false
  editingId.value = null
  newDomain.value = {
    domainName: '',
    notificationEmail: '',
    autoRenewal: true
  }
  dialogVisible.value = true
}

const updateDomain = async () => {
  if (isUpdating.value) return;

  if (!newDomain.value.domainName.trim()) {
    ElMessage.error('域名不能为空')
    return
  }
  
  if (newDomain.value.notificationEmail && !isValidEmail(newDomain.value.notificationEmail)) {
    ElMessage.error('请输入有效的邮箱地址')
    return
  }

  try {
    isUpdating.value = true
    loading.value = true
    const response = await axios.put(`${API_BASE_URL}/domains/${editingId.value}`, newDomain.value)
    if (response.data) {
      ElMessage.success('更新域名成功')
      dialogVisible.value = false
      await loadDomains()
    }
  } catch (error) {
    console.error('Error updating domain:', error)
    const errorMessage = error.response?.data?.error || '更新域名失败'
    ElMessage.error(errorMessage)
  } finally {
    isUpdating.value = false
    loading.value = false
  }
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
  padding: 0;
  max-width: 100%;
  margin: 0;
  height: 100%;
}

.box-card {
  border-radius: 16px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

:deep(.el-card__header) {
  padding: 20px;
  border-bottom: 1px solid #ebeef5;
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
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.filter-select {
  width: 120px;
}

:deep(.el-button--primary) {
  background: linear-gradient(45deg, #6e45e2, #88d3ce);
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  transition: all 0.3s;
}

:deep(.el-button--primary:hover) {
  box-shadow: 0 4px 12px rgba(110, 69, 226, 0.3);
  transform: translateY(-2px);
}

:deep(.el-table) {
  border-radius: 0 0 16px 16px;
}

:deep(.el-table th) {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #606266;
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background-color: #f8f9fa;
}

:deep(.el-tag--success) {
  background-color: rgba(103, 194, 58, 0.1);
  border-color: rgba(103, 194, 58, 0.2);
  color: #67C23A;
}

:deep(.el-tag--danger) {
  background-color: rgba(245, 108, 108, 0.1);
  border-color: rgba(245, 108, 108, 0.2);
  color: #F56C6C;
}

:deep(.el-tag--warning) {
  background-color: rgba(230, 162, 60, 0.1);
  border-color: rgba(230, 162, 60, 0.2);
  color: #E6A23C;
}

.expiring-soon {
  color: #E6A23C;
  display: flex;
  align-items: center;
}

.expiry-tag {
  margin-left: 8px;
}

.operation-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: nowrap;
}

:deep(.operation-buttons .el-button) {
  padding: 6px 12px;
  border-radius: 6px;
}

:deep(.el-button--warning) {
  background-color: rgba(230, 162, 60, 0.1);
  border-color: #E6A23C;
  color: #E6A23C;
}

:deep(.el-button--danger) {
  background-color: rgba(245, 108, 108, 0.1);
  border-color: #F56C6C;
  color: #F56C6C;
}

:deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  margin: 0;
  padding: 20px;
  background: linear-gradient(45deg, #6e45e2, #88d3ce);
  color: white;
}

:deep(.el-dialog__title) {
  color: white;
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 30px 20px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

:deep(.error-row) {
  --el-table-tr-bg-color: rgba(245, 108, 108, 0.1);
}

:deep(.warning-row) {
  --el-table-tr-bg-color: rgba(230, 162, 60, 0.1);
}
</style> 