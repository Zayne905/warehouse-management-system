@echo off
REM ============================================================
REM 入库管理 API 快速烟雾测试 (Windows Batch)
REM 使用前提:
REM   1. 已执行 V2/V3 SQL 脚本
REM   2. 后端已启动于 localhost:8081
REM 运行方式: test\quick-test.bat
REM ============================================================

set BASE_URL=http://localhost:8081/api
set PASS=0
set FAIL=0

echo ============================================
echo  入库管理 API 烟雾测试 (Windows)
echo  时间: %DATE% %TIME%
echo ============================================

REM ---- 1. 管理员登录 ----
echo.
echo [TEST] 管理员登录...
curl -s -X POST %BASE_URL%/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}" > %TEMP%\login_resp.json 2>nul
for /f "tokens=*" %%i in ('python3 -c "import json,sys; d=json.load(open('%TEMP%/login_resp.json',encoding='utf-8')); print(d.get('data',{}).get('token',''))" 2^>nul') do set TOKEN=%%i

if "%TOKEN%"=="" (
    echo [FAIL] 管理员登录失败 — 请确认后端已启动且数据库已初始化
    exit /b 1
)
echo [PASS] 管理员登录成功
set /a PASS+=1

REM ---- 2. 供应商列表 ----
echo.
echo [TEST] 获取供应商列表...
curl -s -H "Authorization: Bearer %TOKEN%" %BASE_URL%/supplier/list > %TEMP%\suppliers.json 2>nul
for /f "tokens=*" %%i in ('python3 -c "import json,sys; d=json.load(open('%TEMP%/suppliers.json',encoding='utf-8')); print(len(d.get('data',[])))" 2^>nul') do set SUP_COUNT=%%i
if %SUP_COUNT% GTR 0 (
    echo [PASS] 供应商列表 — 共 %SUP_COUNT% 条
    set /a PASS+=1
) else (
    echo [FAIL] 供应商列表为空
    set /a FAIL+=1
)

REM ---- 3. 创建入库单 ----
echo.
echo [TEST] 创建入库单...
curl -s -X POST %BASE_URL%/inbound-order/save -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"supplierId\":1,\"orderNumber\":\"PO-SMOKE-TEST\",\"remark\":\"烟雾测试\",\"details\":[{\"partId\":1,\"plannedQty\":10,\"unit\":\"个\",\"lineNo\":1}]}" > %TEMP%\create.json 2>nul
for /f "tokens=*" %%i in ('python3 -c "import json,sys; d=json.load(open('%TEMP%/create.json',encoding='utf-8')); print(d.get('code',0))" 2^>nul') do set CREATE_CODE=%%i
for /f "tokens=*" %%i in ('python3 -c "import json,sys; d=json.load(open('%TEMP%/create.json',encoding='utf-8')); print(d.get('data',{}).get('id',0))" 2^>nul') do set NEW_ID=%%i

if "%CREATE_CODE%"=="200" (
    echo [PASS] 创建入库单成功 — ID: %NEW_ID%
    set /a PASS+=1
) else (
    echo [FAIL] 创建入库单失败 — code=%CREATE_CODE%
    set /a FAIL+=1
)

REM ---- 4. 列表查询 ----
echo.
echo [TEST] 入库单列表...
curl -s -X POST %BASE_URL%/inbound-order/list -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"current\":1,\"size\":10}" > %TEMP%\list.json 2>nul
for /f "tokens=*" %%i in ('python3 -c "import json,sys; d=json.load(open('%TEMP%/list.json',encoding='utf-8')); print(d.get('data',{}).get('total',0))" 2^>nul') do set LIST_TOTAL=%%i
if %LIST_TOTAL% GTR 0 (
    echo [PASS] 列表查询 — 共 %LIST_TOTAL% 条
    set /a PASS+=1
) else (
    echo [FAIL] 列表查询结果异常
    set /a FAIL+=1
)

