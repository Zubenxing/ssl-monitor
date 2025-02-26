<template>
  <el-container class="layout-container">
    <el-aside width="240px" class="aside">
      <div class="logo-container">
        <el-icon class="logo-icon"><Monitor /></el-icon>
        <span class="title">SSL证书监控</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="side-menu"
        @select="handleSelect"
        :router="true">
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/domains">
          <el-icon><Document /></el-icon>
          <span>域名管理</span>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
      
      <div class="help-center">
        <div class="help-icon">
          <el-icon><QuestionFilled /></el-icon>
        </div>
        <div class="help-text">
          <h4>帮助中心</h4>
          <p>遇到问题？请联系我们获取更多帮助</p>
        </div>
        <el-button size="small" type="primary" class="help-button">联系支持</el-button>
      </div>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <h2 class="welcome-text">欢迎使用SSL证书监控系统</h2>
        </div>
        <div class="header-right">
          <el-badge is-dot class="notification-badge">
            <el-icon class="notification-icon"><Bell /></el-icon>
          </el-badge>
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-info">
              <el-avatar :size="40" class="user-avatar">管理</el-avatar>
              <span class="username">管理员</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main-content">
        <router-view></router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Monitor, DataBoard, Document, Setting, Bell, ArrowDown, QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const activeMenu = ref(route.path)

const handleSelect = (index) => {
  router.push(index)
}

const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('token')
    router.push('/login')
    ElMessage.success('已成功退出登录')
  } else if (command === 'profile') {
    ElMessage.info('个人资料功能开发中...')
  }
}

onMounted(() => {
  activeMenu.value = route.path
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background-color: #f5f7fa;
  width: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.aside {
  background: linear-gradient(180deg, #6e45e2 0%, #88d3ce 100%);
  color: white;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
}

.logo-container {
  height: 70px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-icon {
  font-size: 24px;
  margin-right: 10px;
  color: white;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: white;
}

.side-menu {
  border-right: none;
  background: transparent !important;
}

:deep(.el-menu) {
  border-right: none;
  background: transparent;
}

:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.8) !important;
  margin: 4px 0;
  border-radius: 8px;
  height: 50px;
  line-height: 50px;
  padding: 0 16px !important;
  margin: 4px 10px !important;
}

:deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.1) !important;
}

:deep(.el-menu-item.is-active) {
  background-color: rgba(255, 255, 255, 0.2) !important;
  color: white !important;
}

:deep(.el-menu-item i), :deep(.el-menu-item .el-icon) {
  color: rgba(255, 255, 255, 0.8) !important;
  margin-right: 12px;
}

:deep(.el-menu-item.is-active i), :deep(.el-menu-item.is-active .el-icon) {
  color: white !important;
}

.header {
  background-color: white;
  height: 70px;
  line-height: 70px;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-text {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.notification-badge {
  cursor: pointer;
}

.notification-icon {
  font-size: 22px;
  color: #606266;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: all 0.3s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.user-avatar {
  background: linear-gradient(135deg, #6e45e2, #88d3ce);
  color: white;
  font-weight: bold;
}

.username {
  margin: 0 8px;
  font-size: 14px;
  color: #333;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
  height: calc(100vh - 70px); /* 减去header高度 */
  box-sizing: border-box;
}

.help-center {
  margin-top: auto;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 15px;
  margin: 20px 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: auto;
}

.help-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.help-text h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.help-text p {
  margin: 5px 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.4;
}

.help-button {
  margin-top: 5px;
  width: 100%;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
}
</style> 