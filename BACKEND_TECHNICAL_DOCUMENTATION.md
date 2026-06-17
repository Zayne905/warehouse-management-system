# 仓储管理系统后端技术实现文档

## 1. 文档说明

本文档基于 `warehouse-backend` 当前代码编写，说明零件、供应商、库区、入库、扫码、库存、看板、出库、转包和生命周期追溯等功能的后端实现。

后端项目目录：

```text
warehouse-backend/
├─ pom.xml
└─ src/main/
   ├─ java/com/warehouse/
   │  ├─ config/       配置与异常处理
   │  ├─ controller/   HTTP 接口
   │  ├─ mapper/       MyBatis-Plus 数据访问
   │  ├─ model/
   │  │  ├─ dto/       请求和响应对象
   │  │  ├─ entity/    数据库实体
   │  │  └─ enums/     业务状态枚举
   │  ├─ security/     JWT 鉴权
   │  └─ service/      核心业务逻辑
   └─ resources/
      ├─ application.yml
      └─ sql/          初始化和升级脚本
```

## 2. 技术架构

| 类型 | 技术 | 当前版本或实现 |
|---|---|---|
| 开发语言 | Java | Java 17 |
| Web 框架 | Spring Boot | 3.2.5 |
| 安全框架 | Spring Security | JWT 无状态认证 |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | `warehouse_db` |
| Token | JJWT | 0.12.5 |
| 密码加密 | BCrypt | Spring Security |
| 构建工具 | Maven | Spring Boot Maven Plugin |
| 简化模型 | Lombok | 实体和 DTO |

系统采用标准分层结构：

```text
Web/Android
    ↓ HTTP + JWT
Controller
    ↓
Service（事务、校验、状态流转）
    ↓
Mapper（MyBatis-Plus）
    ↓
MySQL
```

所有业务接口统一使用 `/api` 前缀，默认监听 `8081` 端口。

## 3. 通用响应与异常处理

接口通过 `Result<T>` 返回统一结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页接口使用 `PageResult<T>`，包含记录、总数、当前页和每页数量。

`GlobalExceptionHandler` 负责捕获业务异常和系统异常，将异常转换为统一 JSON 响应。Service 中通过抛出 `RuntimeException` 阻止不合法的业务操作，例如：

- 入库单不存在；
- 零件不属于所选供应商；
- 看板状态不允许出库；
- 封存看板不能参与出库；
- 转包数量超过可转数量；
- 已完成单据不能删除。

## 4. 登录与 JWT 鉴权

### 4.1 登录过程

登录入口：

```http
POST /api/auth/login
```

主要流程：

1. `AuthController` 接收入参 `LoginRequest`。
2. `UserService` 根据用户名查询 `sys_user`。
3. 使用 `BCryptPasswordEncoder` 校验密码。
4. `JwtTokenProvider` 生成 JWT。
5. 返回 Token、用户昵称和角色。

除登录接口外，其他接口均需要：

```http
Authorization: Bearer <token>
```

### 4.2 安全过滤链

`SecurityConfig` 使用无状态会话：

```java
SessionCreationPolicy.STATELESS
```

`JwtAuthenticationFilter` 在用户名密码过滤器之前执行，解析 Token 并将用户身份写入 Spring Security 上下文。

