# 仓储管理系统 6.15 版本实现说明

本文档根据 2026 年 6 月 15 日项目当前代码整理，重点介绍修改后的零件管理、入库、库存、出库和转包模块，以及 Web 前端、Spring Boot 后端、MySQL 数据库和 Android 扫码端之间的实现关系。

> 本文描述的是当前代码实际实现。项目主体流程已经具备，但源码构建、历史台账和部分状态逻辑仍有需要继续完善的地方，详见文末“当前限制”。

## 1. 系统架构

```text
Vue 3 Web 管理端 ─┐
                  ├── HTTP/JSON + JWT ── Spring Boot ── MyBatis-Plus ── MySQL
Android 扫码端 ───┘
```

| 层级 | 技术 | 主要职责 |
|---|---|---|
| Web 管理端 | Vue 3、TypeScript、Vite、Element Plus、Pinia | 基础资料、单据、库存、打印和管理查询 |
| Android 端 | Kotlin、Jetpack Compose、CameraX、ML Kit、Retrofit | 现场扫码入库、出库、封存、转包和追溯 |
| 后端 | Spring Boot 3、Spring Security、JWT、MyBatis-Plus | 权限、业务校验、事务和状态流转 |
| 数据库 | MySQL 8 | 保存主数据、业务单据、看板和扫码记录 |

Web 和 Android 使用同一套 `/api` 接口。登录成功后，客户端在请求头中携带：

```http
Authorization: Bearer <JWT>
```

### 1.1 项目目录

```text
warehouse-management-system-qing/
├── warehouse-frontend/    # Vue 3 Web 管理端
├── warehouse-backend/     # Spring Boot 后端
├── warehouse-scanner/     # Android 扫码端
└── README6.15.md          # 本文档
```

### 1.2 公共入口

| 文件 | 功能 |
|---|---|
| `warehouse-frontend/src/main.ts` | 创建 Vue 应用并注册 Router、Pinia 和 Element Plus |
| `warehouse-frontend/src/router/index.ts` | Web 路由和登录守卫 |
| `warehouse-frontend/src/api/request.ts` | Axios、JWT 请求头、401 跳转和错误提示 |
| `warehouse-backend/src/main/java/com/warehouse/WarehouseApplication.java` | Spring Boot 启动入口 |
| `warehouse-backend/src/main/java/com/warehouse/config/SecurityConfig.java` | JWT 无状态认证配置 |
| `warehouse-scanner/app/src/main/java/com/warehouse/scanner/MainActivity.kt` | Android Compose 界面入口 |
| `warehouse-scanner/app/src/main/java/com/warehouse/scanner/ui/AppNavigation.kt` | Android 页面导航 |
| `warehouse-scanner/app/src/main/java/com/warehouse/scanner/network/RetrofitClient.kt` | Retrofit、JWT 拦截器和后端地址 |
| `warehouse-scanner/app/src/main/java/com/warehouse/scanner/network/ApiService.kt` | Android 调用的后端接口 |

## 2. 零件管理

零件是入库、库存、出库和转包的基础数据。修改后的零件资料包含：

- 零件编码、名称、规格和单位
- 包装容量 `packageCapacity`
- 默认库区 `warehouseAreaId`
- 供应商关联 `supplierId`

### 2.1 Web 前端

主要文件：

| 文件 | 功能 |
|---|---|
| `warehouse-frontend/src/views/system/PartList.vue` | 零件列表、新增、修改和删除 |
| `warehouse-frontend/src/api/part.ts` | 零件查询、保存、删除接口 |
| `warehouse-frontend/src/api/supplier.ts` | 供应商下拉数据 |
| `warehouse-frontend/src/api/warehouseArea.ts` | 默认库区下拉数据 |
| `warehouse-frontend/src/types/inbound.ts` | 零件、供应商和库区 TypeScript 类型 |

`PartList.vue` 在编辑表单中绑定供应商、包装容量和默认库区。保存后，这些字段会被后续入库页面直接引用。

### 2.2 后端

主要文件：

| 文件 | 功能 |
|---|---|
| `controller/PartController.java` | 暴露零件查询、保存和删除接口 |
| `service/PartService.java` | 保存零件，维护供应商关联，填充供应商和库区名称 |
| `service/SupplierPartService.java` | 校验零件是否属于指定供应商 |
| `entity/Part.java` | 零件实体 |
| `entity/SupplierPart.java` | 供应商与零件关联实体 |
| `mapper/PartMapper.java` | `part` 表访问 |
| `mapper/SupplierPartMapper.java` | `supplier_part` 表访问 |

