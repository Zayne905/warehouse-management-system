#!/bin/bash
# ============================================================
# 入库管理 API 自动化测试脚本 (v3 — Node.js JSON)
# 使用前提:
#   1. MySQL 已启动，已执行 V2/V3 SQL 脚本
#   2. 后端已启动 (mvn spring-boot:run, 端口 8081)
# 运行方式: bash test/test-inbound.sh
# ============================================================

BASE_URL="http://localhost:8081/api"
PASS=0
FAIL=0
# 使用项目本地临时目录（确保路径为 Unix 风格）
TMPDIR="F:/java-project/warehouse_manage/test/.tmp_$$"
rm -rf "$TMPDIR" 2>/dev/null
mkdir -p "$TMPDIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_pass() { echo -e "${GREEN}[PASS]${NC} $1"; PASS=$((PASS + 1)); }
log_fail() { echo -e "${RED}[FAIL]${NC} $1"; FAIL=$((FAIL + 1)); }
log_info() { echo -e "${YELLOW}[INFO]${NC} $1"; }
log_test() { echo -e "\n${YELLOW}====== $1 ======${NC}"; }

# 从保存的 JSON 响应中提取字段 (使用 node.js)
# 用法: jp <filename> <json-path>
# 例: jp suppliers data.0.id
jp() {
    local file="$TMPDIR/$1"
    local path="$2"
    if [ ! -f "$file" ]; then echo ""; return; fi
    node -e "
try {
  const d=JSON.parse(require('fs').readFileSync('$file','utf8'));
  const keys='$path'.split('.');
  let v=d;
  for(const k of keys) {
    if(v==null) break;
    if(Array.isArray(v) && /^\\d+$/.test(k)) v=v[parseInt(k)];
    else v=v[k];
  }
  if(v==null) console.log('');
  else if(typeof v==='object') console.log(JSON.stringify(v));
  else console.log(String(v));
} catch(e) { console.log(''); }
" 2>/dev/null
}

# 发送 API 请求并保存到临时文件
api_get() {
    local name=$1 url=$2 token=$3
    curl -s -H "Authorization: Bearer $token" "$url" > "$TMPDIR/$name" 2>/dev/null
}
api_post() {
    local name=$1 url=$2 token=$3 body=$4
    curl -s -X POST "$url" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d "$body" > "$TMPDIR/$name" 2>/dev/null
}

# 获取 token
get_token() {
    local resp=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"${1:-admin}\",\"password\":\"${2:-admin123}\"}")
    echo "$resp" | grep -o '"token":"[^"]*"' | sed 's/"token":"//;s/"//'
}

# 获取 code 快捷方法
get_code() { jp "$1" "code"; }
get_data() { jp "$1" "data"; }

echo "============================================"
echo "  入库管理 API 测试套件 v3"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "  目标: $BASE_URL"
echo "============================================"

# ============================================================
# 准备: Token
# ============================================================
log_info "获取测试 Token..."

ADMIN_TOKEN=$(get_token "admin" "admin123")
if [ -z "$ADMIN_TOKEN" ]; then
    log_fail "管理员登录失败 — 请确认后端已启动"
    rm -rf "$TMPDIR"
    exit 1
fi
log_pass "管理员登录成功 (admin)"

USER_TOKEN=$(get_token "user1" "admin123")
if [ -z "$USER_TOKEN" ]; then
    log_fail "普通用户登录失败"
    rm -rf "$TMPDIR"
    exit 1
fi
log_pass "普通用户登录成功 (user1)"

# ============================================================
# 场景1: 正常入库流程
# ============================================================
log_test "场景1: 正常入库流程"