当前配置中数据库密码和 JWT 密钥直接写在 `application.yml`。生产部署时应改为环境变量，例如：

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
```

## 5. 核心数据模型

### 5.1 基础数据

| 实体 | 用途 |
|---|---|
| `User` | 登录用户、昵称、角色和密码 |
| `Supplier` | 供应商基础资料 |
| `Part` | 零件编码、名称、规格、单位、包装容量、默认库区 |
| `SupplierPart` | 供应商与零件的绑定关系 |
| `WarehouseArea` | 仓库内的库区或库位 |

`Part.packageCapacity` 是入库箱数和入库数量换算、看板最大箱容量及标签箱数显示的基础字段。

### 5.2 入库数据

| 实体 | 用途 |
|---|---|
| `InboundOrder` | 入库单主表 |
| `InboundOrderDetail` | 入库零件、计划数量、实入数量、箱数、库区、批次 |
| `ScanRecord` | 入库扫码历史 |
| `Kanban` | 每个实物箱对应的唯一看板 |

### 5.3 出库数据

| 实体 | 用途 |
|---|---|
| `OutboundOrder` | 出库单主表 |
| `OutboundOrderDetail` | 出库零件、计划数量、实出数量和箱数 |
| `OutboundScan` | 看板出库扫描历史 |

### 5.4 转包数据

| 实体 | 用途 |
|---|---|
| `RepackOrder` | 转包单主表 |
| `RepackOrderDetail` | 源看板、目标看板和转移数量 |
| `RepackRelation` | 父看板与子看板的生命周期关系 |

## 6. 零件与基础数据

### 6.1 零件管理

主要接口：

```http
GET  /api/part/list
POST /api/part/save
POST /api/part/delete
```

`PartService` 负责：

- 查询全部零件；
- 根据供应商查询已绑定零件；
- 保存新增或编辑零件；
- 保存零件默认库区、包装容量和供应商关系；
- 删除零件。

入库单选择供应商后，前端调用：

```http
GET /api/part/list?supplierId={id}
```

后端只返回该供应商可使用的零件，入库保存时还会通过 `SupplierPartService` 再次校验，防止绕过前端提交错误关系。

### 6.2 供应商和库区

```http
GET /api/supplier/list
GET /api/warehouse-area/list
```

库区 ID 会保存到入库明细和看板中，用于库存按库区统计和看板信息筛选。

## 7. 入库单实现

核心类：

```text
InboundOrderController
InboundOrderService
InboundOrderMapper
InboundOrderDetailMapper
```

### 7.1 新增和编辑

保存接口：

```http
POST /api/inbound-order/save
```

`InboundOrderService.save()` 使用 `@Transactional` 保证主表、明细和看板创建属于同一事务。

新增流程：

1. 校验供应商存在。
2. 生成入库单号。
3. 插入 `InboundOrder`。
4. 校验明细中不能有重复零件。
5. 校验零件属于当前供应商。
6. 保存 `InboundOrderDetail`。
7. 调用 `KanbanService.generateForOrder()` 生成待入库看板。

编辑流程会先保存原有实入数量和批次号，再重建明细和待入库看板，避免编辑计划数据时丢失已经发生的扫码结果。

### 7.2 入库数量与箱数双向换算

前端同时提交：

```json
{
  "plannedQty": 75,
  "boxCount": 1.5
}
```

后端规则：

1. 优先保存用户输入的 `plannedQty`。
2. 如果未传箱数，则按 `plannedQty / packageCapacity` 计算箱数。
3. 如果未传数量但传了箱数，则按 `packageCapacity × boxCount` 计算数量。
4. 箱数保留两位小数。

例如包装容量为 `50`：

```text
输入 1.5 箱 → 计划入库 75
输入数量 75 → 箱数 1.5
```

### 7.3 看板生成

`KanbanService.generateForOrder()` 根据箱数向上取整生成标签：

```text
1.5 箱 → 生成 2 个看板
```

对于容量为 50、总数量为 75 的入库明细：

| 看板 | 实际数量 `quantity` | 最大箱容量 `originalQty` |
|---|---:|---:|
| 第 1 箱 | 50 | 50 |
| 第 2 箱 | 25 | 50 |

`originalQty` 始终保存零件包装容量，因此库存和标签可以显示：

```text
50/1箱
25/0.5箱
```

新创建的看板状态为 `STATUS_PENDING_INBOUND`，此时不会计入正式库存。

### 7.4 入库单状态

入库单主要状态：

| 状态值 | 含义 |
|---:|---|
| 0 | 未入库 |
| 1 | 部分入库 |
| 2 | 已入库 |
| 3 | 作废 |

`recalculateStatus()` 根据所有明细的计划数量与实入数量重新计算：

- 全部实入为 0：未入库；
- 部分完成：部分入库；
- 全部达到计划数量：已入库。

## 8. 扫码入库实现

核心类：

```text
ScanController
ScanService
ScanRecordMapper
```

主要接口：

```http
POST /api/scan/check-duplicate
POST /api/scan/submit
POST /api/scan/kanban
POST /api/scan/list
POST /api/scan/delete
POST /api/scan/feedback
```

### 8.1 看板扫码

Android 扫描二维码后提交 `KanbanScanDTO`：

```http
POST /api/scan/kanban
```

后端根据看板号查询 `Kanban`，校验：

- 看板是否存在；
- 看板是否属于当前入库单；
- 状态是否允许入库；
- 是否重复扫描；
- 扫描数量是否超过计划数量。

扫码成功后：

1. 插入 `ScanRecord`。
2. 累加入库明细 `actualQty`。
3. 将该看板状态改为“在库可用”。
4. 重新计算入库单状态。
5. 返回当前零件和整张入库单进度。

这保证了“创建入库单”只创建待入库看板，只有扫码或手动入库成功后才进入正式库存。

### 8.2 删除扫码记录

删除扫码记录时，后端会反向扣减明细实入数量，并重新计算入库单状态及相关看板状态，避免库存和入库历史不一致。

## 9. 库存总览实现

核心类：

```text
InventoryController
InventoryService
InventoryVO
```

接口：

```http
POST /api/inventory/stock-list
```

支持参数：

```json
{
  "keyword": "P001",
  "warehouseAreaId": 1
}
```

库存不是直接读取入库计划数量，而是从看板表聚合以下状态：

```text
在库可用
封存
部分转出
```

不计入库存的状态：

```text
待入库
待出库
已出库
已清空
```

聚合维度以零件为主，返回：

- 零件编码和名称；
- 包装容量；
- 在库总数量；
- 在库箱数；
- 平均每箱数量；
- 各库区数量；
- 可用、封存等状态数量。

即使某零件全部看板被封存，封存看板仍属于库存状态，因此零件条目不会从库存总览消失。

## 10. 看板管理与生命周期

核心类：

```text
KanbanController
KanbanService
KanbanMapper
```

### 10.1 看板状态

| 状态值 | 常量 | 含义 |
|---:|---|---|
| 0 | `STATUS_PENDING_INBOUND` | 待入库 |
| 1 | `STATUS_AVAILABLE` | 在库可用 |
| 2 | `STATUS_LOCKED` | 待出库 |
| 3 | `STATUS_OUTBOUND` | 已出库 |
| 4 | `STATUS_BLOCKED` | 封存 |
| 5 | `STATUS_PARTIAL_REPACK` | 部分转出 |
| 6 | `STATUS_CLEARED` | 已清空 |

### 10.2 看板列表

```http
POST /api/kanban/list
```

支持：

- 入库单号、出库单号、看板号、零件号模糊搜索；
- 状态筛选；
- 供应商筛选；
- 库区名称筛选。

空条件查询返回所有看板的简要信息。

### 10.3 生命周期查询

```http
POST /api/kanban/lifecycle
```

输入：

```json
{
  "kanbanNo": "R-2026-06-15-R20260615023-P003C-1"
}
```

`getLifecycle()` 聚合：

- 看板基础信息；
- 入库单和出库单；
- 零件、供应商、库区；
- 实际数量和最大箱容量；
- 创建时间；
- 入库扫描时间；
- 出库扫描时间；
- 转包父子关系；
- 生命周期事件时间线。

同一接口同时供 Web 看板详情和 Android 生命周期查询使用。

### 10.4 封存和解封

接口包括：

```http
POST /api/kanban/block
POST /api/kanban/unblock
POST /api/kanban/toggle-block
POST /api/kanban/batch-block
POST /api/kanban/batch-unblock
```

状态限制：

```text
在库可用 → 封存
封存 → 在库可用
```

批量操作只更新用户勾选的看板号，不再默认封存某零件的全部库存。

## 11. 出库管理与 FIFO

核心类：

```text
OutboundController
OutboundService
OutboundOrderMapper
OutboundOrderDetailMapper
OutboundScanMapper
```

### 11.1 出库单保存

```http
POST /api/outbound-order/save
```

保存出库主表和明细后，`autoMatchKanbans()` 自动匹配看板。

### 11.2 FIFO 自动匹配

匹配规则：

1. 按出库明细中的零件查询看板。
2. 只选择“在库可用”或“部分转出”的看板。
3. 按 `create_time` 升序排列，优先使用最早入库的看板。
4. 被匹配的看板改为 `STATUS_LOCKED`。
5. 写入 `outboundOrderId` 和 `outboundOrderNo`。

当出库数量小于整箱数量时，系统可拆出本次出库数量，并为剩余数量保留新的可用看板，从而支持非整箱出库。

### 11.3 扫码出库

```http
POST /api/outbound/scan
```

扫码时校验：

- 看板存在；
- 看板已被当前出库单锁定；
- 看板不是封存、待入库、已出库或已清空状态；
- 不允许重复出库。

成功后：

1. 插入 `OutboundScan`。
2. 看板状态改为“已出库”。
3. 更新出库明细实出数量。
4. 按 FIFO 扣减对应入库明细库存。
5. 更新出库单完成进度和状态。

取消或删除未完成出库单时，`releaseLockedKanbans()` 会把尚未扫描的锁定看板恢复为“在库可用”。

## 12. 转包管理

核心类：

```text
RepackController
RepackService
RepackOrderMapper
RepackOrderDetailMapper
RepackRelationMapper
```

主要接口：

```http
POST /api/repack/list
POST /api/repack/detail
POST /api/repack/save
POST /api/repack/add-detail
POST /api/repack/remove-detail
POST /api/repack/breakdown-generate
POST /api/repack/confirm
POST /api/repack/preview
POST /api/repack/trace
POST /api/repack/cancel
POST /api/repack/delete
```

### 12.1 转包校验

源看板必须处于：

```text
在库可用
部分转出
```

转移数量不能超过源看板当前剩余数量。

### 12.2 拆包

输入目标箱容量后，`breakdownAdd()` 自动计算目标箱数和每箱数量。

确认转包时：

1. 扣减源看板数量。
2. 源看板剩余为 0 时改为“已清空”。
3. 有剩余时改为“部分转出”。
4. 创建新的目标看板。
5. 目标看板状态为“在库可用”。
6. 写入 `RepackRelation` 父子关系。

### 12.3 合并与追溯

多个源看板可转移到目标看板。每次转包都记录：

- 转包单号；
- 父看板号；
- 子看板号；
- 转移数量；
- 转包时间。

`traceByKanban()` 根据父子关系查询看板的上游和下游链路，供 Web 和 Android 展示转包追溯结果。

## 13. 事务和数据一致性

以下核心修改方法使用 `@Transactional`：

- 入库单保存、删除、作废；
- 看板生成、封存和解封；
- 入库扫码及扫码记录删除；
- 出库单保存、FIFO 匹配、扫码出库；
- 转包明细修改和确认。

事务的作用是保证一次业务操作中的多表修改要么全部成功，要么全部回滚。

例如扫码出库同时涉及：

```text
OutboundScan
Kanban
OutboundOrderDetail
InboundOrderDetail
OutboundOrder
```

其中任一步失败，整个事务回滚，避免出现“看板已出库但库存未扣减”的中间状态。

## 14. 主要接口汇总

| 模块 | 接口 |
|---|---|
| 登录 | `/api/auth/login` |
| 用户信息 | `/api/user/info` |
| 供应商 | `/api/supplier/list` |
| 零件 | `/api/part/list`、`/api/part/save`、`/api/part/delete` |
| 库区 | `/api/warehouse-area/list` |
| 入库单 | `/api/inbound-order/list`、`save`、`submit`、`detail`、`delete`、`cancel` |
| 入库扫码 | `/api/scan/check-duplicate`、`submit`、`kanban`、`list`、`delete` |
| 库存 | `/api/inventory/stock-list` |
| 看板 | `/api/kanban/list`、`lifecycle`、`block`、`unblock`、批量封存/解封 |
| 出库单 | `/api/outbound-order/list`、`save`、`detail`、`delete`、`cancel` |
| 出库扫码 | `/api/outbound/scan` |
| 转包 | `/api/repack/list`、`save`、`confirm`、`preview`、`trace` |

## 15. 数据库脚本

脚本位于：

```text
warehouse-backend/src/main/resources/sql/
```

主要用途：

| 脚本 | 内容 |
|---|---|
| `init.sql` | 基础数据库初始化 |
| `V2__phase2_schema.sql` | 第二阶段表结构 |
| `V3__test_data.sql` | 测试数据 |
| `V4__packaging_and_area.sql` | 包装容量和库区 |
| `V5__kanban.sql` | 看板表 |
| `V6__scan_kanban.sql` | 扫码与看板关联 |
| `V7__outbound.sql` | 出库表结构 |
| `V8__outbound_refactor.sql` | 出库结构调整 |
| `V9__outbound_status_supplier.sql` | 出库状态和供应商 |
| `V10__repack.sql` | 转包表结构 |
| `V11__decimal_inventory_and_kanban_lifecycle.sql` | 小数箱数、看板生命周期相关字段 |

当前项目未配置 Flyway 自动迁移，脚本需要按照部署环境手动执行。

## 16. 构建和运行

### 16.1 环境要求

```text
JDK 17+
Maven 3.8+
MySQL 8
```

### 16.2 数据库配置

默认配置：

```text
地址：localhost:3306
数据库：warehouse_db
用户名：root
密码：123456
```

### 16.3 构建

```powershell
cd warehouse-backend
mvn clean package -DskipTests
```

### 16.4 启动

```powershell
java -jar target/warehouse-backend-1.0.0.jar
```

启动后接口地址：

```text
http://localhost:8081/api
```

## 17. 当前实现注意事项

1. `application.yml` 包含本地数据库密码和 JWT 密钥，公开仓库和生产环境应使用环境变量。
2. 当前 SQL 脚本由人工执行，建议后续引入 Flyway 管理版本。
3. 部分 Controller 使用 `Map<String, Object>` 接收参数，后续可改为明确 DTO，提升类型安全和接口文档质量。
4. 当前库存仓库名称使用逻辑默认值“主仓库”，实际筛选已按库区字段实现。
5. 建议增加入库、FIFO 出库、部分箱、封存及转包的集成测试。
6. 建议增加 OpenAPI/Swagger，自动生成可调试的接口文档。

## 18. 关键功能代码与注释

本节代码经过适度精简，保留现有实现中的关键判断。定位代码时可根据“文件位置”和“方法名”搜索。

### 18.1 JWT 接口保护

文件位置：

```text
warehouse-backend/src/main/java/com/warehouse/config/SecurityConfig.java
```

方法：`filterChain()`

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // 系统提供 REST API，不使用服务端页面表单，因此关闭 CSRF。
        .csrf(csrf -> csrf.disable())

        // JWT 本身携带用户身份，服务端不保存 Session。
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        .authorizeHttpRequests(auth -> auth
            // 登录接口必须允许匿名访问。
            .requestMatchers(new AntPathRequestMatcher("/api/auth/login"))
            .permitAll()

            // 其他库存、入库、出库和看板接口必须登录。
            .anyRequest().authenticated()
        )

        // 在 Spring 默认登录过滤器之前解析 JWT。
        .addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        );

    return http.build();
}
```

