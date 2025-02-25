<template>
  <el-container class="layout-container">
    <el-aside width="240px" class="aside">
      <div class="logo-container">
        <img src="../assets/logo.png" alt="Logo" class="logo">
        <span class="title">SSL证书监控</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="menu"
        @select="handleSelect"
        background-color="#f8f9fa"
        text-color="#333"
        active-text-color="#1a73e8">
        <el-menu-item index="/dashboard">
          <i class="el-icon-monitor"></i>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/domains">
          <i class="el-icon-document"></i>
          <span>域名管理</span>
        </el-menu-item>
        <el-menu-item index="/settings">
          <i class="el-icon-setting"></i>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="header">
        <div class="header-left"></div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar size="small">管理员</el-avatar>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main">
        <router-view></router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
export default {
  name: 'MainLayout',
  data() {
    return {
      activeMenu: '/dashboard'
    }
  },
  methods: {
    handleSelect(index) {
      this.$router.push(index)
    },
    handleCommand(command) {
      if (command === 'logout') {
        // TODO: 实现登出逻辑
        this.$router.push('/login')
      }
    }
  },
  created() {
    this.activeMenu = this.$route.path
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background-color: #f8f9fa;
  border-right: 1px solid #e0e0e0;
}

.logo-container {
  height: 64px;
  padding: 12px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e0e0e0;
}

.logo {
  width: 32px;
  margin-right: 12px;
}

.title {
  font-size: 18px;
  color: #1a73e8;
}

.menu {
  border-right: none;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.user-info {
  cursor: pointer;
}

.main {
  background-color: #f5f5f5;
  padding: 20px;
}
</style> 