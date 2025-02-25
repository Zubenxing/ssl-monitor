# SSL证书监控系统

一个基于Java Spring Boot和Vue3的SSL证书监控系统，用于监控多个域名的SSL证书状态和自动续期。

## 功能特点

- 多域名SSL证书监控
- 证书到期提醒
- 自动续期功能（基于Let's Encrypt）
- 友好的Web界面
- 定时检查证书状态

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2.2
- Spring Data JPA
- H2 Database
- ACME4J (Let's Encrypt客户端)

### 前端
- Vue 3
- Element Plus
- Axios
- Moment.js

## 快速开始

### 后端启动
```bash
cd backend
mvn spring-boot:run
```

### 前端启动
```bash
cd frontend
npm install
npm run dev
```

## 配置说明

### 后端配置
配置文件位置：`backend/src/main/resources/application.properties`

主要配置项：
- 服务器端口：8080
- H2数据库配置
- CORS配置
- 日志级别

### 前端配置
- API基础URL：`http://localhost:8080/api`
- 开发服务器端口：5173

## API接口

### 域名管理
- GET /api/domains - 获取所有域名
- POST /api/domains - 添加新域名
- DELETE /api/domains/{id} - 删除域名
- PUT /api/domains/{id}/auto-renewal - 切换自动续期状态
- POST /api/domains/{id}/check - 检查指定域名的证书状态

## 开发说明

### 目录结构
```
ssl-monitor/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   └── pom.xml
└── frontend/
    ├── src/
    ├── public/
    └── package.json
```

## 注意事项

1. 证书自动续期功能需要确保服务器具有适当的权限
2. Let's Encrypt有请求频率限制，请合理设置检查间隔
3. 建议在生产环境使用持久化数据库替代H2
4. 确保域名可以正常访问，否则可能无法检查证书状态 