JWT 的实际解析位于：

```text
warehouse-backend/src/main/java/com/warehouse/security/JwtAuthenticationFilter.java
warehouse-backend/src/main/java/com/warehouse/security/JwtTokenProvider.java
```

过滤器从 `Authorization: Bearer <token>` 中取得 Token，验证成功后将用户 ID、用户名和角色写入 Spring Security 上下文。

### 18.2 入库数量与箱数保存

文件位置：

```text
warehouse-backend/src/main/java/com/warehouse/service/InboundOrderService.java
```

方法：`saveDetails()`

```java
int capacity = part.getPackageCapacity() != null
        ? part.getPackageCapacity()
        : 1;

// 优先保存前端明确输入的入库数量。
BigDecimal plannedQty = dto.getPlannedQty() != null
        ? dto.getPlannedQty()
        : BigDecimal.ZERO;

BigDecimal boxCount = dto.getBoxCount();

// 只输入数量时，后端补算箱数。
if (boxCount == null && plannedQty.compareTo(BigDecimal.ZERO) > 0) {
    boxCount = plannedQty.divide(
        BigDecimal.valueOf(capacity),
        2,
        RoundingMode.HALF_UP
    );
}

if (boxCount == null) {
    boxCount = BigDecimal.ZERO;
}

// 只输入箱数时，后端补算计划入库数量。
if (plannedQty.compareTo(BigDecimal.ZERO) <= 0
        && boxCount.compareTo(BigDecimal.ZERO) > 0) {
    plannedQty = BigDecimal.valueOf(capacity).multiply(boxCount);
}

detail.setPlannedQty(plannedQty);
detail.setBoxCount(boxCount);
```