主要接口：

| 接口 | 功能 |
|---|---|
| `GET /api/part/list?supplierId=` | 查询全部零件或指定供应商的零件 |
| `POST /api/part/save` | 新增或修改零件 |
| `POST /api/part/delete` | 删除零件及其供应商关联 |

### 2.3 数据库

相关表：

- `part`：零件主数据，包含包装容量和默认库区。
- `supplier`：供应商主数据。
- `supplier_part`：供应商与零件的关联关系。
- `warehouse_area`：库区主数据。

### 2.4 与其他模块的数据绑定

```text
零件包装容量
  └─ 入库计划数量 = 包装容量 × 箱数

零件默认库区
  └─ 入库明细未手工选择库区时自动使用

供应商零件关系
  └─ 创建入库单时只加载所选供应商对应的零件
```

Android 端目前不维护零件主数据，只使用 Web 端维护后的数据进行扫码业务。

## 3. 入库管理

入库模块负责创建入库单、生成每箱看板、扫码收货、更新实收数量和入库状态。

### 3.1 Web 前端

| 文件 | 功能 |
|---|---|
| `views/inventory/Inbound.vue` | 入库单列表、状态筛选、详情、编辑、作废和打印 |
| `views/inventory/inbound/InboundForm.vue` | 创建和编辑入库单 |
| `views/inventory/inbound/InboundDetail.vue` | 入库单详情、扫码记录和看板信息 |
| `views/inventory/inbound/components/PartsTable.vue` | 按供应商加载零件，设置箱数和库区 |
| `views/inventory/inbound/components/PrintDialog.vue` | 打印入库单和每箱二维码看板 |
| `api/inbound.ts` | 入库单和扫码记录接口 |
| `api/kanban.ts` | 查询入库单生成的看板 |

创建入库单时，`PartsTable.vue` 使用：

```text
plannedQty = packageCapacity × boxCount
```

页面把零件 ID、箱数、计划数量、库区等数据提交给后端。

### 3.2 后端

| 文件 | 功能 |
|---|---|
| `controller/InboundOrderController.java` | 入库单列表、保存、详情、删除和作废 |
| `controller/ScanController.java` | 入库扫码、扫码记录和看板扫码 |
| `service/InboundOrderService.java` | 入库单主流程、供应商零件校验和状态重算 |
| `service/KanbanService.java` | 根据箱数生成每箱看板 |
| `service/ScanService.java` | 扫码入库、更新实收数量和看板状态 |
| `service/OrderNoGenerator.java` | 生成入库单号 |
| `entity/InboundOrder.java` | 入库单主表实体 |
| `entity/InboundOrderDetail.java` | 入库单明细实体 |
| `entity/ScanRecord.java` | 入库扫码记录实体 |
| `entity/Kanban.java` | 每箱看板实体 |

主要接口：

| 接口 | 功能 |
|---|---|
| `POST /api/inbound-order/list` | 分页查询入库单 |
| `POST /api/inbound-order/save` | 新建或修改入库单 |
| `POST /api/inbound-order/detail` | 按 ID 查询详情 |
| `POST /api/inbound-order/detail-by-no` | 按单号查询详情，供 Android 使用 |
| `POST /api/inbound-order/delete` | 删除允许删除的入库单 |
| `POST /api/inbound-order/cancel` | 作废入库单 |
| `POST /api/scan/kanban` | 扫描看板完成一箱入库 |
| `POST /api/scan/list` | 查询入库扫码记录 |
| `POST /api/scan/delete` | 删除扫码记录并回退实收数量 |
| `POST /api/kanban/list-by-order` | 查询入库单对应的全部看板 |

### 3.3 状态

入库单状态定义在 `model/enums/InboundStatus.java`：

| 状态值 | 含义 |
|---|---|
| `0` | 未入库 |
| `1` | 部分入库 |
| `2` | 已入库 |
| `3` | 作废 |

`ScanService` 每次扫码后更新明细 `actual_qty`，再调用 `InboundOrderService.recalculateStatus()`：

```text
全部 actualQty = 0             → 未入库
部分 actualQty > 0             → 部分入库
全部 actualQty >= plannedQty   → 已入库
```

### 3.4 Android 扫码端

