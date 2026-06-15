# 仓储管理系统阶段一：登录 + 布局框架 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建 Vue 3 + Spring Boot 仓储管理系统骨架，完成 JWT 登录打通和侧边栏菜单-标签页联动布局。

**Architecture:** 前后端分离。后端 Spring Boot 3 + MyBatis-Plus + Spring Security + JWT 提供 RESTful API。前端 Vue 3 + Element Plus + Pinia 实现 SPA，开发期通过 Vite proxy 转发 API 请求。路由守卫拦截未登录请求，Axios 拦截器自动携带 token。

**Tech Stack:** Vue 3, TypeScript, Vite, Element Plus, Pinia, Vue Router 4, Axios / Spring Boot 3.2, JDK 17, MyBatis-Plus 3.5, Spring Security, jjwt 0.12, MySQL 8.0

**MySQL:** root / 1234 @ localhost:3306

---

## 文件结构

```
warehouse_manage/
├── warehouse-backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/warehouse/
│       │   ├── WarehouseApplication.java          # 启动类
│       │   ├── config/
│       │   │   ├── SecurityConfig.java            # Spring Security + JWT 过滤器配置
│       │   │   └── CorsConfig.java                # 跨域配置
│       │   ├── controller/
│       │   │   └── AuthController.java            # POST /api/auth/login, GET /api/user/info
│       │   ├── model/
│       │   │   ├── entity/User.java               # 用户 POJO
│       │   │   └── dto/
│       │   │       ├── LoginRequest.java          # 登录请求 DTO
│       │   │       └── Result.java                # 统一响应包装
│       │   ├── mapper/
│       │   │   └── UserMapper.java                # MyBatis-Plus BaseMapper
│       │   ├── security/
│       │   │   ├── JwtTokenProvider.java          # JWT 生成与验证
│       │   │   └── JwtAuthenticationFilter.java   # OncePerRequestFilter
│       │   └── service/
│       │       └── UserService.java               # 用户业务逻辑
│       └── resources/
│           ├── application.yml
│           └── sql/
│               └── init.sql                       # 建库建表 + 初始数据
│
└── warehouse-frontend/
    ├── index.html
    ├── package.json
    ├── tsconfig.json
    ├── tsconfig.app.json
    ├── tsconfig.node.json
    ├── vite.config.ts
    └── src/
        ├── main.ts                                # 入口：注册 Element Plus / Pinia / Router
        ├── App.vue                                 # 顶层组件 <router-view />
        ├── env.d.ts                                # TypeScript 声明
        ├── api/
        │   ├── request.ts                          # Axios 实例 + 拦截器
        │   └── auth.ts                             # login(), getUserInfo()
        ├── layout/
        │   ├── MainLayout.vue                      # el-container 主布局骨架
        │   ├── SideMenu.vue                        # el-menu 侧边栏
        │   └── TabBar.vue                          # el-tabs 标签页栏
        ├── router/
        │   └── index.ts                            # 路由配置 + beforeEach 守卫
        ├── stores/
        │   ├── auth.ts                             # token / 用户状态
        │   └── tabs.ts                             # 标签页状态
        └── views/
            ├── Login.vue                           # 登录页
            ├── Dashboard.vue                       # 首页（占位）
            ├── warehouse/
            │   ├── List.vue                        # 仓库列表（占位）
            │   └── Area.vue                        # 库区管理（占位）
            ├── inventory/
            │   ├── Inbound.vue                     # 入库管理（占位）
            │   └── Outbound.vue                    # 出库管理（占位）
            └── system/
                └── User.vue                        # 用户管理（占位）
```

---

## 任务列表

### Task 1: 创建后端项目结构 + pom.xml

**Files:**
- Create: `warehouse-backend/pom.xml`
- Create: `warehouse-backend/src/main/java/com/warehouse/` (空目录结构)
- Create: `warehouse-backend/src/main/resources/` (空目录结构)

- [ ] **Step 1: 创建 Maven pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.warehouse</groupId>
    <artifactId>warehouse-backend</artifactId>
    <version>1.0.0</version>
    <name>warehouse-backend</name>
    <description>仓储管理系统后端</description>

    <properties>
        <java.version>17</java.version>
        <jjwt.version>0.12.5</jjwt.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>3.5.7</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建目录结构**

```bash
mkdir -p warehouse-backend/src/main/java/com/warehouse/{config,controller,model/{entity,dto},mapper,security,service}
mkdir -p warehouse-backend/src/main/resources/sql
```