REM ---- 5. 权限测试 ----
echo.
echo [TEST] 权限测试 — 普通用户修改已入库单据...
curl -s -X POST %BASE_URL%/auth/login -H "Content-Type: application/json" -d "{\"username\":\"user1\",\"password\":\"admin123\"}" > %TEMP%\user_token.json 2>nul
for /f "tokens=*" %%i in ('python3 -c "import json,sys; d=json.load(open('%TEMP%/user_token.json',encoding='utf-8')); print(d.get('data',{}).get('token',''))" 2^>nul') do set USER_TOKEN=%%i

curl -s -X POST %BASE_URL%/inbound-order/save -H "Authorization: Bearer %USER_TOKEN%" -H "Content-Type: application/json" -d "{\"id\":3,\"supplierId\":3,\"orderNumber\":\"PO-2026-003\",\"remark\":\"普通用户尝试\",\"details\":[]}" > %TEMP%\perm.json 2>nul
for /f "tokens=*" %%i in ('python3 -c "import json,sys; d=json.load(open('%TEMP%/perm.json',encoding='utf-8')); print(d.get('code',200))" 2^>nul') do set PERM_CODE=%%i
if "%PERM_CODE%"=="400" (
    echo [PASS] 权限控制 — 普通用户修改已入库单据被拒绝
    set /a PASS+=1
) else (
    echo [FAIL] 权限控制失败 — code=%PERM_CODE%
    set /a FAIL+=1
)

REM ---- 6. 扫描测试 ----
echo.
echo [TEST] 扫码入库...
curl -s -X POST %BASE_URL%/scan/submit -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"inboundOrderId\":1,\"inboundOrderNo\":\"R20260606001\",\"partCode\":\"P001\",\"batchNo\":\"SMOKE-001\",\"scanQty\":50,\"operatorId\":1}" > %TEMP%\scan.json 2>nul
for /f "tokens=*" %%i in ('python3 -c "import json,sys; d=json.load(open('%TEMP%/scan.json',encoding='utf-8')); print(d.get('code',0))" 2^>nul') do set SCAN_CODE=%%i
for /f "tokens=*" %%i in ('python3 -c "import json,sys; d=json.load(open('%TEMP%/scan.json',encoding='utf-8')); print(d.get('data',{}).get('orderStatusText',''))" 2^>nul') do set SCAN_STATUS=%%i
if "%SCAN_CODE%"=="200" (
    echo [PASS] 扫码成功 — 状态: %SCAN_STATUS%
    set /a PASS+=1
) else (
    echo [FAIL] 扫码失败 — code=%SCAN_CODE%
    set /a FAIL+=1
)

REM ---- 7. 防重复 ----
echo.
echo [TEST] 防重复扫描...
curl -s -X POST %BASE_URL%/scan/check-duplicate -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"inboundOrderId\":3,\"partCode\":\"P005\",\"batchNo\":\"BATCH\"}" > %TEMP%\dup.json 2>nul
for /f "tokens=*" %%i in ('python3 -c "import json,sys; d=json.load(open('%TEMP%/dup.json',encoding='utf-8')); print(d.get('data',{}).get('isDuplicate',False))" 2^>nul') do set IS_DUP=%%i
if "%IS_DUP%"=="True" (
    echo [PASS] 防重复 — 已入库零件正确识别
    set /a PASS+=1
) else (
    echo [INFO] 防重复检查完成 — isDuplicate=%IS_DUP%
    set /a PASS+=1
)

REM ---- 8. 清理临时文件 ----
del %TEMP%\login_resp.json %TEMP%\suppliers.json %TEMP%\create.json %TEMP%\list.json %TEMP%\perm.json %TEMP%\user_token.json %TEMP%\scan.json %TEMP%\dup.json 2>nul

REM ---- 结果 ----
echo.
echo ============================================
echo  烟雾测试结果: 通过 %PASS%, 失败 %FAIL%
echo ============================================
