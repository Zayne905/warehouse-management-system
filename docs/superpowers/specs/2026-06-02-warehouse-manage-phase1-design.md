# 仓储管理系统 — 阶段一：登录 + 布局框架 设计文档

> 日期：2026-06-02  
> 状态：待审阅

---

## 1. 目标

打通 Vue 3 + Element Plus 前端与 Spring Boot 后端的 JWT 登录流程，并构建经典的"左侧菜单 + 顶部标签页"后台管理布局框架。菜单项点击后显示占位页面，业务功能在后续阶段填充。

---

## 2. 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端框架 | Vue 3 + TypeScript + Vite | SPA 开发框架 |
| UI 组件库 | Element Plus | 后台管理 UI |
| 路由 | Vue Router 4 | 前端路由 + 导航守卫 |
| 状态管理 | Pinia | 用户状态、标签页状态 |
| HTTP 客户端 | Axios | 请求封装 + 拦截器 |
| 后端框架 | Spring Boot 3.x + JDK 17 | RESTful API |
| 安全 | Spring Security + JWT | 无状态认证 |
| ORM | MyBatis-Plus 3.5 | 数据访问 |
| 数据库 | MySQL 8.0 | 持久化存储 |

---

## 3. 项目结构

```
warehouse_manage/
├── warehouse-frontend/          # 前端独立项目
│   ├── src/
│   │   ├── api/
│   │   │   ├── request.ts       # Axios 实例 + 拦截器
│   │   │   └── auth.ts          # 登录 API
│   │   ├── layout/
│   │   │   ├── MainLayout.vue   # 主布局容器
│   │   │   ├── SideMenu.vue     # 侧边栏菜单
│   │   │   └── TabBar.vue       # 顶部标签页栏
│   │   ├── router/
│   │   │   └── index.ts         # 路由 + beforeEach 守卫
│   │   ├── stores/
│   │   │   ├── auth.ts          # token / 用户信息
│   │   │   └── tabs.ts          # 标签页列表
│   │   ├── views/
│   │   │   ├── Login.vue        # 登录页
│   │   │   ├── Dashboard.vue    # 首页（占位）
│   │   │   ├── warehouse/       # 仓库管理（占位）
│   │   │   ├── inventory/       # 库存管理（占位）
│   │   │   └── system/          # 系统管理（占位）
│   │   ├── App.vue
│   │   └── main.ts
│   ├── index.html
│   ├── vite.config.ts
│   └── package.json
│
└── warehouse-backend/           # 后端独立项目
    └── src/main/
        ├── java/com/warehouse/
        │   ├── WarehouseApplication.java
        │   ├── config/
        │   │   ├── SecurityConfig.java
        │   │   └── CorsConfig.java
        │   ├── controller/
        │   │   └── AuthController.java
        │   ├── model/
        │   │   ├── entity/User.java
        │   │   └── dto/LoginRequest.java
        │   ├── mapper/
        │   │   └── UserMapper.java
        │   ├── security/
        │   │   ├── JwtTokenProvider.java
        │   │   └── JwtAuthenticationFilter.java
        │   └── service/
        │       └── UserService.java
        └── resources/
            ├── application.yml
            └── mapper/
                └── UserMapper.xml
```

---

## 4. 数据库

```sql
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `enabled`     TINYINT(1)   DEFAULT 1 COMMENT '是否启用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化管理员账号: admin / admin123
INSERT INTO sys_user (username, password, nickname) VALUES ('admin', '{bcrypt_hash}', '管理员');
```

---

## 5. API 设计

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录，返回 JWT | 否 |
| GET  | `/api/user/info`  | 获取当前用户信息 | 是 |

**POST /api/auth/login**

```
Request:  { "username": "admin", "password": "admin123" }
Response: { "code": 200, "message": "success", "data": { "token": "xxx", "nickname": "管理员" } }
Error:    { "code": 401, "message": "用户名或密码错误" }
```

---

## 6. 核心交互流程

### 6.1 登录流程

```
Login.vue → POST /api/auth/login
  ├─ 成功 → localStorage.set('token') → router.push('/')
  └─ 失败 → ElMessage.error('用户名或密码错误')
```

### 6.2 路由守卫

```
router.beforeEach(to, from, next)
  ├─ token 存在 + 目标 /login → redirect /
  ├─ token 存在 + 其他路由 → next()
  └─ token 不存在 + 非 /login → redirect /login
```

### 6.3 HTTP 拦截器

```
Axios 请求拦截器:
  └─ headers.Authorization = `Bearer ${localStorage.token}`

Axios 响应拦截器:
  ├─ 200 → 返回 data
  └─ 401 → 清除 token → router.push('/login')
```

### 6.4 菜单-标签页联动

```
点击侧边栏菜单项:
  tabsStore.addTab({ path, title, icon })
    ├─ tab 已存在 → 激活该 tab
    └─ tab 不存在 → push 到 tabs 数组 → 激活

关闭标签:
  tabsStore.removeTab(key)
    ├─ 关闭的是当前激活 tab → 激活相邻 tab
    └─ 最后一个 tab 不可关闭（首页）

路由变化 → 同步:
  └─ watch(route) → 同步 tabsStore.activeTab 和菜单高亮
```

### 6.5 标签页状态（Pinia Store）

```ts
interface Tab {
  path: string       // 路由路径
  title: string      // 标签标题
  icon?: string      // 图标
  closable: boolean  // 是否可关闭（首页不可关闭）
}

interface TabsState {
  tabs: Tab[]
  activeTab: string  // 当前激活的 path
}
```

---

## 7. 菜单结构（阶段一）

```
🏠 首页            → /dashboard
📦 仓库管理
   ├── 仓库列表    → /warehouse/list
   └── 库区管理    → /warehouse/area
📋 库存管理
   ├── 入库管理    → /inventory/inbound
   └── 出库管理    → /inventory/outbound
👤 系统管理
   └── 用户管理    → /system/user
```

所有目标页面在阶段一均为占位组件，显示页面标题即可。

---

## 8. 关键配置

### 8.1 Vite 开发代理（解决跨域）

```ts
// vite.config.ts
server: {
  proxy: {
    '/api': 'http://localhost:8080'
  }
}
```

### 8.2 Spring Boot CORS（生产环境）

```java
// CorsConfig.java — 允许前端域名跨域访问
registry.addMapping("/api/**")
    .allowedOrigins("http://localhost:5173")
    .allowedMethods("*")
    .allowedHeaders("*");
```

### 8.3 JWT 配置

```
jwt.secret: 256-bit random key
jwt.expiration: 86400000 (24小时)
```

---

## 9. 阶段一不包含

- 用户注册 / 修改密码
- 角色与权限（RBAC）
- 业务 CRUD 功能
- 操作日志 / 审计
- 文件上传
- 数据导入导出

以上内容在后续阶段补充。