| 文件 | 功能 |
|---|---|
| `ui/scanner/ScannerScreen.kt` | 入库扫码界面 |
| `ui/scanner/ScannerViewModel.kt` | 解析二维码并调用入库接口 |
| `ui/scanner/CameraScanDialog.kt` | CameraX 相机预览和 ML Kit 识别 |
| `ui/detail/OrderDetailScreen.kt` | Android 入库单详情 |
| `ui/detail/OrderDetailViewModel.kt` | 加载入库单详情 |
| `ui/submit/SubmitScreen.kt` | 扫码记录页面 |
| `ui/submit/SubmitViewModel.kt` | 查询和删除扫码记录 |
| `model/Models.kt` | `KanbanScanRequest`、`KanbanScanResult` 等模型 |

入库链路：

```text
Web 创建入库单
→ 后端生成看板
→ Web 打印二维码
→ Android 扫描二维码
→ POST /api/scan/kanban
→ 写入 scan_record
→ 更新 inbound_order_detail.actual_qty
→ 更新 kanban.status
→ 重算 inbound_order.status
```

## 4. 库存管理

库存模块以“有效看板”为主要库存来源，不是简单汇总所有入库单。

### 4.1 Web 前端

| 文件 | 功能 |
|---|---|
| `views/inventory/Stock.vue` | 按零件显示库存、箱数、箱均数量和库区分布 |
| `api/inventory.ts` | 库存总览接口 |
| `api/kanban.ts` | 展开查看每个零件的看板明细 |

库存表展开后可查看：

- 看板号、箱序号
- 原始箱容量和当前剩余数量
- 当前库区
- 看板状态
- 入库单号和入库时间
- 出库单号和出库扫码时间
- 封存和解封操作

### 4.2 后端

| 文件 | 功能 |
|---|---|
| `controller/InventoryController.java` | 库存总览接口 |
| `service/InventoryService.java` | 按零件和库区汇总有效看板 |
| `controller/KanbanController.java` | 看板明细、封存和解封接口 |
| `service/KanbanService.java` | 查询看板、填充出库时间、执行状态切换 |
| `dto/InventoryVO.java` | 库存汇总返回对象 |

主要接口：

| 接口 | 功能 |
|---|---|
| `POST /api/inventory/stock-list` | 查询库存总览 |
| `POST /api/kanban/list-by-part` | 查询零件的全部看板 |
| `POST /api/kanban/block` | 封存看板 |
| `POST /api/kanban/unblock` | 解封看板 |
| `POST /api/kanban/toggle-block` | 根据当前状态自动封存或解封 |
| `POST /api/kanban/block-by-part` | 按零件批量封存 |
| `POST /api/kanban/batch-unblock` | 批量解封 |

库存汇总主要统计以下状态：

- `1`：在库可用
- `5`：部分转出但仍有剩余数量

### 4.3 看板状态

状态常量位于 `model/entity/Kanban.java`：

| 状态值 | 含义 |
|---|---|
| `0` | 待入库 |
| `1` | 在库可用 |
| `2` | 待出库，已被出库单锁定 |
| `3` | 已出库 |
| `4` | 封存 |
| `5` | 部分转出 |
| `6` | 已清空 |

### 4.4 Android 封存和查询

| 文件 | 功能 |
|---|---|
| `ui/scanner/BlockUnblockScreen.kt` | 扫码封存和解封页面 |
| `ui/scanner/BlockUnblockViewModel.kt` | 调用 `/kanban/toggle-block` |
| `ui/scanner/TraceScreen.kt` | 扫码查询看板来源和去向 |
| `ui/scanner/TraceViewModel.kt` | 调用 `/repack/trace` |

## 5. 出库管理

出库模块负责创建出库计划、自动匹配库存看板、锁定待出库看板和扫码出库。

### 5.1 Web 前端

| 文件 | 功能 |
|---|---|
| `views/inventory/Outbound.vue` | 出库单列表、筛选、打印、编辑、删除和作废 |
| `views/inventory/outbound/OutboundForm.vue` | 创建出库单并查询零件可用库存 |
| `views/inventory/outbound/OutboundDetail.vue` | 出库明细、待出库看板和扫码历史 |
| `views/inventory/outbound/components/OutboundPrintDialog.vue` | 打印待出库看板 |
| `api/outbound.ts` | 出库单和扫码出库相关接口 |

### 5.2 后端

| 文件 | 功能 |
|---|---|
| `controller/OutboundController.java` | 出库单和扫码出库接口 |
| `service/OutboundService.java` | 出库业务、库存匹配、看板锁定和状态更新 |
| `entity/OutboundOrder.java` | 出库单主表实体 |
| `entity/OutboundOrderDetail.java` | 出库单明细实体 |
| `entity/OutboundScan.java` | 出库扫码记录实体 |

