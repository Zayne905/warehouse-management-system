# 仓储管理系统 (Warehouse Management System)

基于 **Vue 3 + Spring Boot** 的全栈仓储管理系统，前后端分离架构。

## 功能模块

### 已完成

| 模块 | 功能 | 说明 |
|------|------|------|
| 登录认证 | JWT 无状态登录 | admin / admin123 |
| 布局框架 | 侧边栏菜单 + 顶部标签页 | 菜单与标签页联动 |
| 供应商管理 | 供应商列表查询 | 主数据模块 |
| 物料管理 | 物料列表查询 | 按供应商筛选 |
| 库区管理 | 库区列表查询 | A/B/C/D/E 五区 |
| 入库管理 | 入库单 CRUD + 扫码入库 | 完整入库流程 |
| 权限控制 | admin/user 角色区分 | 业务级权限校验 |

### 规划中

- 出库管理
- 库存盘点
- 仓库列表管理
- 用户管理
- 操作日志

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端框架 | Vue 3 + TypeScript + Vite |
| UI 组件库 | Element Plus（中文） |
| 状态管理 | Pinia |
| HTTP 客户端 | Axios |
| 路由 | Vue Router 4 |
| 后端框架 | Spring Boot 3.2.5 |
| 安全认证 | Spring Security + JWT (jjwt 0.12.5) |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8.0 |
| 构建工具 | Maven / npm |

## 项目结构

```
warehouse_manage/
├── warehouse-frontend/           # Vue 3 前端项目 (dev port 5173)
│   └── src/
│       ├── api/                  # Axios 封装 + API 函数
│       ├── layout/               # MainLayout / SideMenu / TabBar
│       ├── router/               # 路由配置 + beforeEach 守卫
│       ├── stores/               # Pinia 状态 (auth, tabs)
│       ├── types/                # TypeScript 类型定义
│       └── views/                # 页面组件
│           ├── Login.vue
│           ├── Dashboard.vue
│           ├── warehouse/        # 仓库管理
│           ├── inventory/        # 库存管理（入库已实现）
│           │   └── inbound/      # 入库表单 / 详情 / 组件
│           └── system/           # 系统管理
│
└── warehouse-backend/            # Spring Boot 后端项目 (port 8081)
    └── src/main/
        ├── java/com/warehouse/
        │   ├── config/           # Security / CORS / MyBatis-Plus / 全局异常
        │   ├── controller/       # REST 控制器
        │   ├── model/
        │   │   ├── entity/       # 数据库实体
        │   │   ├── dto/          # 数据传输对象
        │   │   └── enums/        # 枚举（入库状态等）
        │   ├── mapper/           # MyBatis-Plus Mapper
        │   ├── security/         # JWT 工具 / 过滤器 / 安全工具
        │   └── service/          # 业务逻辑层
        └── resources/
            ├── application.yml   # 应用配置
            └── sql/              # 数据库初始化脚本
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- Git

### 1. 克隆项目

```bash
git clone git@github.com:Zayne905/warehouse-management-system.git
cd warehouse_manage
```

### 2. 数据库初始化

确保 MySQL 运行中，然后依次执行：

```bash
# Phase 1: 用户表 + 管理员账号
mysql -u root -p1234 < warehouse-backend/src/main/resources/sql/init.sql

# Phase 2: 入库管理相关表（供应商、物料、库区、入库单等）
mysql -u root -p1234 < warehouse-backend/src/main/resources/sql/V2__phase2_schema.sql

# Phase 2: 测试数据（入库单样例、扫描记录）
mysql -u root -p1234 < warehouse-backend/src/main/resources/sql/V3__test_data.sql
```

> **默认管理员账号**：`admin` / `admin123`
>
> **数据库连接**：root / 1234 @ localhost:3306（可在 `application.yml` 中修改）

### 3. 启动后端

```bash
cd warehouse-backend
mvn spring-boot:run
```

后端启动在 **http://localhost:8081**。

### 4. 启动前端

```bash
cd warehouse-frontend
npm install
npm run dev
```

前端启动在 **http://localhost:5173**，API 请求自动代理到后端。

### 5. 访问系统

浏览器打开 http://localhost:5173，使用 `admin` / `admin123` 登录。

## 配置说明

### 数据库配置

编辑 `warehouse-backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/warehouse_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
    username: root          # 修改为你的数据库用户名
    password: 1234          # 修改为你的数据库密码
```

### JWT 配置

```yaml
jwt:
  secret: your-256-bit-secret-key-here   # 生产环境务必更换
  expiration: 86400000                    # Token 有效期（毫秒），默认 24 小时
```

### 前端代理配置

编辑 `warehouse-frontend/vite.config.ts`：

```ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8081',  // 后端地址
      changeOrigin: true,
    },
  },
}
```

## API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录（无需认证） |
| GET | `/api/user/info` | 当前用户信息 |
| GET | `/api/supplier/list` | 供应商列表 |
| GET | `/api/part/list` | 物料列表 |
| GET | `/api/warehouse-area/list` | 库区列表 |
| POST | `/api/inbound-order/list` | 入库单列表（分页） |
| POST | `/api/inbound-order/save` | 新增/修改入库单 |
| POST | `/api/inbound-order/detail` | 入库单详情 |
| POST | `/api/inbound-order/delete` | 删除入库单 |
| POST | `/api/inbound-order/submit` | 提交入库单 |
| POST | `/api/inbound-order/batch-copy-parts` | 批量复制物料 |
| POST | `/api/inbound-order/batch-set-area` | 批量设置库区 |
| POST | `/api/scan/check-duplicate` | 扫码重复检查 |
| POST | `/api/scan/submit` | 提交扫描记录 |
| POST | `/api/scan/list` | 扫描记录列表 |
| POST | `/api/scan/delete` | 删除扫描记录 |
| POST | `/api/scan/feedback` | 扫描回显信息 |

## 入库状态说明

| 状态码 | 状态名 | 说明 |
|--------|--------|------|
| 0 | 未入库 | 新建入库单，还未开始入库 |
| 1 | 部分入库 | 有部分物料已入库 |
| 2 | 已入库 | 所有物料数量达标 |
| 3 | 作废 | 入库单已作废 |

## 角色权限

| 操作 | admin | user |
|------|-------|------|
| 创建入库单 | ✅ | ✅ |
| 编辑未入库/部分入库单 | ✅ | ✅ |
| 编辑已入库单 | ✅ | ❌ |
| 操作作废单 | ❌ | ❌ |
| 删除扫描记录 | ✅ | ❌ |