前端双向输入位于：

```text
warehouse-frontend/src/views/inventory/inbound/components/PartsTable.vue
```

虽然该文件属于前端，但它与后端保存规则配套：

```ts
// 修改箱数时自动补全数量。
function onBoxCountChange(row: PartRow) {
  row.lastEdited = 'box'
  row.plannedQty = round(
    (row.packageCapacity || 1) * (row.boxCount || 0)
  )
}

// 修改数量时自动补全箱数。
function onQuantityChange(row: PartRow) {
  row.lastEdited = 'quantity'
  row.boxCount = round(
    (row.plannedQty || 0) / (row.packageCapacity || 1)
  )
}
```

### 18.3 入库看板生成和尾箱容量

文件位置：

```text
warehouse-backend/src/main/java/com/warehouse/service/KanbanService.java
```

方法：`generateForOrder()`

```java
BigDecimal boxCount = dto.getBoxCount() != null
        ? dto.getBoxCount()
        : BigDecimal.ZERO;

BigDecimal capacity = BigDecimal.valueOf(
    part.getPackageCapacity() != null
        ? part.getPackageCapacity()
        : 1
);

BigDecimal totalQty = dto.getPlannedQty() != null
        ? dto.getPlannedQty()
        : capacity.multiply(boxCount);

// 1.5 箱需要打印和创建 2 个看板。
int labelCount = boxCount
        .setScale(0, RoundingMode.CEILING)
        .intValue();

BigDecimal remaining = totalQty;

for (int seq = 0;
     seq < labelCount && remaining.compareTo(BigDecimal.ZERO) > 0;
     seq++) {

    // 前面的箱取完整容量，尾箱只取剩余数量。
    BigDecimal boxQty = remaining.min(capacity);

    Kanban kanban = new Kanban();
    kanban.setQuantity(boxQty);       // 当前箱实际数量，例如 25。
    kanban.setOriginalQty(capacity);  // 最大箱容量仍为 50。
    kanban.setBoxSeq(seq);

    // 创建单据时只生成待入库看板，不直接计入库存。
    kanban.setStatus(Kanban.STATUS_PENDING_INBOUND);
    kanbanMapper.insert(kanban);

    remaining = remaining.subtract(boxQty);
}
```