主要接口：

| 接口 | 功能 |
|---|---|
| `POST /api/outbound-order/list` | 分页查询出库单 |
| `POST /api/outbound-order/save` | 新建或修改出库单 |
| `POST /api/outbound-order/detail` | 查询出库详情和扫码记录 |
| `POST /api/outbound-order/delete` | 删除出库单并释放未出库看板 |
| `POST /api/outbound-order/cancel` | 作废出库单并释放未出库看板 |
| `POST /api/outbound/available-stock` | 查询零件可用库存 |
| `POST /api/outbound-order/pending-kanbans` | 查询已锁定的待出库看板 |
| `POST /api/outbound/scan` | Android 扫码出库 |

### 5.3 FIFO 和看板锁定

保存出库单后，`OutboundService.autoMatchKanbans()`：

1. 查询出库明细需要的零件和数量。
2. 查询状态为“在库可用”或“部分转出”的看板。
3. 按 `create_time`、`box_seq` 升序匹配。
4. 把选中的看板改为状态 `2`。
5. 写入 `outbound_order_id` 和 `outbound_order_no`。
6. 库存不足时通过事务回滚整个保存操作。

当一个看板数量大于剩余需求时，当前代码会拆分看板：

- 原看板保留本次需要出库的数量并被锁定。
- 生成一个新的余量看板，继续保持在库可用。
- 创建一张自动入库单记录余量。

### 5.4 扫码出库

`OutboundService.scanOutbound()` 执行：

```text
校验看板存在
→ 校验看板已被当前出库单锁定
→ 写入 outbound_scan
→ 看板状态改为已出库
→ 清空看板库区
→ 更新出库明细 actual_qty
→ 重算出库单状态
```

出库单状态位于 `model/enums/OutboundStatus.java`：

| 状态值 | 含义 |
|---|---|
| `0` | 未出库 |
| `1` | 部分出库 |
| `2` | 已出库 |
| `3` | 作废 |

### 5.5 Android 出库端

| 文件 | 功能 |
|---|---|
| `ui/scanner/OutboundScannerScreen.kt` | 独立扫码出库页面 |
| `ui/scanner/OutboundScannerViewModel.kt` | 解析看板并调用 `/outbound/scan` |
| `ui/scanner/ScannerViewModel.kt` | 通用扫码页面中的出库模式 |
| `model/Models.kt` | `OutboundScanResult` 数据模型 |

## 6. 转包管理

转包用于处理包装拆分、多个包装合并和带余量转移。

### 6.1 转包类型

定义在 `model/enums/RepackType.java`：

| 类型 | 含义 |
|---|---|
| `BREAKDOWN` | 向下转包，`1 → N` 拆包 |
| `CONSOLIDATE` | 向上转包，`N → 1` 合并 |
| `REMAINDER` | 带余量转包，`1 → 1` 转出部分数量 |

转包状态定义在 `RepackStatus.java`：

| 状态值 | 含义 |
|---|---|
| `0` | 待转包 |
| `1` | 已完成 |
| `2` | 已取消 |

### 6.2 Web 前端

| 文件 | 功能 |
|---|---|
| `views/inventory/repack/RepackList.vue` | 转包列表、新建、确认、取消和扫码溯源 |
| `views/inventory/repack/RepackForm.vue` | 添加源看板、数量和目标箱容量 |
| `views/inventory/repack/RepackDetail.vue` | 转包明细和父子看板关系 |
| `api/repack.ts` | 转包及追溯接口 |
| `types/repack.ts` | 转包相关 TypeScript 类型 |

### 6.3 后端

| 文件 | 功能 |
|---|---|
| `controller/RepackController.java` | 转包单、预览、确认和追溯接口 |
| `service/RepackService.java` | 拆包、合并、余量转包和父子关系维护 |
| `entity/RepackOrder.java` | 转包单主表 |
| `entity/RepackOrderDetail.java` | 转包源看板明细 |
| `entity/RepackRelation.java` | 新旧看板父子关系 |

主要接口：

| 接口 | 功能 |
|---|---|
| `POST /api/repack/list` | 查询转包单 |
| `POST /api/repack/save` | 创建转包单 |
| `POST /api/repack/add-detail` | 添加源看板 |
| `POST /api/repack/remove-detail` | 删除源看板明细 |
| `POST /api/repack/breakdown-generate` | 按目标箱容量生成拆包方案 |
| `POST /api/repack/preview` | 扫码后预览看板是否可转包 |
| `POST /api/repack/confirm` | 确认并执行转包 |
| `POST /api/repack/cancel` | 取消转包单 |
| `POST /api/repack/trace` | 查询看板父子关系 |