---

### Task 2: 配置 application.yml + 数据库初始化 SQL

**Files:**
- Create: `warehouse-backend/src/main/resources/application.yml`
- Create: `warehouse-backend/src/main/resources/sql/init.sql`

- [ ] **Step 1: 编写 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/warehouse_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
    username: root
    password: 1234
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

jwt:
  secret: d2FyZWhvdXNlLWp3dC1zZWNyZXQta2V5LWZvci1oczI1Ni1hbGdvcml0aG0tMTIzNDU2Nzg5MA==
  expiration: 86400000
```

- [ ] **Step 2: 编写数据库初始化 SQL**

```sql
-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS warehouse_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE warehouse_db;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    enabled     TINYINT(1)   DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 初始化管理员账号: admin / admin123
-- BCrypt 加密后的密码（admin123 经过 BCrypt 编码）
INSERT INTO sys_user (username, password, nickname) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员')
ON DUPLICATE KEY UPDATE username = username;
```

- [ ] **Step 3: 执行 SQL 初始化数据库**

```bash
# 在 MySQL 客户端执行或用命令行导入
mysql -u root -p1234 < warehouse-backend/src/main/resources/sql/init.sql
```

---

### Task 3: 创建 User 实体 + LoginRequest DTO + Result 响应类

**Files:**
- Create: `warehouse-backend/src/main/java/com/warehouse/model/entity/User.java`
- Create: `warehouse-backend/src/main/java/com/warehouse/model/dto/LoginRequest.java`
- Create: `warehouse-backend/src/main/java/com/warehouse/model/dto/Result.java`

- [ ] **Step 1: 编写 User.java**

```java
package com.warehouse.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private Boolean enabled;
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 编写 LoginRequest.java**

```java
package com.warehouse.model.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
```

- [ ] **Step 3: 编写 Result.java**

```java
package com.warehouse.model.dto;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
```

---

### Task 4: 创建 UserMapper

**Files:**
- Create: `warehouse-backend/src/main/java/com/warehouse/mapper/UserMapper.java`

- [ ] **Step 1: 编写 UserMapper.java**