包装容量为 50、入库 75 件时，结果为：

```text
看板 1：quantity=50，originalQty=50，显示 50/1箱
看板 2：quantity=25，originalQty=50，显示 25/0.5箱
```

### 18.4 扫码入库与库存同步

文件位置：

```text
warehouse-backend/src/main/java/com/warehouse/service/ScanService.java
```

方法：`scanKanban()`

核心处理逻辑可概括为：

```java
@Transactional
public Map<String, Object> scanKanban(KanbanScanDTO dto) {
    // 1. 根据看板号查询看板，不存在则拒绝扫码。
    Kanban kanban = kanbanMapper.selectOne(
        new QueryWrapper<Kanban>()
            .eq("kanban_no", dto.getKanbanNo())
    );
    if (kanban == null) {
        throw new RuntimeException("看板不存在");
    }

    // 2. 看板必须属于当前入库单和零件。
    if (!kanban.getInboundOrderId().equals(dto.getInboundOrderId())) {
        throw new RuntimeException("看板不属于当前入库单");
    }

    // 3. 防止同一看板重复扫码。
    long duplicateCount = scanRecordMapper.selectCount(
        new QueryWrapper<ScanRecord>()
            .eq("kanban_no", dto.getKanbanNo())
    );
    if (duplicateCount > 0) {
        throw new RuntimeException("该看板已扫码");
    }

    // 4. 插入扫描记录并累加明细实际入库数量。
    scanRecordMapper.insert(record);
    detail.setActualQty(
        detail.getActualQty().add(kanban.getQuantity())
    );
    detailMapper.updateById(detail);

    // 5. 扫描完成后，看板才真正进入可用库存。
    kanban.setStatus(Kanban.STATUS_AVAILABLE);
    kanbanMapper.updateById(kanban);

    // 6. 根据所有明细重新计算未入库、部分入库或已入库。
    inboundOrderService.recalculateStatus(dto.getInboundOrderId());

    return result;
}
```