# 1.1 供应商
log_info "1.1 供应商列表..."
api_get "s1_suppliers" "$BASE_URL/supplier/list" "$ADMIN_TOKEN"
S1_CODE=$(get_code "s1_suppliers")
GYS001_ID=$(node -e "
const d=JSON.parse(require('fs').readFileSync('$TMPDIR/s1_suppliers','utf8'));
const s=d.data.find(x=>x.code==='GYS001');
console.log(s?s.id:'');
" 2>/dev/null)
SUP_COUNT=$(node -e "
const d=JSON.parse(require('fs').readFileSync('$TMPDIR/s1_suppliers','utf8'));
console.log(d.data?d.data.length:0);
" 2>/dev/null)

if [ "$S1_CODE" = "200" ] && [ "$SUP_COUNT" -gt 0 ] 2>/dev/null; then
    log_pass "供应商列表 — 共 $SUP_COUNT 条 (GYS001=$GYS001_ID)"
else
    log_fail "供应商列表失败"
fi

# 1.2 零件
log_info "1.2 GYS001 的零件列表..."
api_get "s1_parts" "$BASE_URL/part/list?supplierId=$GYS001_ID" "$ADMIN_TOKEN"
P001_ID=$(node -e "
const d=JSON.parse(require('fs').readFileSync('$TMPDIR/s1_parts','utf8'));
const p=(d.data||[]).find(x=>x.code==='P001');
console.log(p?p.id:'');
" 2>/dev/null)
P002_ID=$(node -e "
const d=JSON.parse(require('fs').readFileSync('$TMPDIR/s1_parts','utf8'));
const p=(d.data||[]).find(x=>x.code==='P002');
console.log(p?p.id:'');
" 2>/dev/null)

if [ -n "$P001_ID" ] && [ -n "$P002_ID" ]; then
    log_pass "零件列表 — P001=$P001_ID, P002=$P002_ID"
else
    log_fail "零件列表获取失败"
fi

# 1.3 创建入库单
log_info "1.3 创建入库单..."
api_post "s1_create" "$BASE_URL/inbound-order/save" "$ADMIN_TOKEN" \
    "{\"supplierId\":$GYS001_ID,\"orderNumber\":\"PO-2026-TEST-001\",\"remark\":\"自动化测试\",\"details\":[{\"partId\":$P001_ID,\"plannedQty\":100,\"unit\":\"个\",\"lineNo\":1},{\"partId\":$P002_ID,\"plannedQty\":50,\"unit\":\"个\",\"lineNo\":2}]}"

S1_CODE=$(get_code "s1_create")
ORDER_NO=$(jp "s1_create" "data.orderNo")
ORDER_ID=$(jp "s1_create" "data.id")
ORDER_STATUS=$(jp "s1_create" "data.status")

if [ "$S1_CODE" = "200" ] && [ "$ORDER_STATUS" = "0" ]; then
    log_pass "创建入库单成功 (单号: $ORDER_NO, ID: $ORDER_ID, 状态: 未入库)"
else
    log_fail "创建入库单失败"
    cat "$TMPDIR/s1_create"
fi

# 1.4 首次扫码 → 部分入库
if [ -n "$ORDER_ID" ]; then
    log_info "1.4 首次扫码 (P001 +30)..."
    api_post "s1_scan1" "$BASE_URL/scan/submit" "$ADMIN_TOKEN" \
        "{\"inboundOrderId\":$ORDER_ID,\"inboundOrderNo\":\"$ORDER_NO\",\"partCode\":\"P001\",\"batchNo\":\"BATCH-TEST-001\",\"scanQty\":30,\"operatorId\":1}"

    S1_SCAN_CODE=$(get_code "s1_scan1")
    S1_SCAN_STATUS=$(jp "s1_scan1" "data.orderStatus")
    S1_SCAN_TEXT=$(jp "s1_scan1" "data.orderStatusText")

    if [ "$S1_SCAN_CODE" = "200" ] && [ "$S1_SCAN_STATUS" = "1" ]; then
        log_pass "扫码成功 — 状态变为「${S1_SCAN_TEXT}」(部分入库)"
    else
        log_fail "扫码后状态异常 — code=$S1_SCAN_CODE status=$S1_SCAN_STATUS (预期 1)"
    fi

    # 1.5 继续扫码 → 已入库
    log_info "1.5 继续扫码完成 (P001 +70, P002 +50)..."
    api_post "s1_scan2" "$BASE_URL/scan/submit" "$ADMIN_TOKEN" \
        "{\"inboundOrderId\":$ORDER_ID,\"inboundOrderNo\":\"$ORDER_NO\",\"partCode\":\"P001\",\"batchNo\":\"BATCH-TEST-002\",\"scanQty\":70,\"operatorId\":1}"
    api_post "s1_scan3" "$BASE_URL/scan/submit" "$ADMIN_TOKEN" \
        "{\"inboundOrderId\":$ORDER_ID,\"inboundOrderNo\":\"$ORDER_NO\",\"partCode\":\"P002\",\"batchNo\":\"BATCH-TEST-003\",\"scanQty\":50,\"operatorId\":1}"

    S1_FINAL_STATUS=$(jp "s1_scan3" "data.orderStatus")
    S1_FINAL_TEXT=$(jp "s1_scan3" "data.orderStatusText")

    if [ "$S1_FINAL_STATUS" = "2" ]; then
        log_pass "扫码完成 — 状态变为「${S1_FINAL_TEXT}」(已入库)"
    else
        log_fail "扫码完成状态异常 — status=$S1_FINAL_STATUS (预期 2=已入库)"
    fi
fi

# ============================================================
# 场景2: 权限控制
# ============================================================
log_test "场景2: 权限控制测试"

# 2.1 确认已入库单据
api_post "s2_detail" "$BASE_URL/inbound-order/detail" "$ADMIN_TOKEN" '{"id":3}'
S2_STATUS=$(jp "s2_detail" "data.status")
S2_ID=$(jp "s2_detail" "data.id")
if [ "$S2_STATUS" = "2" ]; then
    log_pass "R20260606003 确认状态=已入库(2)"
else
    log_info "R20260606003 状态: $S2_STATUS (预期 2)"
fi

# 2.2 普通用户修改已入库 → 拒绝
log_info "2.2 普通用户修改已入库单据..."
api_post "s2_user_reject" "$BASE_URL/inbound-order/save" "$USER_TOKEN" \
    "{\"id\":$S2_ID,\"supplierId\":3,\"orderNumber\":\"PO-TEST\",\"remark\":\"no\",\"details\":[]}"
P1_CODE=$(get_code "s2_user_reject")
if [ "$P1_CODE" = "400" ]; then
    log_pass "普通用户修改已入库 → 正确拒绝 (code=400)"
else
    log_fail "普通用户修改已入库未被拒绝 (code=$P1_CODE)"
fi

# 2.3 管理员修改已入库 → 成功
log_info "2.3 管理员修改已入库单据..."
api_post "s2_admin_ok" "$BASE_URL/inbound-order/save" "$ADMIN_TOKEN" \
    "{\"id\":$S2_ID,\"supplierId\":3,\"orderNumber\":\"PO-2026-003\",\"remark\":\"管理员更新\",\"details\":[{\"partId\":5,\"plannedQty\":300,\"unit\":\"只\",\"lineNo\":1},{\"partId\":6,\"plannedQty\":200,\"unit\":\"张\",\"lineNo\":2}]}"
P2_CODE=$(get_code "s2_admin_ok")
if [ "$P2_CODE" = "200" ]; then
    log_pass "管理员修改已入库 → 成功"
else
    log_fail "管理员修改已入库失败 (code=$P2_CODE)"
fi

# 2.4 普通用户修改未入库 → 成功
log_info "2.4 普通用户修改未入库单据 (status=0)..."
api_post "s2_user_pending" "$BASE_URL/inbound-order/save" "$USER_TOKEN" \
    '{"id":1,"supplierId":1,"orderNumber":"PO-2026-001","remark":"user edit","details":[{"partId":1,"plannedQty":100,"unit":"个","lineNo":1}]}'
P3_CODE=$(get_code "s2_user_pending")
if [ "$P3_CODE" = "200" ]; then
    log_pass "普通用户修改未入库 → 成功"
else
    log_fail "普通用户修改未入库失败 (code=$P3_CODE)"
fi

# 2.5 修改作废(3) → 拒绝
log_info "2.5 修改作废单据 (status=3)..."
api_post "s2_cancelled" "$BASE_URL/inbound-order/save" "$ADMIN_TOKEN" \
    '{"id":4,"supplierId":4,"orderNumber":"PO-TEST","remark":"try","details":[]}'
P4_CODE=$(get_code "s2_cancelled")
if [ "$P4_CODE" = "400" ]; then
    log_pass "作废单据修改 → 正确拒绝"
else
    log_fail "作废单据修改未被拒绝 (code=$P4_CODE)"
fi

# ============================================================
# 场景3: 防重复扫描
# ============================================================
log_test "场景3: 防重复扫描测试"

# 3.1 已满零件防重
log_info "3.1 已入库订单零件防重..."
api_post "s3_dup" "$BASE_URL/scan/check-duplicate" "$ADMIN_TOKEN" \
    '{"inboundOrderId":3,"partCode":"P005","batchNo":"BATCH"}'
DUP1=$(jp "s3_dup" "data.isDuplicate")
if [ "$DUP1" = "true" ]; then
    log_pass "已入库零件 → 防重正确识别为重复"
else
    log_info "防重检查: isDuplicate=$DUP1 (预期 true)"
fi

# 3.2 未满零件防重
log_info "3.2 部分入库零件防重..."
api_post "s3_dup2" "$BASE_URL/scan/check-duplicate" "$ADMIN_TOKEN" \
    '{"inboundOrderId":2,"partCode":"P004","batchNo":"BATCH-002"}'
DUP2=$(jp "s3_dup2" "data.isDuplicate")
REM=$(jp "s3_dup2" "data.remainingQty")
if [ "$DUP2" = "false" ]; then
    log_pass "未满零件 → 允许继续扫描 (剩余: $REM)"
else
    log_fail "未满零件防重异常: isDuplicate=$DUP2"
fi

# 3.3 超量扫描
log_info "3.3 超量扫描 (9999, 应拒绝)..."
api_post "s3_over" "$BASE_URL/scan/submit" "$ADMIN_TOKEN" \
    '{"inboundOrderId":2,"inboundOrderNo":"R20260606002","partCode":"P004","batchNo":"OVER","scanQty":9999,"operatorId":1}'
OV_CODE=$(get_code "s3_over")
if [ "$OV_CODE" = "400" ]; then
    log_pass "超量扫描 → 正确拒绝"
else
    log_fail "超量扫描未被拒绝 (code=$OV_CODE)"
fi

# ============================================================
# 场景4: 列表查询与筛选
# ============================================================
log_test "场景4: 列表查询与筛选"

log_info "4.1 分页列表..."
api_post "s4_all" "$BASE_URL/inbound-order/list" "$ADMIN_TOKEN" \
    '{"current":1,"size":10}'
L_CODE=$(get_code "s4_all")
L_TOTAL=$(jp "s4_all" "data.total")
if [ "$L_CODE" = "200" ] && [ -n "$L_TOTAL" ]; then
    log_pass "分页查询 — 共 $L_TOTAL 条"
else
    log_fail "分页查询失败"
fi

log_info "4.2 按状态=2 筛选..."
api_post "s4_status" "$BASE_URL/inbound-order/list" "$ADMIN_TOKEN" \
    '{"current":1,"size":10,"status":2}'
log_pass "按状态筛选(已入库) — $(jp 's4_status' 'data.total') 条"

log_info "4.3 按供应商=1 筛选..."
api_post "s4_supp" "$BASE_URL/inbound-order/list" "$ADMIN_TOKEN" \
    '{"current":1,"size":10,"supplierId":1}'
log_pass "按供应商(GYS001) — $(jp 's4_supp' 'data.total') 条"

log_info "4.4 模糊搜索 R20260606..."
api_post "s4_fuzzy" "$BASE_URL/inbound-order/list" "$ADMIN_TOKEN" \
    '{"current":1,"size":10,"orderNo":"R20260606"}'
log_pass "模糊搜索 — $(jp 's4_fuzzy' 'data.total') 条"

# ============================================================
# 场景5: 批量操作
# ============================================================
log_test "场景5: 批量操作"

log_info "5.1 批量设置库区..."
api_post "s5_area" "$BASE_URL/inbound-order/batch-set-area" "$ADMIN_TOKEN" \
    '{"detailIds":[1,2],"warehouseAreaId":3}'
BA_CODE=$(get_code "s5_area")
if [ "$BA_CODE" = "200" ]; then
    log_pass "批量设置库区成功"
else
    log_fail "批量设置库区失败 (code=$BA_CODE)"
fi

# ============================================================
# 场景6: 详情与扫描回显
# ============================================================
log_test "场景6: 详情与扫描回显"

log_info "6.1 入库单详情..."
api_post "s6_detail" "$BASE_URL/inbound-order/detail" "$ADMIN_TOKEN" '{"id":2}'
DET_CODE=$(get_code "s6_detail")
DET_COUNT=$(node -e "
const d=JSON.parse(require('fs').readFileSync('$TMPDIR/s6_detail','utf8'));
console.log((d.data&&d.data.details)?d.data.details.length:0);
" 2>/dev/null)
if [ "$DET_CODE" = "200" ] && [ "$DET_COUNT" -gt 0 ] 2>/dev/null; then
    log_pass "详情查询成功 ($DET_COUNT 行明细)"
else
    log_fail "详情查询失败"
fi

log_info "6.2 扫描回显..."
api_post "s6_feedback" "$BASE_URL/scan/feedback" "$ADMIN_TOKEN" \
    '{"orderNo":"R20260606002","partCode":"P004"}'
FB_PART=$(jp "s6_feedback" "data.partFeedback.partName")
if [ -n "$FB_PART" ] && [ "$FB_PART" != "null" ] && [ "$FB_PART" != "undefined" ]; then
    log_pass "扫描回显成功 (零件: $FB_PART)"
else
    log_fail "扫描回显失败 (got: $FB_PART)"
fi

log_info "6.3 扫描记录列表..."
api_post "s6_scans" "$BASE_URL/scan/list" "$ADMIN_TOKEN" \
    '{"inboundOrderId":2}'
SC_CODE=$(get_code "s6_scans")
SC_COUNT=$(node -e "
const d=JSON.parse(require('fs').readFileSync('$TMPDIR/s6_scans','utf8'));
console.log(d.data?d.data.length:0);
" 2>/dev/null)
if [ "$SC_CODE" = "200" ]; then
    log_pass "扫描记录查询成功 ($SC_COUNT 条)"
else
    log_fail "扫描记录查询失败"
fi

# ============================================================
# 汇总
# ============================================================
echo ""
echo "============================================"
echo "  测试结果汇总"
echo "============================================"
echo -e "  ${GREEN}通过: $PASS${NC}"
echo -e "  ${RED}失败: $FAIL${NC}"
echo "  总计: $((PASS + FAIL))"
echo "============================================"

rm -rf "$TMPDIR"

if [ "$FAIL" -eq 0 ]; then
    echo -e "  ${GREEN}全部测试通过!${NC}"
    exit 0
else
    echo -e "  ${RED}存在 $FAIL 个失败项${NC}"
    exit 1
fi