确认转包时，后端会：

1. 校验源看板状态和可转数量。
2. 更新源看板剩余数量。
3. 源看板有余量时改为“部分转出”，无余量时改为“已清空”。
4. 生成一个或多个新看板。
5. 写入 `repack_relation`，记录父看板、子看板、转包单号和转移数量。
6. 将转包单状态改为已完成。

### 6.4 Android 转包与追溯

| 文件 | 功能 |
|---|---|
| `ui/scanner/RepackScannerScreen.kt` | 选择转包类型、扫描源包装并确认 |
| `ui/scanner/RepackScannerViewModel.kt` | 创建转包单、预览、添加明细和确认转包 |
| `ui/scanner/TraceScreen.kt` | 扫码展示当前看板和上下游关系 |
| `ui/scanner/TraceViewModel.kt` | 调用 `/repack/trace` |
| `model/Models.kt` | `RepackPreviewData`、`RepackOrderData`、`TraceData` |

追溯结果包含：

- 当前看板信息
- 向上追溯的来源看板
- 向下追溯的目标看板
- 转包单号、类型、数量和时间

## 7. Android 与电脑后端连接

Android 开发版后端地址为：

```kotlin
http://localhost:8081/api/
```

手机中的 `localhost` 默认指手机本身，因此 USB 连接后需要执行：

```powershell
adb reverse tcp:8081 tcp:8081
```

这样手机访问 `localhost:8081` 时，请求会转发到电脑的 Spring Boot 服务。

Android 首页功能由 `HomeScreen.kt` 提供：

- 扫码入库
- 扫码出库
- 扫码封存/解封
- 扫码转包
- 扫码溯源

## 8. 数据库脚本

SQL 位于 `warehouse-backend/src/main/resources/sql/`：

| 脚本 | 功能 |
|---|---|
| `init.sql` | 数据库、用户表和管理员 |
| `V2__phase2_schema.sql` | 供应商、零件、库区和入库表 |
| `V3__test_data.sql` | 测试基础数据和入库数据 |
| `V4__packaging_and_area.sql` | 包装容量、默认库区和箱数 |
| `V5__kanban.sql` | 看板表 |
| `V6__scan_kanban.sql` | 扫码记录增加看板号 |
| `V7__outbound.sql` | 出库单、明细和扫码表 |
| `V8__outbound_refactor.sql` | 出库和看板状态调整 |
| `V9__outbound_status_supplier.sql` | 客户和出库状态补充 |
| `V10__repack.sql` | 转包单、转包明细和父子关系 |

## 9. 端到端业务关系

```text
零件管理
  ├─ 包装容量 ──────────────→ 入库计划数量、看板箱容量
  ├─ 默认库区 ──────────────→ 入库明细和看板库区
  └─ 供应商关联 ────────────→ 入库零件筛选与后端校验

入库单
  └─ 生成看板 ─→ Android 扫码入库 ─→ 在库可用

库存
  └─ 汇总有效看板 ─→ 可被出库单 FIFO 匹配

出库单
  └─ 锁定看板 ─→ Android 扫码出库 ─→ 已出库

转包单
  └─ 源看板 ─→ 生成子看板 ─→ 保存父子关系 ─→ 扫码追溯
```

## 10. 当前限制

当前代码可以展示主要业务流程，但继续开发时应注意：

1. 创建入库单时，看板当前直接生成成“在库可用”，严格流程应先为“待入库”，扫码后再变为可用。
2. 编辑入库单时会删除并重新生成该单看板，可能影响已扫码看板的身份和历史。
3. 出库 FIFO 当前依据看板 `create_time`，不是独立的实际入库时间或批次日期。
4. 出库时还会扣减入库明细 `actual_qty`，会改变原入库历史，应改为独立库存流水或余额字段。
5. 当前没有统一的库存流水表和看板状态事件表，生命周期主要通过当前状态、扫码记录和转包关系拼接。
6. 零件与供应商数据库支持关联表，但页面和 `PartService` 当前主要按单个供应商展示和保存。
7. 前端 TypeScript 构建仍存在 QRCode 类型和部分看板类型错误。
8. 后端 Maven 构建环境当前存在 Lombok 注解处理问题。

以上限制不影响理解当前模块结构，但在生产验收前需要继续修正并增加自动化测试。