```java
package com.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.warehouse.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

---

### Task 5: 创建 UserService

**Files:**
- Create: `warehouse-backend/src/main/java/com/warehouse/service/UserService.java`

- [ ] **Step 1: 编写 UserService.java**

```java
package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.UserMapper;
import com.warehouse.model.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public User login(String username, String password) {
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            return null;
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }
        return user;
    }

    public User getById(Long id) {
        return userMapper.selectById(id);
    }
}
```

---

### Task 6: 创建 JwtTokenProvider

**Files:**
- Create: `warehouse-backend/src/main/java/com/warehouse/security/JwtTokenProvider.java`

- [ ] **Step 1: 编写 JwtTokenProvider.java**

```java
package com.warehouse.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Long userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getKey())
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("username", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

### Task 7: 创建 JwtAuthenticationFilter

**Files:**
- Create: `warehouse-backend/src/main/java/com/warehouse/security/JwtAuthenticationFilter.java`

- [ ] **Step 1: 编写 JwtAuthenticationFilter.java**

```java
package com.warehouse.security;

import com.warehouse.model.entity.User;
import com.warehouse.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            User user = userService.getById(userId);

            if (user != null && user.getEnabled()) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user, null, Collections.emptyList());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

---

### Task 8: 创建 SecurityConfig

**Files:**
- Create: `warehouse-backend/src/main/java/com/warehouse/config/SecurityConfig.java`

- [ ] **Step 1: 编写 SecurityConfig.java**

```java
package com.warehouse.config;

import com.warehouse.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

### Task 9: 创建 CorsConfig

**Files:**
- Create: `warehouse-backend/src/main/java/com/warehouse/config/CorsConfig.java`

- [ ] **Step 1: 编写 CorsConfig.java**

```java
package com.warehouse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
```

---

### Task 10: 创建 AuthController

**Files:**
- Create: `warehouse-backend/src/main/java/com/warehouse/controller/AuthController.java`

- [ ] **Step 1: 编写 AuthController.java**

```java
package com.warehouse.controller;

import com.warehouse.model.dto.LoginRequest;
import com.warehouse.model.dto.Result;
import com.warehouse.model.entity.User;
import com.warehouse.security.JwtTokenProvider;
import com.warehouse.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/auth/login")
    public Result<?> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        if (user == null) {
            return Result.error(401, "用户名或密码错误");
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("nickname", user.getNickname());
        return Result.ok(data);
    }

    @GetMapping("/user/info")
    public Result<?> getUserInfo(@RequestAttribute(name = "userId", required = false) Long userId) {
        // 从 SecurityContext 获取当前用户
        User user = (User) org.springframework.security.core.context
                .SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        return Result.ok(data);
    }
}
```

---

### Task 11: 创建启动类 WarehouseApplication

**Files:**
- Create: `warehouse-backend/src/main/java/com/warehouse/WarehouseApplication.java`

- [ ] **Step 1: 编写 WarehouseApplication.java**

```java
package com.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WarehouseApplication {
    public static void main(String[] args) {
        SpringApplication.run(WarehouseApplication.class, args);
    }
}
```

- [ ] **Step 2: 编译验证后端项目**

```bash
cd warehouse-backend
mvn compile
```

Expected: `BUILD SUCCESS`

---

### Task 12: 初始化前端项目

**Files:**
- Create: `warehouse-frontend/` 完整项目结构

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "warehouse-frontend",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.21",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.7",
    "axios": "^1.6.8",
    "element-plus": "^2.7.0",
    "@element-plus/icons-vue": "^2.3.1"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.4",
    "typescript": "~5.4.0",
    "vite": "^5.2.0",
    "vue-tsc": "^2.0.6",
    "@types/node": "^20.12.0"
  }
}
```

- [ ] **Step 2: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>仓储管理系统</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

- [ ] **Step 3: 创建 tsconfig.json**

```json
{
  "files": [],
  "references": [
    { "path": "./tsconfig.app.json" },
    { "path": "./tsconfig.node.json" }
  ]
}
```

- [ ] **Step 4: 创建 tsconfig.app.json**

```json
{
  "compilerOptions": {
    "composite": true,
    "target": "ES2020",
    "useDefineForExpose": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": false,
    "noUnusedParameters": false,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.tsx", "src/**/*.vue", "src/env.d.ts"]
}
```

- [ ] **Step 5: 创建 tsconfig.node.json**

```json
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
```

- [ ] **Step 6: 创建 src/env.d.ts**

```ts
/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
```

- [ ] **Step 7: 创建目录结构 + 安装依赖**

```bash
mkdir -p warehouse-frontend/src/{api,layout,router,stores,views/{warehouse,inventory,system}}
cd warehouse-frontend
npm install
```

---

### Task 13: 创建 Axios 实例 + Auth API

**Files:**
- Create: `warehouse-frontend/src/api/request.ts`
- Create: `warehouse-frontend/src/api/auth.ts`

- [ ] **Step 1: 编写 request.ts**

```ts
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    const msg = error.response?.data?.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
```

- [ ] **Step 2: 编写 auth.ts**

```ts
import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  nickname: string
}

export interface UserInfo {
  username: string
  nickname: string
}

export function loginApi(params: LoginParams): Promise<{
  code: number
  message: string
  data: LoginResult
}> {
  return request.post('/auth/login', params)
}

export function getUserInfoApi(): Promise<{
  code: number
  message: string
  data: UserInfo
}> {
  return request.get('/user/info')
}
```

---

### Task 14: 创建 Pinia Stores

**Files:**
- Create: `warehouse-frontend/src/stores/auth.ts`
- Create: `warehouse-frontend/src/stores/tabs.ts`

- [ ] **Step 1: 编写 auth.ts**

```ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, type LoginParams } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const nickname = ref('')

  async function login(params: LoginParams): Promise<boolean> {
    try {
      const res = await loginApi(params)
      token.value = res.data.token
      nickname.value = res.data.nickname
      localStorage.setItem('token', res.data.token)
      return true
    } catch {
      return false
    }
  }

  function logout() {
    token.value = ''
    nickname.value = ''
    localStorage.removeItem('token')
  }

  return { token, nickname, login, logout }
})
```

- [ ] **Step 2: 编写 tabs.ts**

```ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

export interface Tab {
  path: string
  title: string
  icon?: string
  closable: boolean
}

