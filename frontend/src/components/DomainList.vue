<template>
  <div class="domain-list">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>域名证书监控</span>
          <el-button type="primary" @click="showAddDomainDialog">添加域名</el-button>
        </div>
      </template>

      <el-table :data="domains" style="width: 100%" v-loading="loading">
        <el-table-column prop="domainName" label="域名" width="180" />
        <el-table-column prop="certificateStatus" label="证书状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.certificateStatus)">
              {{ scope.row.certificateStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="certificateExpiryDate" label="到期时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.certificateExpiryDate) }}
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
        <el-table-column label="操作">
          <template #default="scope">
            <el-button size="small" @click="checkCertificate(scope.row)">
              检查证书
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
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import moment from 'moment'

const API_BASE_URL = 'http://localhost:8080/api'
const domains = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const newDomain = ref({
  domainName: '',
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

const addDomain = async () => {
  try {
    await axios.post(`${API_BASE_URL}/domains`, newDomain.value)
    ElMessage.success('添加域名成功')
    dialogVisible.value = false
    loadDomains()
  } catch (error) {
    ElMessage.error('添加域名失败')
  }
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

const showAddDomainDialog = () => {
  newDomain.value = {
    domainName: '',
    autoRenewal: true
  }
  dialogVisible.value = true
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

.dialog-footer {
  margin-top: 20px;
}
</style> 