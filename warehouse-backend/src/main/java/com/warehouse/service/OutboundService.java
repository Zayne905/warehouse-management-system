package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.mapper.*;
import com.warehouse.model.dto.PageResult;
import com.warehouse.model.entity.*;
import com.warehouse.model.enums.OutboundStatus;
import com.warehouse.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OutboundService {

    private final OutboundOrderMapper orderMapper;
    private final OutboundOrderDetailMapper detailMapper;
    private final OutboundScanMapper scanMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final PartService partService;
    private final InboundOrderDetailMapper inboundDetailMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final KanbanMapper kanbanMapper;

    public OutboundService(OutboundOrderMapper orderMapper,
                           OutboundOrderDetailMapper detailMapper,
                           OutboundScanMapper scanMapper,
                           OrderNoGenerator orderNoGenerator,
                           PartService partService,
                           InboundOrderDetailMapper inboundDetailMapper,
                           InboundOrderMapper inboundOrderMapper,
                           KanbanMapper kanbanMapper) {
        this.orderMapper = orderMapper;
        this.detailMapper = detailMapper;
        this.scanMapper = scanMapper;
        this.orderNoGenerator = orderNoGenerator;
        this.partService = partService;
        this.inboundDetailMapper = inboundDetailMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.kanbanMapper = kanbanMapper;
    }

    // ========== 出库单号生成 ==========

    private synchronized String generateOrderNo() {
        String prefix = "C" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        QueryWrapper<OutboundOrder> wrapper = new QueryWrapper<OutboundOrder>()
                .likeRight("order_no", prefix)
                .orderByDesc("order_no")
                .last("LIMIT 1");
        OutboundOrder last = orderMapper.selectOne(wrapper);
        int seq = 1;
        if (last != null) {
            String lastNo = last.getOrderNo();
            try { seq = Integer.parseInt(lastNo.substring(prefix.length())) + 1; } catch (Exception ignore) {}
        }
        return prefix + String.format("%03d", seq);
    }

    // ========== 列表 ==========

    public PageResult<Map<String, Object>> list(int current, int size, String orderNo, Integer status,
                                                 String supplier, String customerName) {
        QueryWrapper<OutboundOrder> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(orderNo)) wrapper.like("order_no", orderNo);
        if (status != null) wrapper.eq("status", status);
        if (StringUtils.hasText(customerName)) wrapper.like("customer_name", customerName);
        wrapper.orderByDesc("create_time");

        Page<OutboundOrder> page = new Page<>(current, size);
        Page<OutboundOrder> result = orderMapper.selectPage(page, wrapper);

        List<Map<String, Object>> list = result.getRecords().stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("status", o.getStatus());
            m.put("statusText", getStatusText(o.getStatus()));
            m.put("remark", o.getRemark());
            m.put("customerName", o.getCustomerName());
            m.put("createTime", o.getCreateTime() != null
                    ? o.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            // 统计零件种类和计划数量
            List<OutboundOrderDetail> details = detailMapper.selectList(
                    new QueryWrapper<OutboundOrderDetail>().eq("outbound_order_id", o.getId()));
            int partCount = details.size();
            BigDecimal totalQty = details.stream()
                    .map(d -> d.getPlannedQty() != null ? d.getPlannedQty() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalActualQty = details.stream()
                    .map(d -> d.getActualQty() != null ? d.getActualQty() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            m.put("partCount", partCount);
            m.put("totalQty", totalQty);
            m.put("totalActualQty", totalActualQty);
            // 进度
            Long outboundCount = scanMapper.selectCount(
                    new QueryWrapper<OutboundScan>().eq("outbound_order_id", o.getId()));
            Long totalKanbans = kanbanMapper.selectCount(
                    new QueryWrapper<Kanban>().eq("outbound_order_id", o.getId()));
            m.put("outboundCount", outboundCount);
            m.put("totalKanbans", totalKanbans);
            // 零件级完成统计
            long completedParts = details.stream()
                .filter(d -> {
                    BigDecimal a = d.getActualQty() != null ? d.getActualQty() : BigDecimal.ZERO;
                    BigDecimal p = d.getPlannedQty() != null ? d.getPlannedQty() : BigDecimal.ZERO;
                    return a.compareTo(p) >= 0;
                }).count();
            m.put("completedParts", completedParts);
            m.put("totalParts", (long) details.size());
            // 供应商（从看板自动聚合，去重）
            List<Kanban> allKanbans = kanbanMapper.selectList(
                    new QueryWrapper<Kanban>().eq("outbound_order_id", o.getId()));
            Set<String> suppliers = new LinkedHashSet<>();
            for (Kanban k : allKanbans) {
                if (k.getSupplierName() != null && !k.getSupplierName().isEmpty()) {
                    suppliers.add(k.getSupplierName());
                }
            }
            m.put("suppliers", suppliers);
            return m;
        }).collect(Collectors.toList());

        // 按供应商筛选（后过滤）
        long total = result.getTotal();
        if (StringUtils.hasText(supplier)) {
            list = list.stream()
                    .filter(m -> {
                        @SuppressWarnings("unchecked")
                        Set<String> s = (Set<String>) m.get("suppliers");
                        return s != null && s.stream().anyMatch(n -> n.contains(supplier));
                    })
                    .collect(Collectors.toList());
            total = list.size();
        }

        return PageResult.of(list, total, result.getCurrent(), result.getSize());
    }

    // ========== 详情 ==========

    public Map<String, Object> getDetail(Long id) {
        OutboundOrder order = orderMapper.selectById(id);
        if (order == null) throw new RuntimeException("出库单不存在");
        Map<String, Object> m = new HashMap<>();
        m.put("id", order.getId());
        m.put("orderNo", order.getOrderNo());
        m.put("status", order.getStatus());
        m.put("statusText", getStatusText(order.getStatus()));
        m.put("remark", order.getRemark());
        m.put("customerName", order.getCustomerName());
        m.put("createTime", order.getCreateTime() != null
                ? order.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");

        // 零件级明细
        List<OutboundOrderDetail> details = detailMapper.selectList(
                new QueryWrapper<OutboundOrderDetail>().eq("outbound_order_id", id).orderByAsc("line_no"));
        List<Map<String, Object>> detailList = details.stream().map(d -> {
            Map<String, Object> dm = new HashMap<>();
            dm.put("id", d.getId());
            dm.put("partId", d.getPartId());
            dm.put("partCode", d.getPartCode());
            dm.put("partName", d.getPartName());
            dm.put("unit", d.getUnit());
            dm.put("plannedQty", d.getPlannedQty());
            dm.put("actualQty", d.getActualQty());
            dm.put("boxCount", d.getBoxCount());
            dm.put("warehouseAreaId", d.getWarehouseAreaId());
            dm.put("lineNo", d.getLineNo());
            dm.put("availableStock", getAvailableStock(d.getPartId()));
            // 零件级完成状态
            BigDecimal planned = d.getPlannedQty() != null ? d.getPlannedQty() : BigDecimal.ZERO;
            BigDecimal actual = d.getActualQty() != null ? d.getActualQty() : BigDecimal.ZERO;
            double rate = planned.compareTo(BigDecimal.ZERO) > 0
                ? actual.divide(planned, 4, RoundingMode.HALF_UP).doubleValue()
                : (actual.compareTo(BigDecimal.ZERO) > 0 ? 1.0 : 0.0);
            dm.put("completionRate", rate);
            dm.put("completionStatus", actual.compareTo(planned) >= 0 ? "done"
                : (actual.compareTo(BigDecimal.ZERO) > 0 ? "partial" : "none"));
            return dm;
        }).collect(Collectors.toList());
        m.put("details", detailList);

        // 零件级完成汇总
        long completedParts = detailList.stream()
            .filter(d -> "done".equals(d.get("completionStatus"))).count();
        m.put("completedPartCount", completedParts);
        m.put("totalPartCount", (long) detailList.size());

        // 待出库清单（锁定的看板）
        List<Kanban> pendingKanbans = kanbanMapper.selectList(
                new QueryWrapper<Kanban>()
                        .eq("outbound_order_id", id)
                        .eq("status", Kanban.STATUS_LOCKED)
                        .orderByAsc("box_seq"));
        m.put("pendingKanbans", pendingKanbans);

        // 已出库扫描记录
        List<OutboundScan> scans = scanMapper.selectList(
                new QueryWrapper<OutboundScan>().eq("outbound_order_id", id).orderByDesc("scan_time"));
        m.put("scans", scans);

        // 出库进度
        long totalLocked = kanbanMapper.selectCount(
                new QueryWrapper<Kanban>().eq("outbound_order_id", id));
        m.put("totalKanbans", totalLocked);
        m.put("outboundCount", scans.size());

        // 供应商（从看板自动聚合）
        List<Kanban> allKanbans = kanbanMapper.selectList(
                new QueryWrapper<Kanban>().eq("outbound_order_id", id));
        Set<String> suppliers = new LinkedHashSet<>();
        for (Kanban k : allKanbans) {
            if (k.getSupplierName() != null && !k.getSupplierName().isEmpty()) {
                suppliers.add(k.getSupplierName());
            }
        }
        m.put("suppliers", suppliers);

        return m;
    }

    /**
     * 按单号查询出库单（供 Android 扫码端选择出库单时使用）
     */
    public Map<String, Object> getDetailByOrderNo(String orderNo) {
        OutboundOrder order = orderMapper.selectOne(
                new QueryWrapper<OutboundOrder>().eq("order_no", orderNo));
        if (order == null) throw new RuntimeException("出库单不存在: " + orderNo);
        Long id = order.getId();

        Map<String, Object> m = new HashMap<>();
        m.put("id", order.getId());
        m.put("orderNo", order.getOrderNo());
        m.put("status", order.getStatus());
        m.put("statusText", getStatusText(order.getStatus()));
        m.put("customerName", order.getCustomerName());
        m.put("remark", order.getRemark());

        // 零件清单
        List<OutboundOrderDetail> details = detailMapper.selectList(
                new QueryWrapper<OutboundOrderDetail>().eq("outbound_order_id", id).orderByAsc("line_no"));
        List<Map<String, Object>> partList = details.stream().map(d -> {
            Map<String, Object> dm = new HashMap<>();
            dm.put("partId", d.getPartId());
            dm.put("partCode", d.getPartCode());
            dm.put("partName", d.getPartName());
            dm.put("plannedQty", d.getPlannedQty());
            return dm;
        }).collect(Collectors.toList());
        m.put("parts", partList);
        m.put("partCount", details.size());
        return m;
    }

    // ========== 保存出库单（含自动匹配） ==========

    @Transactional
    public Map<String, Object> save(Map<String, Object> dto) {
        Long id = dto.get("id") != null ? Long.valueOf(dto.get("id").toString()) : null;
        if (id != null) return update(dto);

        // 新建
        OutboundOrder order = new OutboundOrder();
        order.setOrderNo(generateOrderNo());
        order.setStatus(OutboundStatus.PENDING_OUT.getCode());
        order.setRemark((String) dto.get("remark"));
        order.setCustomerName((String) dto.get("customerName"));
        order.setCreateUserId(SecurityUtils.getCurrentUserId());
        orderMapper.insert(order);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> details = (List<Map<String, Object>>) dto.get("details");
        if (details != null) {
            saveDetails(order.getId(), details);
            // 不再自动匹配：用户可在详情页手动触发FIFO匹配，或直接非FIFO扫码出库
        }

        return getDetail(order.getId());
    }

    private Map<String, Object> update(Map<String, Object> dto) {
        Long id = Long.valueOf(dto.get("id").toString());
        OutboundOrder order = orderMapper.selectById(id);
        if (order == null) throw new RuntimeException("出库单不存在");
        if (order.getStatus() == OutboundStatus.COMPLETED.getCode()
                || order.getStatus() == OutboundStatus.CANCELLED.getCode()) {
            throw new RuntimeException("已出库或已作废的出库单不允许修改");
        }

        // 删除旧明细
        detailMapper.delete(new QueryWrapper<OutboundOrderDetail>().eq("outbound_order_id", id));

        order.setRemark((String) dto.get("remark"));
        order.setCustomerName((String) dto.get("customerName"));
        orderMapper.updateById(order);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> details = (List<Map<String, Object>>) dto.get("details");
        if (details != null) {
            saveDetails(id, details);
            // 不再自动重新匹配，用户可手动触发或直接非FIFO扫码
        }

        return getDetail(id);
    }

    private void saveDetails(Long orderId, List<Map<String, Object>> detailList) {
        int lineNo = 1;
        for (Map<String, Object> dm : detailList) {
            Long partId = Long.valueOf(dm.get("partId").toString());
            Part part = partService.getById(partId);
            if (part == null) throw new RuntimeException("零件不存在: " + partId);

            OutboundOrderDetail detail = new OutboundOrderDetail();
            detail.setOutboundOrderId(orderId);
            detail.setPartId(partId);
            detail.setPartCode(part.getCode());
            detail.setPartName(part.getName());
            detail.setUnit(dm.get("unit") != null ? (String) dm.get("unit") : part.getUnit());
            detail.setPlannedQty(dm.get("plannedQty") != null
                    ? new BigDecimal(dm.get("plannedQty").toString()) : BigDecimal.ZERO);
            detail.setActualQty(BigDecimal.ZERO);
            detail.setBoxCount(dm.get("boxCount") != null
                    ? Integer.valueOf(dm.get("boxCount").toString()) : 0);
            if (dm.get("warehouseAreaId") != null) {
                detail.setWarehouseAreaId(Long.valueOf(dm.get("warehouseAreaId").toString()));
            }
            detail.setLineNo(lineNo++);
            detailMapper.insert(detail);
        }
    }

    // ========== FIFO自动匹配看板 ==========

    @Transactional
    public void autoMatchKanbans(Long orderId) {
        OutboundOrder order = orderMapper.selectById(orderId);
        String orderNo = order != null ? order.getOrderNo() : null;
        // 获取出库单所有零件级明细
        List<OutboundOrderDetail> details = detailMapper.selectList(
                new QueryWrapper<OutboundOrderDetail>().eq("outbound_order_id", orderId));

        for (OutboundOrderDetail detail : details) {
            BigDecimal needed = detail.getPlannedQty();
            if (needed.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 查询该零件的在库可用看板（FIFO：按入库时间升序），包括部分转出的看板
            List<Kanban> available = kanbanMapper.selectList(
                    new QueryWrapper<Kanban>()
                            .eq("part_id", detail.getPartId())
                            .in("status", Kanban.STATUS_AVAILABLE, Kanban.STATUS_PARTIAL_REPACK)
                            .orderByAsc("create_time", "box_seq"));

            // 如果没有在库可用的看板，尝试将待入库看板自动提升为在库可用
            // （兼容看板已生成但未扫码入库的场景，如测试数据或直接导入的入库明细）
            if (available.isEmpty()) {
                List<Kanban> pending = kanbanMapper.selectList(
                        new QueryWrapper<Kanban>()
                                .eq("part_id", detail.getPartId())
                                .eq("status", Kanban.STATUS_PENDING_INBOUND)
                                .orderByAsc("create_time", "box_seq"));
                for (Kanban k : pending) {
                    k.setStatus(Kanban.STATUS_AVAILABLE);
                    kanbanMapper.updateById(k);
                }
                available = pending;
            }

            // 如果该零件没有任何看板记录（兼容 V5 看板系统引入前的老数据），
            // 从入库明细自动生成看板
            if (available.isEmpty()) {
                available = autoGenerateKanbansFromDetails(detail.getPartId());
            }

            BigDecimal accumulated = BigDecimal.ZERO;
            BigDecimal remaining = needed;
            for (Kanban kanban : available) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal kanbanQtyBd = kanban.getQuantity();

                if (kanbanQtyBd.compareTo(remaining) <= 0) {
                    // 整箱出：看板数量 ≤ 剩余需要量
                    kanban.setStatus(Kanban.STATUS_LOCKED);
                    kanban.setOutboundOrderId(orderId);
                    kanban.setOutboundOrderNo(orderNo);
                    kanbanMapper.updateById(kanban);
                    accumulated = accumulated.add(kanbanQtyBd);
                    remaining = remaining.subtract(kanbanQtyBd);
                } else {
                    // 看板数量 > 剩余需要量：锁定整箱，拆箱在扫码出库时由溢出逻辑处理
                    kanban.setStatus(Kanban.STATUS_LOCKED);
                    kanban.setOutboundOrderId(orderId);
                    kanban.setOutboundOrderNo(orderNo);
                    kanbanMapper.updateById(kanban);
                    accumulated = accumulated.add(kanbanQtyBd);
                    remaining = remaining.subtract(kanbanQtyBd);
                }
            }

            if (accumulated.compareTo(needed) < 0) {
                // 库存不足，回滚已锁定的看板（通过事务回滚）
                throw new RuntimeException(
                        "零件 " + detail.getPartCode() + " " + detail.getPartName()
                                + " 可用库存不足，需要 " + needed + "，可用 " + accumulated);
            }
        }
    }

    /**
     * 从入库明细自动生成看板（兼容看板系统引入前的老数据）
     * 有 actual_qty > 0 但还没有对应看板的入库明细行，各自生成一个看板
     */
    private List<Kanban> autoGenerateKanbansFromDetails(Long partId) {
        List<InboundOrderDetail> ibDetails = inboundDetailMapper.selectList(
                new QueryWrapper<InboundOrderDetail>()
                        .eq("part_id", partId)
                        .gt("actual_qty", 0)
                        .orderByAsc("create_time"));

        List<Kanban> kanbans = new ArrayList<>();
        for (InboundOrderDetail ibd : ibDetails) {
            // 查入库单获取单号和供应商
            InboundOrder ibOrder = inboundOrderMapper.selectById(ibd.getInboundOrderId());
            String ibOrderNo = ibOrder != null ? ibOrder.getOrderNo() : "UNKNOWN";
            String supplierName = ibOrder != null ? ibOrder.getSupplierName() : "";

            int boxCount = ibd.getBoxCount() != null && ibd.getBoxCount().compareTo(BigDecimal.ZERO) > 0
                    ? ibd.getBoxCount().setScale(0, RoundingMode.CEILING).intValue() : 1;
            int qtyPerBox = ibd.getActualQty().intValue() / boxCount;
            if (qtyPerBox <= 0) qtyPerBox = ibd.getActualQty().intValue();

            for (int seq = 0; seq < boxCount; seq++) {
                Kanban k = new Kanban();
                k.setKanbanNo("AUTO-" + ibd.getPartCode() + "-" + ibd.getId() + "C-" + seq);
                k.setInboundOrderId(ibd.getInboundOrderId());
                k.setInboundOrderNo(ibOrderNo);
                k.setPartId(ibd.getPartId());
                k.setPartCode(ibd.getPartCode());
                k.setPartName(ibd.getPartName());
                k.setSupplierName(supplierName);
                k.setQuantity(BigDecimal.valueOf(seq == boxCount - 1
                        ? ibd.getActualQty().intValue() - qtyPerBox * seq
                        : qtyPerBox));
                Part part = partService.getById(ibd.getPartId());
                BigDecimal capacity = BigDecimal.valueOf(
                        part != null && part.getPackageCapacity() != null
                                ? part.getPackageCapacity() : qtyPerBox);
                k.setOriginalQty(capacity);
                k.setBoxSeq(seq);
                k.setWarehouseAreaId(ibd.getWarehouseAreaId());
                k.setStatus(Kanban.STATUS_AVAILABLE); // 直接设为在库可用
                kanbanMapper.insert(k);
                kanbans.add(k);
            }
        }
        return kanbans;
    }

    // ========== 释放锁定看板 ==========

    private void releaseLockedKanbans(Long orderId) {
        List<Kanban> locked = kanbanMapper.selectList(
                new QueryWrapper<Kanban>()
                        .eq("outbound_order_id", orderId)
                        .eq("status", Kanban.STATUS_LOCKED));
        for (Kanban k : locked) {
            k.setStatus(Kanban.STATUS_AVAILABLE);
            k.setOutboundOrderId(null);
            kanbanMapper.updateById(k);
        }
    }

    // ========== 删除 ==========

    @Transactional
    public void delete(Long id) {
        // 释放锁定看板
        releaseLockedKanbans(id);
        detailMapper.delete(new QueryWrapper<OutboundOrderDetail>().eq("outbound_order_id", id));
        scanMapper.delete(new QueryWrapper<OutboundScan>().eq("outbound_order_id", id));
        orderMapper.deleteById(id);
    }

    // ========== 作废 ==========

    @Transactional
    public void cancel(Long id) {
        OutboundOrder order = orderMapper.selectById(id);
        if (order == null) throw new RuntimeException("出库单不存在");
        if (!SecurityUtils.isAdmin()) throw new RuntimeException("只有管理员才能作废");
        if (order.getStatus() == OutboundStatus.CANCELLED.getCode()) throw new RuntimeException("已作废");

        // 释放锁定看板
        releaseLockedKanbans(id);

        order.setStatus(OutboundStatus.CANCELLED.getCode());
        orderMapper.updateById(order);
    }

    // ========== 扫码出库 ==========

    @Transactional
    public Map<String, Object> scanOutbound(Long orderId, String kanbanNo, Integer operatorId, boolean confirmNonFifo) {
        // 1. 查找看板
        Kanban kanban = kanbanMapper.selectOne(
                new QueryWrapper<Kanban>().eq("kanban_no", kanbanNo));
        if (kanban == null) throw new RuntimeException("看板不存在: " + kanbanNo);

        boolean autoAdded = false;
        boolean crossOrdered = false;

        // 2. 状态校验
        if (kanban.getStatus() == Kanban.STATUS_OUTBOUND) {
            throw new RuntimeException("该条码已出库，不能重复出库");
        }
        if (kanban.getStatus() == Kanban.STATUS_BLOCKED) {
            throw new RuntimeException("该条码已被封存，不能出库");
        }
        if (kanban.getStatus() == Kanban.STATUS_PENDING_INBOUND) {
            throw new RuntimeException("该条码尚未入库，不能出库");
        }
        if (kanban.getStatus() == Kanban.STATUS_CLEARED) {
            throw new RuntimeException("该条码已被清空（完全转出），不能出库");
        }
        if (kanban.getStatus() == Kanban.STATUS_PARTIAL_REPACK) {
            throw new RuntimeException("该条码已部分转出，请先匹配到出库单后再出库");
        }

        // 3. 处理 STATUS_AVAILABLE（在库可用）—— 非FIFO手动出库
        if (kanban.getStatus() == Kanban.STATUS_AVAILABLE) {
            if (orderId == null) {
                throw new RuntimeException("该条码尚未被任何出库单匹配。请先选择出库单或通过Web端创建出库单匹配此看板。");
            }
            // 校验出库单存在且未作废
            OutboundOrder order = orderMapper.selectById(orderId);
            if (order == null) throw new RuntimeException("出库单不存在");
            if (order.getStatus() == OutboundStatus.CANCELLED.getCode())
                throw new RuntimeException("出库单已作废，不能出库");

            // 校验该看板的零件是否在出库单明细中
            validatePartOnOrder(orderId, kanban.getPartId());

            // 非FIFO出库需用户确认
            if (!confirmNonFifo) {
                Map<String, Object> confirmResult = new HashMap<>();
                confirmResult.put("needsConfirm", true);
                confirmResult.put("kanbanNo", kanban.getKanbanNo());
                confirmResult.put("partCode", kanban.getPartCode());
                confirmResult.put("partName", kanban.getPartName());
                confirmResult.put("quantity", kanban.getQuantity());
                confirmResult.put("message", "该看板（" + kanban.getPartName()
                        + " x" + kanban.getQuantity() + "）未按FIFO顺序匹配到此出库单，是否确认出库？");
                return confirmResult;
            }

            // 自动锁定看板到此出库单
            kanban.setOutboundOrderId(orderId);
            kanban.setOutboundOrderNo(order.getOrderNo());
            autoAdded = true;
        }

        // 4. 处理 STATUS_LOCKED 跨单转移
        if (kanban.getStatus() == Kanban.STATUS_LOCKED
                && orderId != null
                && !orderId.equals(kanban.getOutboundOrderId())) {
            // 从旧出库单释放
            releaseKanbanFromOrder(kanban);
            // 校验新出库单
            OutboundOrder newOrder = orderMapper.selectById(orderId);
            if (newOrder == null) throw new RuntimeException("目标出库单不存在");
            if (newOrder.getStatus() == OutboundStatus.CANCELLED.getCode())
                throw new RuntimeException("目标出库单已作废，不能出库");
            // 校验零件是否在新单明细中
            validatePartOnOrder(orderId, kanban.getPartId());
            // 重新锁定到新出库单
            kanban.setOutboundOrderId(orderId);
            kanban.setOutboundOrderNo(newOrder.getOrderNo());
            crossOrdered = true;
        }

        // 5. 原有校验：LOCKED 但未传 orderId（Android 兼容）
        if (kanban.getStatus() == Kanban.STATUS_LOCKED && orderId == null) {
            // 使用看板自身的 orderId，保持向后兼容
        }

        Long effectiveOrderId = orderId != null ? orderId : kanban.getOutboundOrderId();
        if (effectiveOrderId == null) {
            throw new RuntimeException("无法确定出库单，请提供出库单号");
        }

        // 6. 计算本次实际出库数量（处理超量拆分）
        List<OutboundOrderDetail> details = detailMapper.selectList(
                new QueryWrapper<OutboundOrderDetail>()
                        .eq("outbound_order_id", effectiveOrderId)
                        .eq("part_id", kanban.getPartId()));
        BigDecimal scanQty = kanban.getQuantity();
        boolean overflowSplit = false;
        BigDecimal overflowQty = BigDecimal.ZERO;

        if (!details.isEmpty()) {
            OutboundOrderDetail firstDetail = details.get(0);
            BigDecimal currentActual = firstDetail.getActualQty() != null ? firstDetail.getActualQty() : BigDecimal.ZERO;
            BigDecimal planned = firstDetail.getPlannedQty() != null ? firstDetail.getPlannedQty() : BigDecimal.ZERO;
            BigDecimal newTotal = currentActual.add(scanQty);

            if (planned.compareTo(BigDecimal.ZERO) > 0 && newTotal.compareTo(planned) > 0) {
                // 超量：拆分看板，超出部分作为余量转包入库
                BigDecimal outboundQty = planned.subtract(currentActual);
                if (outboundQty.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("该零件计划出库 " + planned
                            + "，实出已达 " + currentActual + "，无法继续出库");
                }
                BigDecimal remainderQty = scanQty.subtract(outboundQty);
                overflowSplit = true;
                overflowQty = remainderQty;

                // 生成余量新看板
                Kanban remainderKanban = new Kanban();
                remainderKanban.setKanbanNo(kanban.getKanbanNo() + "-OB-" + System.nanoTime() % 1_000_000);
                remainderKanban.setInboundOrderId(kanban.getInboundOrderId());
                remainderKanban.setInboundOrderNo(kanban.getInboundOrderNo());
                remainderKanban.setPartId(kanban.getPartId());
                remainderKanban.setPartCode(kanban.getPartCode());
                remainderKanban.setPartName(kanban.getPartName());
                remainderKanban.setSupplierName(kanban.getSupplierName());
                remainderKanban.setQuantity(remainderQty);
                remainderKanban.setOriginalQty(kanban.getOriginalQty() != null ? kanban.getOriginalQty() : scanQty);
                remainderKanban.setBoxSeq(kanban.getBoxSeq() != null ? kanban.getBoxSeq() : 0);
                remainderKanban.setWarehouseAreaId(kanban.getWarehouseAreaId());
                remainderKanban.setWarehouseAreaName(kanban.getWarehouseAreaName());
                remainderKanban.setStatus(Kanban.STATUS_AVAILABLE);
                // 生成二维码内容
                remainderKanban.setQrContent(buildRemainderQr(remainderKanban));
                kanbanMapper.insert(remainderKanban);

                // 自动创建入库单记录余量
                String inboundNo = orderNoGenerator.generate();
                OutboundOrder refOrder = orderMapper.selectById(effectiveOrderId);
                String refOrderNo = refOrder != null ? refOrder.getOrderNo() : String.valueOf(effectiveOrderId);
                InboundOrder ib = new InboundOrder();
                ib.setOrderNo(inboundNo);
                ib.setSupplierId(0L);
                ib.setSupplierName("出库超量转包");
                ib.setOrderNumber(refOrderNo);
                ib.setStatus(2);
                ib.setRemark("出库单[" + refOrderNo
                        + "]出库超量，余量 " + remainderQty + " 件自动转包入库");
                inboundOrderMapper.insert(ib);

                InboundOrderDetail ibDetail = new InboundOrderDetail();
                ibDetail.setInboundOrderId(ib.getId());
                ibDetail.setPartId(remainderKanban.getPartId());
                ibDetail.setPartCode(remainderKanban.getPartCode());
                ibDetail.setPartName(remainderKanban.getPartName());
                ibDetail.setUnit("件");
                ibDetail.setPlannedQty(remainderQty);
                ibDetail.setActualQty(remainderQty);
                ibDetail.setWarehouseAreaId(remainderKanban.getWarehouseAreaId());
                ibDetail.setBoxCount(BigDecimal.ONE);
                ibDetail.setLineNo(1);
                inboundDetailMapper.insert(ibDetail);

                remainderKanban.setInboundOrderId(ib.getId());
                remainderKanban.setInboundOrderNo(inboundNo);
                kanbanMapper.updateById(remainderKanban);

                // 原看板数量缩减为实际出库量
                kanban.setQuantity(outboundQty);
                scanQty = outboundQty;
            }
        }

        // 7. 创建出库扫描记录
        OutboundOrder order = orderMapper.selectById(effectiveOrderId);
        OutboundScan scan = new OutboundScan();
        scan.setOutboundOrderId(effectiveOrderId);
        scan.setKanbanNo(kanban.getKanbanNo());
        scan.setPartId(kanban.getPartId());
        scan.setPartCode(kanban.getPartCode());
        scan.setPartName(kanban.getPartName());
        scan.setQuantity(scanQty);
        scan.setWarehouseAreaId(kanban.getWarehouseAreaId());
        scan.setWarehouseAreaName(kanban.getWarehouseAreaName());
        scan.setScanTime(LocalDateTime.now());
        scan.setOperatorId(operatorId != null ? Long.valueOf(operatorId) : null);
        if (order != null) scan.setOutboundOrderNo(order.getOrderNo());
        scanMapper.insert(scan);

        // 8. 更新看板状态为已出库，清空库位绑定
        kanban.setStatus(Kanban.STATUS_OUTBOUND);
        kanban.setWarehouseAreaId(null);
        kanban.setWarehouseAreaName(null);
        kanbanMapper.updateById(kanban);

        // 9. 更新出库单明细实出数量
        for (OutboundOrderDetail detail : details) {
            detail.setActualQty(detail.getActualQty().add(scanQty));
            detailMapper.updateById(detail);
        }

        // 10. 扣减入库库存（FIFO），只扣减实际出库量
        deductInboundStock(kanban.getPartId(), scanQty);

        // 11. 重算出库单状态
        recalculateStatus(effectiveOrderId);

        // 12. 构建返回
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("kanbanNo", kanban.getKanbanNo());
        result.put("partCode", kanban.getPartCode());
        result.put("partName", kanban.getPartName());
        result.put("quantity", scanQty);
        result.put("warehouseAreaName", scan.getWarehouseAreaName());
        result.put("autoAdded", autoAdded);
        result.put("crossOrdered", crossOrdered);
        result.put("overflowSplit", overflowSplit);
        if (overflowSplit) {
            result.put("overflowQty", overflowQty);
        }

        // 出库单明细进度（兼容 Scanner 端）
        double plannedQty = 0.0;
        double actualQty = 0.0;
        if (!details.isEmpty()) {
            OutboundOrderDetail d = details.get(0);
            plannedQty = d.getPlannedQty() != null ? d.getPlannedQty().doubleValue() : 0.0;
            actualQty = d.getActualQty() != null ? d.getActualQty().doubleValue() : 0.0;
        }
        result.put("plannedQty", plannedQty);
        result.put("actualQty", actualQty);

        // 进度信息
        long remainingLocked = kanbanMapper.selectCount(
                new QueryWrapper<Kanban>()
                        .eq("outbound_order_id", effectiveOrderId)
                        .eq("status", Kanban.STATUS_LOCKED));
        result.put("remainingKanbans", remainingLocked);
        if (remainingLocked == 0) {
            result.put("allDone", true);
        }
        return result;
    }

    /**
     * 校验零件是否在出库单明细中
     */
    private void validatePartOnOrder(Long orderId, Long partId) {
        List<OutboundOrderDetail> details = detailMapper.selectList(
                new QueryWrapper<OutboundOrderDetail>()
                        .eq("outbound_order_id", orderId)
                        .eq("part_id", partId));
        if (details.isEmpty()) {
            Part part = partService.getById(partId);
            String partName = part != null ? part.getName() : "未知";
            throw new RuntimeException("零件 " + partName + " 不在当前出库单明细中，请先修改出库单添加此零件");
        }
    }

    /**
     * 从旧出库单释放一个看板（跨单转移时用）
     */
    private void releaseKanbanFromOrder(Kanban kanban) {
        Long oldOrderId = kanban.getOutboundOrderId();
        if (oldOrderId == null) return;
        List<OutboundOrderDetail> details = detailMapper.selectList(
                new QueryWrapper<OutboundOrderDetail>()
                        .eq("outbound_order_id", oldOrderId)
                        .eq("part_id", kanban.getPartId()));
        for (OutboundOrderDetail d : details) {
            BigDecimal newActual = d.getActualQty().subtract(kanban.getQuantity());
            if (newActual.compareTo(BigDecimal.ZERO) < 0) newActual = BigDecimal.ZERO;
            d.setActualQty(newActual);
            detailMapper.updateById(d);
        }
        recalculateStatus(oldOrderId);
    }

    private void deductInboundStock(Long partId, BigDecimal qty) {
        BigDecimal remaining = qty;
        // FIFO: 按创建时间升序取入库明细
        List<InboundOrderDetail> details = inboundDetailMapper.selectList(
                new QueryWrapper<InboundOrderDetail>()
                        .eq("part_id", partId)
                        .gt("actual_qty", 0)
                        .orderByAsc("create_time"));
        for (InboundOrderDetail d : details) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal available = d.getActualQty();
            BigDecimal deduct = available.min(remaining);
            d.setActualQty(available.subtract(deduct));
            inboundDetailMapper.updateById(d);
            remaining = remaining.subtract(deduct);
        }
    }

    // ========== 可用库存 ==========

    public BigDecimal getAvailableStock(Long partId) {
        // 统计status=1（在库可用）+ status=5（部分转出）的看板数量之和
        List<Kanban> kanbans = kanbanMapper.selectList(
                new QueryWrapper<Kanban>().eq("part_id", partId).in("status", Kanban.STATUS_AVAILABLE, Kanban.STATUS_PARTIAL_REPACK));
        if (!kanbans.isEmpty()) {
            return kanbans.stream()
                    .map(Kanban::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        // fallback: 从入库明细汇总
        List<InboundOrderDetail> details = inboundDetailMapper.selectList(
                new QueryWrapper<InboundOrderDetail>().eq("part_id", partId));
        return details.stream()
                .map(d -> d.getActualQty() != null ? d.getActualQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ========== 待出库清单 ==========

    public List<Kanban> getPendingKanbans(Long orderId) {
        return kanbanMapper.selectList(
                new QueryWrapper<Kanban>()
                        .eq("outbound_order_id", orderId)
                        .eq("status", Kanban.STATUS_LOCKED)
                        .orderByAsc("box_seq"));
    }

    // ========== 状态 ==========

    private void recalculateStatus(Long orderId) {
        // 按明细 actualQty 判断：已出库 / 部分出库 / 未出库
        List<OutboundOrderDetail> details = detailMapper.selectList(
                new QueryWrapper<OutboundOrderDetail>().eq("outbound_order_id", orderId));
        boolean hasOutbound = false;
        boolean allComplete = true;
        for (OutboundOrderDetail d : details) {
            BigDecimal actual = d.getActualQty() != null ? d.getActualQty() : BigDecimal.ZERO;
            BigDecimal planned = d.getPlannedQty() != null ? d.getPlannedQty() : BigDecimal.ZERO;
            if (actual.compareTo(BigDecimal.ZERO) > 0) hasOutbound = true;
            if (actual.compareTo(planned) < 0) allComplete = false;
        }
        int newStatus;
        if (!details.isEmpty() && allComplete && hasOutbound) {
            newStatus = OutboundStatus.COMPLETED.getCode();  // 已出库
        } else if (hasOutbound) {
            newStatus = OutboundStatus.PARTIAL_OUT.getCode(); // 部分出库
        } else {
            newStatus = OutboundStatus.PENDING_OUT.getCode(); // 未出库
        }
        OutboundOrder o = new OutboundOrder();
        o.setId(orderId);
        o.setStatus(newStatus);
        orderMapper.updateById(o);
    }

    public static String getStatusText(int status) {
        switch (status) {
            case 0: return "未出库";
            case 1: return "部分出库";
            case 2: return "已出库";
            case 3: return "作废";
            default: return "未知";
        }
    }

    /** 为溢出余量看板构建二维码JSON（与其他看板格式一致） */
    private String buildRemainderQr(Kanban k) {
        try {
            java.util.Map<String, Object> qr = new java.util.LinkedHashMap<>();
            qr.put("kanbanNo", k.getKanbanNo());
            qr.put("inboundOrderNo", k.getInboundOrderNo() != null ? k.getInboundOrderNo() : "");
            qr.put("partCode", k.getPartCode());
            qr.put("partName", k.getPartName());
            qr.put("quantity", k.getQuantity());
            qr.put("boxSeq", k.getBoxSeq());
            qr.put("supplierName", k.getSupplierName());
            qr.put("warehouseArea", k.getWarehouseAreaName());
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(qr);
        } catch (Exception e) {
            return "{\"kanbanNo\":\"" + k.getKanbanNo() + "\"}";
        }
    }
}