export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref<Tab[]>([
    {
      path: '/dashboard',
      title: '首页',
      icon: 'HomeFilled',
      closable: false,
    },
  ])
  const activeTab = ref('/dashboard')

  function addTab(route: RouteLocationNormalized) {
    const existing = tabs.value.find((t) => t.path === route.path)
    if (existing) {
      activeTab.value = existing.path
      return
    }
    tabs.value.push({
      path: route.path,
      title: (route.meta?.title as string) || (route.name as string),
      icon: route.meta?.icon as string,
      closable: true,
    })
    activeTab.value = route.path
  }

  function removeTab(path: string) {
    const idx = tabs.value.findIndex((t) => t.path === path)
    if (idx === -1) return
    tabs.value.splice(idx, 1)
    if (activeTab.value === path) {
      const next = tabs.value[Math.min(idx, tabs.value.length - 1)]
      activeTab.value = next?.path || '/dashboard'
    }
  }

  return { tabs, activeTab, addTab, removeTab }
})
```

---

### Task 15: 创建路由

**Files:**
- Create: `warehouse-frontend/src/router/index.ts`

- [ ] **Step 1: 编写 router/index.ts**

```ts
import { createRouter, createWebHistory } from 'vue-router'
import { useTabsStore } from '@/stores/tabs'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { title: '首页', icon: 'HomeFilled' },
        },
        {
          path: 'warehouse/list',
          name: 'WarehouseList',
          component: () => import('@/views/warehouse/List.vue'),
          meta: { title: '仓库列表', icon: 'List' },
        },
        {
          path: 'warehouse/area',
          name: 'WarehouseArea',
          component: () => import('@/views/warehouse/Area.vue'),
          meta: { title: '库区管理', icon: 'Grid' },
        },
        {
          path: 'inventory/inbound',
          name: 'Inbound',
          component: () => import('@/views/inventory/Inbound.vue'),
          meta: { title: '入库管理', icon: 'Download' },
        },
        {
          path: 'inventory/outbound',
          name: 'Outbound',
          component: () => import('@/views/inventory/Outbound.vue'),
          meta: { title: '出库管理', icon: 'Upload' },
        },
        {
          path: 'system/user',
          name: 'SystemUser',
          component: () => import('@/views/system/User.vue'),
          meta: { title: '用户管理', icon: 'User' },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') {
    next('/login')
  } else if (token && to.path === '/login') {
    next('/')
  } else {
    // 如果是子路由且已登录，添加到标签页
    if (token && to.matched.length > 1) {
      const tabsStore = useTabsStore()
      tabsStore.addTab(to)
    }
    next()
  }
})

export default router
```

---

### Task 16: 创建布局组件

**Files:**
- Create: `warehouse-frontend/src/layout/MainLayout.vue`
- Create: `warehouse-frontend/src/layout/SideMenu.vue`
- Create: `warehouse-frontend/src/layout/TabBar.vue`

- [ ] **Step 1: 编写 MainLayout.vue**

```vue
<template>
  <el-container class="main-layout">
    <el-aside width="220px">
      <SideMenu />
    </el-aside>
    <el-container>
      <el-header height="auto" style="padding: 0">
        <TabBar />
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import SideMenu from './SideMenu.vue'
import TabBar from './TabBar.vue'
</script>

<style scoped>
.main-layout {
  height: 100vh;
}
.el-aside {
  background-color: #304156;
  overflow: hidden;
}
.el-main {
  background-color: #f0f2f5;
  padding: 16px;
}
</style>
```

- [ ] **Step 2: 编写 SideMenu.vue**

```vue
<template>
  <div class="side-menu">
    <div class="logo">📦 仓储管理系统</div>
    <el-menu
      :default-active="activeMenu"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409EFF"
      router
    >
      <el-menu-item index="/dashboard">
        <el-icon><HomeFilled /></el-icon>
        <span>首页</span>
      </el-menu-item>

      <el-sub-menu index="warehouse">
        <template #title>
          <el-icon><Box /></el-icon>
          <span>仓库管理</span>
        </template>
        <el-menu-item index="/warehouse/list">
          <el-icon><List /></el-icon>
          <span>仓库列表</span>
        </el-menu-item>
        <el-menu-item index="/warehouse/area">
          <el-icon><Grid /></el-icon>
          <span>库区管理</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="inventory">
        <template #title>
          <el-icon><Folder /></el-icon>
          <span>库存管理</span>
        </template>
        <el-menu-item index="/inventory/inbound">
          <el-icon><Download /></el-icon>
          <span>入库管理</span>
        </el-menu-item>
        <el-menu-item index="/inventory/outbound">
          <el-icon><Upload /></el-icon>
          <span>出库管理</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="system">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </template>
        <el-menu-item index="/system/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  HomeFilled,
  Box,
  List,
  Grid,
  Folder,
  Download,
  Upload,
  Setting,
  User,
} from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped>
.side-menu {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  white-space: nowrap;
  overflow: hidden;
}
.el-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}
</style>
```

- [ ] **Step 3: 编写 TabBar.vue**

```vue
<template>
  <div class="tab-bar">
    <el-tabs
      v-model="tabsStore.activeTab"
      type="card"
      class="tab-tabs"
      @tab-click="handleTabClick"
      @tab-remove="handleTabRemove"
    >
      <el-tab-pane
        v-for="tab in tabsStore.tabs"
        :key="tab.path"
        :name="tab.path"
        :closable="tab.closable"
      >
        <template #label>
          <span class="tab-label">
            <el-icon v-if="tab.icon" class="tab-icon">
              <component :is="tab.icon" />
            </el-icon>
            {{ tab.title }}
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <div class="tab-actions">
      <el-dropdown trigger="click" @command="handleDropdownCommand">
        <span class="dropdown-trigger">
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="closeCurrent">关闭当前</el-dropdown-item>
            <el-dropdown-item command="closeOthers">关闭其它</el-dropdown-item>
            <el-dropdown-item command="closeAll">关闭所有</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useTabsStore } from '@/stores/tabs'