实际文件中还包含二维码字段校验、数量上限校验和进度返回。`@Transactional` 确保扫描记录、明细数量、看板状态和入库单状态同步提交。

### 18.5 库存聚合

文件位置：

```text
warehouse-backend/src/main/java/com/warehouse/service/InventoryService.java
```

方法：`listStock()`

```java
QueryWrapper<Kanban> inventoryQuery = new QueryWrapper<Kanban>()
    // 待入库和已出库看板不计入当前库存。
    .in(
        "status",
        Kanban.STATUS_AVAILABLE,
        Kanban.STATUS_BLOCKED,
        Kanban.STATUS_PARTIAL_REPACK
    );

if (warehouseAreaId != null) {
    // 库存页面可按库区筛选。
    inventoryQuery.eq("warehouse_area_id", warehouseAreaId);
}

List<Kanban> inventoryKanbans =
        kanbanMapper.selectList(inventoryQuery);

for (Kanban kanban : inventoryKanbans) {
    Long partId = kanban.getPartId();

    // 同一零件所有有效看板的当前数量求和。
    partTotalQty.merge(
        partId,
        kanban.getQuantity(),
        BigDecimal::add
    );

    // 看板数量即在库箱数，包括封存箱。
    partBoxCount.merge(partId, 1, Integer::sum);

    // 同时按库区累计数量。
    areaMap.merge(
        kanban.getWarehouseAreaId(),
        kanban.getQuantity(),
        BigDecimal::add
    );
}
```

库存来源是看板状态，而不是入库单计划数量。因此创建入库单但尚未扫码时，看板处于“待入库”，不会出现在正式库存中。

### 18.6 看板生命周期

文件位置：

```text
warehouse-backend/src/main/java/com/warehouse/service/KanbanService.java
```

方法：`getLifecycle()`

