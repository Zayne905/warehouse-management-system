# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Chinese-language warehouse management system (仓储管理系统) — a full-stack, separated frontend/backend web application. Phase 1 (login + layout skeleton) is complete. Phase 2 (inbound order management with scan-based receiving + master data CRUD) is implemented. Outbound, inventory views, and user management are still placeholder pages.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Vue 3 + TypeScript + Vite |
| UI Library | Element Plus (Chinese locale) |
| State | Pinia |
| HTTP | Axios |
| Backend | Spring Boot 3.2.5 + JDK 17 |
| Security | Spring Security + JWT (jjwt 0.12.5), stateless |
| ORM | MyBatis-Plus 3.5.7 |
| Database | MySQL 8.0 (`warehouse_db`, root / 1234 @ localhost:3306) |

## Project Structure

```
warehouse_manage/
├── warehouse-backend/          # Spring Boot Maven project (port 8081)
│   └── src/main/java/com/warehouse/
│       ├── WarehouseApplication.java
│       ├── config/             # SecurityConfig, CorsConfig, MybatisPlusConfig, GlobalExceptionHandler
│       ├── controller/         # AuthController, InboundOrderController, ScanController, SupplierController, ...
│       ├── model/
│       │   ├── entity/         # User, Supplier, Part, WarehouseArea, InboundOrder, InboundOrderDetail, ScanRecord, SupplierPart
│       │   ├── dto/            # LoginRequest, Result<T>, PageResult<T>, InboundOrderSaveDTO, ScanSubmitDTO, etc.
│       │   └── enums/          # InboundStatus (PENDING/PARTIAL/COMPLETED/CANCELLED)
│       ├── mapper/             # MyBatis-Plus BaseMapper interfaces (UserMapper, InboundOrderMapper, ...)
│       ├── security/           # JwtTokenProvider, JwtAuthenticationFilter, SecurityUtils
│       └── service/            # UserService, InboundOrderService, ScanService, OrderNoGenerator, ...
│
└── warehouse-frontend/          # Vue 3+Vite project (dev port 5173)
    └── src/
        ├── api/                # request.ts (Axios instance), auth.ts, inbound.ts, supplier.ts, part.ts, warehouseArea.ts
        ├── layout/             # MainLayout, SideMenu, TabBar
        ├── router/             # Route config + beforeEach guard
        ├── stores/             # auth.ts (token/nickname), tabs.ts (tab state)
        ├── types/              # inbound.ts (all DTO/VO/enum types)
        └── views/              # Login, Dashboard, warehouse/*, inventory/* (inbound fully built), system/*
```

## Commands

### Backend (warehouse-backend/)

```bash
# Compile
mvn compile

# Run (starts on port 8081)
mvn spring-boot:run

# Generate a BCrypt password hash (for adding users manually)
mvn exec:java -Dexec.mainClass="com.warehouse.GenPassword"
```

### Frontend (warehouse-frontend/)

```bash
# Install dependencies
npm install

# Dev server (port 5173, proxies /api → localhost:8081)
npm run dev

# Type-check + build
npm run build
```

### Database

```bash
# Initialize Phase 1 schema + admin user (admin / admin123)
mysql -u root -p1234 < warehouse-backend/src/main/resources/sql/init.sql

# Phase 2 schema (inbound orders, master data tables, role column)
mysql -u root -p1234 < warehouse-backend/src/main/resources/sql/V2__phase2_schema.sql

# Phase 2 test data (test inbound orders in various states, scan records)
mysql -u root -p1234 < warehouse-backend/src/main/resources/sql/V3__test_data.sql
```

## Architecture Notes

### API Convention

All API endpoints use POST (except `/api/auth/login` GET and `/api/supplier/list` GET). Even read operations like detail queries and list queries use POST with a JSON body. This is because the Axios request interceptor only attaches the token to same-origin requests via the Vite proxy; using POST avoids browser GET-caching issues.

Responses use `Result<T>`: `{ code: 200, message: "success", data: T }`. Error responses use `Result.error(code, message)`. The Axios response interceptor unwraps `response.data` (the full JSON body), so API functions receive the Result object directly.

Paginated list responses embed `PageResult<T>` inside `Result.data`: `{ code: 200, message: "success", data: { records: T[], total, current, size } }`.

### Authentication Flow

1. `POST /api/auth/login` — the **only unauthenticated endpoint**. Returns JWT (24h expiry, HMAC-SHA256).
2. Axios request interceptor attaches `Authorization: Bearer <token>` to every request.
3. `JwtAuthenticationFilter` (a `OncePerRequestFilter`) extracts the token, loads the `User` from DB, and sets `SecurityContextHolder`'s authentication to the `User` entity itself. Controllers can get the current user via `SecurityUtils.getCurrentUser()`.
4. Router `beforeEach` guard: no token → redirect `/login`; token + on `/login` → redirect `/`.