import { ArrowDown } from '@element-plus/icons-vue'
import type { TabsPaneContext } from 'element-plus'

const router = useRouter()
const tabsStore = useTabsStore()

function handleTabClick(pane: TabsPaneContext) {
  const path = pane.paneName as string
  router.push(path)
}

function handleTabRemove(path: string) {
  tabsStore.removeTab(path)
  if (tabsStore.activeTab !== path) {
    router.push(tabsStore.activeTab)
  }
}

function handleDropdownCommand(command: string) {
  const currentPath = tabsStore.activeTab
  const closableTabs = tabsStore.tabs.filter((t) => t.closable)

  switch (command) {
    case 'closeCurrent':
      if (currentPath !== '/dashboard') {
        tabsStore.removeTab(currentPath)
        router.push(tabsStore.activeTab)
      }
      break
    case 'closeOthers':
      tabsStore.tabs = tabsStore.tabs.filter(
        (t) => !t.closable || t.path === currentPath
      )
      tabsStore.activeTab = currentPath
      break
    case 'closeAll':
      tabsStore.tabs = tabsStore.tabs.filter((t) => !t.closable)
      tabsStore.activeTab = '/dashboard'
      router.push('/dashboard')
      break
  }
}
</script>

<style scoped>
.tab-bar {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding-right: 8px;
}
.tab-tabs {
  flex: 1;
}
.tab-tabs :deep(.el-tabs__header) {
  margin: 0;
  border-bottom: none;
}
.tab-tabs :deep(.el-tabs__nav) {
  border: none !important;
}
.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.tab-icon {
  font-size: 14px;
}
.tab-actions {
  flex-shrink: 0;
  padding: 0 8px;
}
.dropdown-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  cursor: pointer;
  border-radius: 4px;
}
.dropdown-trigger:hover {
  background-color: #f0f2f5;
}
</style>
```

---

### Task 17: 创建页面组件

**Files:**
- Create: `warehouse-frontend/src/views/Login.vue`
- Create: `warehouse-frontend/src/views/Dashboard.vue`
- Create: `warehouse-frontend/src/views/warehouse/List.vue`
- Create: `warehouse-frontend/src/views/warehouse/Area.vue`
- Create: `warehouse-frontend/src/views/inventory/Inbound.vue`
- Create: `warehouse-frontend/src/views/inventory/Outbound.vue`
- Create: `warehouse-frontend/src/views/system/User.vue`

- [ ] **Step 1: 编写 Login.vue**

```vue
<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="login-title">仓储管理系统</h2>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  username: 'admin',
  password: 'admin123',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  const success = await authStore.login({
    username: form.username,
    password: form.password,
  })
  loading.value = false

  if (success) {
    ElMessage.success('登录成功')
    router.push('/')
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}
.login-title {
  text-align: center;
  margin-bottom: 32px;
  font-size: 24px;
  color: #303133;
}
</style>
```

- [ ] **Step 2: 编写 Dashboard.vue（首页占位）**

```vue
<template>
  <div class="page-container">
    <h2>📊 首页</h2>
    <p>欢迎使用仓储管理系统</p>
  </div>
</template>

<script setup lang="ts">
</script>
```

- [ ] **Step 3: 编写仓库列表占位页 (warehouse/List.vue)**

```vue
<template>
  <div class="page-container">
    <h2>📋 仓库列表</h2>
    <p>仓库列表功能开发中...</p>
  </div>