```java
Kanban kanban = findRequired(kanbanNo);

// 查询首次入库扫描记录。
ScanRecord inboundScan = scanRecordMapper.selectOne(
    new QueryWrapper<ScanRecord>()
        .eq("kanban_no", kanbanNo)
        .last("LIMIT 1")
);

// 查询最后一次出库扫描记录。
OutboundScan outboundScan = outboundScanMapper.selectOne(
    new QueryWrapper<OutboundScan>()
        .eq("kanban_no", kanbanNo)
        .orderByDesc("scan_time")
        .last("LIMIT 1")
);

// 当前看板既可能是转包父看板，也可能是子看板。
List<RepackRelation> repacks = repackRelationMapper.selectList(
    new QueryWrapper<RepackRelation>()
        .and(w -> w
            .eq("parent_kanban_no", kanbanNo)
            .or()
            .eq("child_kanban_no", kanbanNo)
        )
        .orderByAsc("repack_time")
);

List<Map<String, Object>> events = new ArrayList<>();

// 创建、入库、转包和出库统一转换为时间线事件。
addEvent(events, kanban.getCreateTime(),
         "CREATED", "看板创建", kanban.getInboundOrderNo());

if (inboundScan != null) {
    addEvent(events, inboundScan.getScanTime(),
             "INBOUND", "扫码入库", inboundScan.getInboundOrderNo());
}

for (RepackRelation relation : repacks) {
    String action = kanbanNo.equals(relation.getParentKanbanNo())
            ? "转包转出"
            : "转包生成";
    addEvent(events, relation.getRepackTime(),
             "REPACK", action, relation.getRepackOrderNo());
}

if (outboundScan != null) {
    addEvent(events, outboundScan.getScanTime(),
             "OUTBOUND", "扫码出库", outboundScan.getOutboundOrderNo());
}

// 按时间排序后同时返回给 Web 和 Android。
events.sort(Comparator.comparing(
    event -> (LocalDateTime) event.get("time"),
    Comparator.nullsLast(Comparator.naturalOrder())
));
```

接口位置：

```text
warehouse-backend/src/main/java/com/warehouse/controller/KanbanController.java
POST /api/kanban/lifecycle
```

### 18.7 FIFO 自动匹配看板

文件位置：

```text
warehouse-backend/src/main/java/com/warehouse/service/OutboundService.java
```

方法：`autoMatchKanbans()`

```java
List<Kanban> available = kanbanMapper.selectList(
    new QueryWrapper<Kanban>()
        .eq("part_id", detail.getPartId())

        // 封存、待入库和已出库看板不会参与匹配。
        .in(
            "status",
            Kanban.STATUS_AVAILABLE,
            Kanban.STATUS_PARTIAL_REPACK
        )

        // FIFO 的核心：创建时间最早的看板排在前面。
        .orderByAsc("create_time")
);

BigDecimal remaining = detail.getPlannedQty();

for (Kanban kanban : available) {
    if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        break;
    }

    if (kanban.getQuantity().compareTo(remaining) <= 0) {
        // 整箱匹配：锁定整个看板等待扫码出库。
        kanban.setStatus(Kanban.STATUS_LOCKED);
        kanban.setOutboundOrderId(orderId);
        kanban.setOutboundOrderNo(order.getOrderNo());
        kanbanMapper.updateById(kanban);

        remaining = remaining.subtract(kanban.getQuantity());
    } else {
        /*
         * 部分箱匹配：
         * 当前出库只锁定所需数量，
         * 剩余数量生成新的在库可用看板。
         */
        createRemainderKanban(kanban, remaining);
        lockOutboundQuantity(kanban, order, remaining);
        remaining = BigDecimal.ZERO;
    }
}

if (remaining.compareTo(BigDecimal.ZERO) > 0) {
    throw new RuntimeException("可用库存不足");
}
```

该方法在新增或编辑出库单后自动执行。通过锁定看板，可以防止同一库存同时分配给多张出库单。

### 18.8 扫码出库

文件位置：

```text
warehouse-backend/src/main/java/com/warehouse/service/OutboundService.java
```

方法：`scanOutbound()`

