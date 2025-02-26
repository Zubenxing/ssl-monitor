<template>
    <div class="login-page">
      <div class="login-container">
        <div class="login-box">
          <div class="login-header">
            <h2 class="login-title">SSL证书监控系统</h2>
            <p class="login-subtitle">安全监控您的网站证书</p>
          </div>
          
          <el-form :model="loginForm" :rules="rules" ref="loginForm">
            <el-form-item prop="username">
              <div class="input-label">用户名</div>
              <el-input 
                v-model="loginForm.username" 
                placeholder="请输入用户名">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <el-form-item prop="password">
              <div class="input-label">密码</div>
              <el-input 
                v-model="loginForm.password" 
                type="password" 
                placeholder="请输入密码"
                show-password>
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            
            <div class="login-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <a href="javascript:void(0)" @click="forgotPassword" class="forgot-password">忘记密码?</a>
            </div>
            
            <div class="button-group">
              <el-button 
                type="primary" 
                @click="handleLogin" 
                class="login-button"
                :loading="loading">
                {{ loading ? '登录中...' : '登录' }}
              </el-button>
              
              <el-button 
                @click="goToRegister" 
                class="register-button">
                注册账号
              </el-button>
            </div>
          </el-form>
          
          <div class="login-footer">
            <p>© {{ new Date().getFullYear() }} SSL证书监控系统</p>
          </div>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  import { User, Lock } from '@element-plus/icons-vue'
  import axios from 'axios'
  
  export default {
    name: 'Login',
    components: {
      User,
      Lock
    },
    data() {
      return {
        loading: false,
        rememberMe: false,
        loginForm: {
          username: '',
          password: ''
        },
        rules: {
          username: [
            { required: true, message: '请输入用户名', trigger: 'blur' }
          ],
          password: [
            { required: true, message: '请输入密码', trigger: 'blur' }
          ]
        }
      }
    },
    mounted() {
      // 如果之前选择了记住我，自动填充用户名
      const savedUsername = localStorage.getItem('username')
      if (savedUsername) {
        this.loginForm.username = savedUsername
        this.rememberMe = true
      }
    },
    methods: {
      async handleLogin() {
        this.$refs.loginForm.validate(async valid => {
          if (valid) {
            this.loading = true
            try {
              // 使用全局配置的 axios 实例
              const response = await this.$http.post('/auth/login', {
                username: this.loginForm.username,
                password: this.loginForm.password
              })
              
              // 保存令牌
              localStorage.setItem('token', response.data.token)
              
              // 如果选择了记住我，保存用户名
              if (this.rememberMe) {
                localStorage.setItem('username', this.loginForm.username)
              }
              
              // 登录成功后跳转到仪表盘
              this.$router.push('/dashboard')
              this.$message.success('登录成功！')
            } catch (error) {
              console.error('Login error:', error)
              // 处理登录错误
              const errorMsg = error.response?.data?.message || '登录失败，请检查网络连接'
              this.$message.error(errorMsg)
            } finally {
              this.loading = false
            }
          }
        })
      },
      forgotPassword() {
        // 暂时使用弹窗提示
        this.$message({
          message: '密码重置功能正在开发中...',
          type: 'info'
        })
      },
      goToRegister() {
        this.$message({
          message: '注册功能正在开发中...',
          type: 'info'
        })
      }
    }
  }
  </script>
  
  <style scoped>
  .login-page {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(135deg, #ff6b6b, #4ecdc4);
    background-size: 400% 400%;
    animation: gradientBG 15s ease infinite;
  }
  
  @keyframes gradientBG {
    0% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
    100% { background-position: 0% 50%; }
  }
  
  .login-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    width: 100%;
  }
  
  .login-box {
    width: 90%;
    max-width: 420px;
    background: rgba(255, 255, 255, 0.15);
    backdrop-filter: blur(10px);
    border-radius: 20px;
    border: 1px solid rgba(255, 255, 255, 0.3);
    padding: 35px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
    display: flex;
    flex-direction: column;
  }
  
  .login-header {
    text-align: center;
    margin-bottom: 30px;
  }
  
  .login-title {
    color: white;
    font-size: 28px;
    margin: 0 0 10px;
    font-weight: 500;
    letter-spacing: 1px;
  }
  
  .login-subtitle {
    color: rgba(255, 255, 255, 0.8);
    margin: 0;
    font-size: 16px;
  }
  
  .input-label {
    color: white;
    font-size: 15px;
    margin-bottom: 8px;
  }
  
  .button-group {
    display: flex;
    gap: 15px;
    margin-top: 10px;
  }
  
  .login-button {
    flex: 3;
    background: linear-gradient(45deg, #3be8b0, #1a73e8);
    border: none;
    height: 45px;
    font-size: 16px;
    border-radius: 10px;
    transition: all 0.3s ease;
  }
  
  .register-button {
    flex: 2;
    background: rgba(255, 255, 255, 0.2);
    border: 1px solid rgba(255, 255, 255, 0.3);
    color: white;
    height: 45px;
    font-size: 16px;
    border-radius: 10px;
    transition: all 0.3s ease;
  }
  
  .login-button:hover, .register-button:hover {
    transform: translateY(-2px);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
  }
  
  .login-options {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }
  
  .forgot-password {
    color: rgba(255, 255, 255, 0.8);
    text-decoration: none;
    font-size: 14px;
    transition: all 0.2s ease;
  }
  
  .forgot-password:hover {
    color: white;
    text-decoration: underline;
  }
  
  .login-footer {
    text-align: center;
    margin-top: 30px;
    color: rgba(255, 255, 255, 0.6);
    font-size: 14px;
  }
  
  :deep(.el-form-item) {
    margin-bottom: 20px !important;
  }
  
  :deep(.el-form-item__error) {
    color: #ff9a9e !important;
  }
  
  :deep(.el-checkbox__label) {
    color: rgba(255, 255, 255, 0.8) !important;
  }
  
  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: #3be8b0 !important;
    border-color: #3be8b0 !important;
  }
  
  :deep(.el-input__inner) {
    background: rgba(255, 255, 255, 0.6) !important;
    border: 1px solid rgba(255, 255, 255, 0.3) !important;
    color: #333333 !important;
    height: 45px;
    border-radius: 10px;
    padding-left: 45px;
    font-weight: 500;
    width: 100%;
  }
  
  :deep(.el-input__prefix) {
    left: 15px !important;
    color: rgba(0, 0, 0, 0.5);
  }
  
  :deep(.el-input__suffix) {
    right: 15px !important;
    color: rgba(0, 0, 0, 0.5);
  }
  
  :deep(.el-input__inner::placeholder) {
    color: rgba(0, 0, 0, 0.4);
  }
  
  /* 响应式调整 */
  @media (max-width: 480px) {
    .login-box {
      padding: 25px;
    }
    
    .button-group {
      flex-direction: column;
      gap: 10px;
    }
    
    .login-button, .register-button {
      width: 100%;
    }
  }
  </style>