</template>

<script setup lang="ts">
</script>
```

- [ ] **Step 4: 编写库区管理占位页 (warehouse/Area.vue)**

```vue
<template>
  <div class="page-container">
    <h2>🔲 库区管理</h2>
    <p>库区管理功能开发中...</p>
  </div>
</template>

<script setup lang="ts">
</script>
```

- [ ] **Step 5: 编写入库管理占位页 (inventory/Inbound.vue)**

```vue
<template>
  <div class="page-container">
    <h2>📥 入库管理</h2>
    <p>入库管理功能开发中...</p>
  </div>
</template>

<script setup lang="ts">
</script>
```

- [ ] **Step 6: 编写出库管理占位页 (inventory/Outbound.vue)**

```vue
<template>
  <div class="page-container">
    <h2>📤 出库管理</h2>
    <p>出库管理功能开发中...</p>
  </div>
</template>

<script setup lang="ts">
</script>
```

- [ ] **Step 7: 编写用户管理占位页 (system/User.vue)**

```vue
<template>
  <div class="page-container">
    <h2>👤 用户管理</h2>
    <p>用户管理功能开发中...</p>
  </div>
</template>

<script setup lang="ts">
</script>
```

- [ ] **Step 8: 编写全局样式**

没有专门的 CSS 文件——全局样式通过 `App.vue` 的 style 注入，页面共用 `.page-container` 类在各组件中通过 scoped style 或全局注入。在 `main.ts` 中我们会为 body 设置基础样式。

---

### Task 18: 创建 App.vue + main.ts + vite.config.ts

**Files:**
- Create: `warehouse-frontend/src/App.vue`
- Create: `warehouse-frontend/src/main.ts`
- Create: `warehouse-frontend/vite.config.ts`

- [ ] **Step 1: 编写 App.vue**

```vue
<template>
  <router-view />
</template>

<script setup lang="ts">
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
html,
body,
#app {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', Arial, sans-serif;
}
.page-container {
  padding: 0;
}
.page-container h2 {
  font-size: 20px;
  margin-bottom: 12px;
}
.page-container p {
  color: #909399;
}
</style>
```

- [ ] **Step 2: 编写 main.ts**

```ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
```

- [ ] **Step 3: 编写 vite.config.ts**

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

---

### Task 19: 编译验证前端项目

- [ ] **Step 1: 安装依赖**

```bash
cd warehouse-frontend
npm install
```

- [ ] **Step 2: 编译检查**

```bash
cd warehouse-frontend
npx vue-tsc -b --noEmit 2>&1 || npx vite build
```

Expected: 无编译错误（或仅有 minor 类型警告可忽略）

---

### Task 20: 启动并验证完整流程

- [ ] **Step 1: 启动后端**

```bash
cd warehouse-backend
mvn spring-boot:run
```

Expected: `Started WarehouseApplication in X.XXX seconds`

- [ ] **Step 2: 测试登录 API（新终端）**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Expected: 返回 JSON 包含 `code: 200` 和 `data.token`

- [ ] **Step 3: 启动前端（新终端）**

```bash
cd warehouse-frontend
npm run dev
```

Expected: `VITE ready in Xms → http://localhost:5173/`

- [ ] **Step 4: 浏览器验证完整流程**

打开 `http://localhost:5173`，依次验证：
1. 未登录时自动跳转 `/login`
2. 输入 admin / admin123 点击登录
3. 登录成功跳转到首页，左侧菜单高亮"首页"
4. 点击"仓库列表"→ 新标签页打开，菜单高亮联动
5. 点击"入库管理"→ 标签页正常切换
6. 关闭某个标签 → 自动激活相邻标签
7. 刷新页面 → 保持在当前页面（token 有效）
8. 点击"关闭所有" → 回到首页标签

---

## 完成标准

- [x] （未开始）后端编译通过，启动成功
- [x] （未开始）前端编译通过，启动成功
- [x] （未开始）`POST /api/auth/login` 返回 JWT token
- [x] （未开始）未登录访问任何页面重定向到 `/login`
- [x] （未开始）登录成功进入主布局，显示侧边栏+标签页
- [x] （未开始）点击菜单项打开/激活对应标签页，菜单高亮联动
- [x] （未开始）标签页可关闭，首页标签不可关闭
- [x] （未开始）刷新页面保持登录状态