### Authorization (Basic RBAC)

The `sys_user` table has a `role` column (`admin` / `user`). `SecurityUtils.isAdmin()` checks this. The `InboundStatus.canEdit(status, isAdmin)` method encodes business rules:
- `CANCELLED` orders: no one can edit
- `COMPLETED` orders: only admins can edit
- `PARTIAL` / `PENDING`: anyone can edit

This is used in `InboundOrderService` to gate update/delete operations. No Spring Security role-based access control yet — just application-level checks.

### Inbound Order State Machine

```
PENDING(0) ──scan──▶ PARTIAL(1) ──scan──▶ COMPLETED(2)
    │                    │                     │
    └────cancel──▶ CANCELLED(3) ◀──cancel──────┘
```

Status is recalculated automatically after every scan: if all details have `actualQty >= plannedQty` → `COMPLETED`; if any detail has `actualQty > 0` → `PARTIAL`; otherwise → `PENDING`. See `InboundOrderService.recalculateStatus()`.

### Order Number Generation

`OrderNoGenerator.generate()` produces IDs in format `RyyyyMMddNNN` (e.g., `R20260606001`). It queries today's orders via `LIKE` prefix match, takes the max sequence, and increments. The method is `synchronized` to prevent duplicates under concurrent requests.

### Scan-Based Receiving

The scan flow models a handheld scanner workflow:

1. **Check duplicate**: `POST /api/scan/check-duplicate` — verifies the part belongs to the inbound order and has remaining quantity
2. **Submit scan**: `POST /api/scan/submit` — inserts a `ScanRecord`, increments `inbound_order_detail.actual_qty`, recalculates order status
3. **Delete scan**: `POST /api/scan/delete` — removes the scan record, decrements `actual_qty`, recalculates status
4. **Feedback**: `POST /api/scan/feedback` — returns order summary + per-part scanned totals for display on the scanner UI

### Supplier-Part Relationship

The `supplier_part` junction table links suppliers to the parts they supply. `InboundOrderService.saveDetails()` validates that each part in an inbound order belongs to the order's supplier. `SupplierPartService.isPartBelongsToSupplier()` is used for this check and during batch copy operations to filter out parts not belonging to the target supplier.

### Tab State Model

The `tabs` Pinia store drives the tag-bar tabs. Dashboard (`/dashboard`) is always present and non-closable. When navigating to a child route, `router.beforeEach` calls `tabsStore.addTab(to)` which appends a `Tab` object `{ path, title, icon, closable }`. Hidden routes (e.g., InboundForm, InboundDetail) still appear as tabs. Tab removal activates the nearest sibling or falls back to dashboard.

### CORS Strategy

Two-layer: `CorsConfig` (Spring bean) permits all origin patterns for development; Vite dev server proxies `/api` → `http://localhost:8081` to avoid cross-origin issues during development. The proxy target in `vite.config.ts` is now aligned with the backend port (8081).

### Global Error Handling

`GlobalExceptionHandler` (a `@RestControllerAdvice`) catches `RuntimeException` → 400 and generic `Exception` → 500, returning `Result.error(code, message)`. Services throw `RuntimeException` with Chinese error messages for business errors (e.g., "入库单不存在", "当前状态不允许修改").

### Password Storage

BCrypt via `BCryptPasswordEncoder`. The `GenPassword` utility class prints BCrypt hashes for seeding users. Default admin password hash is stored in init.sql.

## Key Constraints

- **Phase 2 implemented**: Inbound order management (CRUD, scan receiving, status flow), supplier/part/warehouse area master data. Outbound, warehouse list, area management, and user management views are still placeholders.
- **No Spring Security RBAC**: Authorization uses application-level `role` field checks, not Spring Security granted authorities.
- **No tests**: No test files exist in either the backend or frontend project.
- **MySQL required**: The app expects MySQL running locally with credentials root/1234. Database `warehouse_db` is auto-created via `createDatabaseIfNotExist=true`.
- **All APIs use POST** (mostly): Pattern is `POST /api/<resource>/<action>` with JSON body, even for reads. Only auth login is GET.
- **Frontend types**: All TypeScript interfaces for the inbound module live in `src/types/inbound.ts` — this file also exports `Supplier`, `Part`, `WarehouseArea`, `BatchOperationDTO`, and `ScanRecord` types used across the app (not just inbound).