```java
@Transactional
public Map<String, Object> scanOutbound(
        Long orderId,
        String kanbanNo,
        Integer operatorId) {

    Kanban kanban = findKanban(kanbanNo);

    // 只能扫描已经由当前出库单 FIFO 锁定的看板。
    if (kanban.getStatus() != Kanban.STATUS_LOCKED) {
        if (kanban.getStatus() == Kanban.STATUS_BLOCKED) {
            throw new RuntimeException("看板已封存，不能出库");
        }
        if (kanban.getStatus() == Kanban.STATUS_OUTBOUND) {
            throw new RuntimeException("看板已出库");
        }
        throw new RuntimeException("看板未被当前出库单锁定");
    }

    if (!orderId.equals(kanban.getOutboundOrderId())) {
        throw new RuntimeException("看板不属于当前出库单");
    }

    // 保存不可重复的出库扫描历史。
    OutboundScan scan = new OutboundScan();
    scan.setOutboundOrderId(orderId);
    scan.setKanbanNo(kanbanNo);
    scan.setScanQty(kanban.getQuantity());
    scan.setOperatorId(operatorId);
    scanMapper.insert(scan);

    // 看板完成生命周期中的出库状态转换。
    kanban.setStatus(Kanban.STATUS_OUTBOUND);
    kanbanMapper.updateById(kanban);

    // 累加出库明细实出数量并更新单据状态。
    updateOutboundDetailActualQty(orderId, kanban);
    recalculateOutboundStatus(orderId);

    // 同步扣减入库明细库存，扣减顺序同样采用 FIFO。
    deductInboundStockFIFO(
        kanban.getPartId(),
        kanban.getQuantity()
    );

    return result;
}
```

### 18.9 转包确认和父子关系

文件位置：

```text
warehouse-backend/src/main/java/com/warehouse/service/RepackService.java
```

方法：`confirmRepack()`

```java
@Transactional
public RepackOrderVO confirmRepack(Long orderId) {
    RepackOrder order = getPendingOrder(orderId);
    List<RepackOrderDetail> details = getDetails(orderId);

    for (RepackOrderDetail detail : details) {
        Kanban source = getAvailableSource(detail.getSourceKanbanNo());
        int transferQty = detail.getTransferQty();

        if (source.getQuantity()
                .compareTo(BigDecimal.valueOf(transferQty)) < 0) {
            throw new RuntimeException("转包数量超过源看板可用数量");
        }

        // 从源看板扣减转出数量。
        BigDecimal remaining = source.getQuantity()
                .subtract(BigDecimal.valueOf(transferQty));
        source.setQuantity(remaining);

        // 全部转出后清空，否则标记为部分转出。
        source.setStatus(
            remaining.compareTo(BigDecimal.ZERO) <= 0
                ? Kanban.STATUS_CLEARED
                : Kanban.STATUS_PARTIAL_REPACK
        );
        kanbanMapper.updateById(source);

        // 创建新的目标看板，目标看板直接进入可用库存。
        Kanban target = createTargetKanban(
            order, source, detail, transferQty
        );
        target.setStatus(Kanban.STATUS_AVAILABLE);
        kanbanMapper.insert(target);

        // 保存父子关系，为生命周期追溯提供依据。
        RepackRelation relation = new RepackRelation();
        relation.setRepackOrderNo(order.getOrderNo());
        relation.setParentKanbanNo(source.getKanbanNo());
        relation.setChildKanbanNo(target.getKanbanNo());
        relation.setTransferQty(transferQty);
        relation.setRepackTime(LocalDateTime.now());
        repackRelationMapper.insert(relation);
    }

    order.setStatus(RepackOrder.STATUS_COMPLETED);
    repackOrderMapper.updateById(order);
    return buildVO(order);
}
```

实际实现根据合并、拆包等转包类型调用不同的内部处理方法，但共同原则是：

```text
扣减源看板 → 创建目标看板 → 保存父子关系 → 更新转包单状态
```

### 18.10 状态流转总览

关键状态定义文件：

```text
warehouse-backend/src/main/java/com/warehouse/model/entity/Kanban.java
warehouse-backend/src/main/java/com/warehouse/model/enums/KanbanStatus.java
```

```text
创建入库单
  ↓
待入库
  ↓ 扫码入库
在库可用
  ├─ 封存 → 封存 → 解封 → 在库可用
  ├─ FIFO 分配 → 待出库 → 扫码 → 已出库
  └─ 转包
       ├─ 有剩余 → 部分转出
       └─ 无剩余 → 已清空
```

所有状态修改都在 Service 层完成，Controller 只负责接收参数和返回结果，避免不同客户端各自实现状态判断。

## 19. 核心业务关系总结

```text
供应商
  └─ 绑定零件
       └─ 设置包装容量和默认库区
            └─ 创建入库单
                 └─ 生成待入库看板
                      └─ 扫码入库
                           └─ 看板进入库存
                                ├─ 封存/解封
                                ├─ FIFO 匹配出库
                                │    └─ 扫码出库
                                └─ 转包
                                     └─ 创建父子看板关系
                                          └─ 生命周期追溯
```

系统以“看板”为库存最小追踪单元，以入库和出库扫描记录作为时间节点，以转包关系作为父子链路，从而实现零件从创建、入库、在库、封存、转包到出库的完整生命周期管理。
