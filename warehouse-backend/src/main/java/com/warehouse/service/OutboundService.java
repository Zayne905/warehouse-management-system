package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.mapper.*;
import com.warehouse.model.dto.PageResult;
import com.warehouse.model.entity.*;
import com.warehouse.model.enums.InboundStatus;
import com.warehouse.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
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
    private final KanbanMapper kanbanMapper;

    public OutboundService(OutboundOrderMapper orderMapper,
                           OutboundOrderDetailMapper detailMapper,
                           OutboundScanMapper scanMapper,
                           OrderNoGenerator orderNoGenerator,
                           PartService partService,
                           InboundOrderDetailMapper inboundDetailMapper,
                           KanbanMapper kanbanMapper) {
        this.orderMapper = orderMapper;
        this.detailMapper = detailMapper;
        this.scanMapper = scanMapper;
        this.orderNoGenerator = orderNoGenerator;
        this.partService = partService;
        this.inboundDetailMapper = inboundDetailMapper;
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

    public PageResult<Map<String, Object>> list(int current, int size, String orderNo, Integer status) {
        QueryWrapper<OutboundOrder> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(orderNo)) wrapper.like("order_no", orderNo);
        if (status != null) wrapper.eq("status", status);
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
            m.put("createTime", o.getCreateTime() != null
                    ? o.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            // 统计零件种类和总数量
            List<OutboundOrderDetail> details = detailMapper.selectList(
                    new QueryWrapper<OutboundOrderDetail>().eq("outbound_order_id", o.getId()));
            int partCount = details.size();
            BigDecimal totalQty = details.stream()
                    .map(d -> d.getPlannedQty() != null ? d.getPlannedQty() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            m.put("partCount", partCount);
            m.put("totalQty", totalQty);
            return m;
        }).collect(Collectors.toList());

        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
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
        m.put("createTime", order.getCreateTime() != null
                ? order.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");

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
            // 可用库存
            dm.put("availableStock", getAvailableStock(d.getPartId()));
            return dm;
        }).collect(Collectors.toList());
        m.put("details", detailList);

        // 扫描记录
        List<OutboundScan> scans = scanMapper.selectList(
                new QueryWrapper<OutboundScan>().eq("outbound_order_id", id).orderByDesc("scan_time"));
        m.put("scans", scans);

        return m;
    }

    // ========== 保存出库单 ==========

    @Transactional
    public Map<String, Object> save(Map<String, Object> dto) {
        Long id = dto.get("id") != null ? Long.valueOf(dto.get("id").toString()) : null;
        if (id != null) return update(dto);

        // 新建
        OutboundOrder order = new OutboundOrder();
        order.setOrderNo(generateOrderNo());
        order.setStatus(0);
        order.setRemark((String) dto.get("remark"));
        order.setCreateUserId(SecurityUtils.getCurrentUserId());
        orderMapper.insert(order);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> details = (List<Map<String, Object>>) dto.get("details");
        if (details != null) saveDetails(order.getId(), details);

        return getDetail(order.getId());
    }

    private Map<String, Object> update(Map<String, Object> dto) {
        Long id = Long.valueOf(dto.get("id").toString());
        OutboundOrder order = orderMapper.selectById(id);
        if (order == null) throw new RuntimeException("出库单不存在");
        if (order.getStatus() == 2 || order.getStatus() == 3) throw new RuntimeException("当前状态不允许修改");

        order.setRemark((String) dto.get("remark"));
        orderMapper.updateById(order);

        // 删除旧明细
        detailMapper.delete(new QueryWrapper<OutboundOrderDetail>().eq("outbound_order_id", id));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> details = (List<Map<String, Object>>) dto.get("details");
        if (details != null) saveDetails(id, details);

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

    // ========== 删除 ==========

    @Transactional
    public void delete(Long id) {
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
        if (order.getStatus() == 3) throw new RuntimeException("已作废");
        order.setStatus(3);
        orderMapper.updateById(order);
    }

    // ========== 扫码出库（FIFO） ==========

    @Transactional
    public Map<String, Object> scanOutbound(Long orderId, String kanbanNo, Integer operatorId) {
        // 1. 查找看板
        Kanban kanban = kanbanMapper.selectOne(
                new QueryWrapper<Kanban>().eq("kanban_no", kanbanNo));
        if (kanban == null) throw new RuntimeException("看板不存在: " + kanbanNo);
        if (kanban.getStatus() != 1) throw new RuntimeException("该看板未入库或已出库");

        // 2. FIFO: 检查是否有更早的同零件看板未出库
        if (kanbanNo == null || kanbanNo.isEmpty()) {
            // 未指定看板号时自动选最早入库的
            List<Kanban> oldest = kanbanMapper.selectList(
                    new QueryWrapper<Kanban>()
                            .eq("part_id", kanban.getPartId())
                            .eq("status", 1)
                            .orderByAsc("create_time")
                            .last("LIMIT 1"));
            if (oldest.isEmpty()) throw new RuntimeException("该零件无可用库存");
            kanban = oldest.get(0);
        }

        // 3. 查找出库单明细
        OutboundOrderDetail detail = null;
        if (orderId != null) {
            detail = detailMapper.selectOne(
                    new QueryWrapper<OutboundOrderDetail>()
                            .eq("outbound_order_id", orderId)
                            .eq("part_id", kanban.getPartId()));
            if (detail == null) throw new RuntimeException("该零件不在出库单明细中");

            // 检查是否超量
            BigDecimal remaining = detail.getPlannedQty().subtract(detail.getActualQty());
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) throw new RuntimeException("该零件已完成出库");
        }

        // 4. 创建出库扫描记录
        OutboundScan scan = new OutboundScan();
        scan.setOutboundOrderId(orderId);
        scan.setKanbanNo(kanban.getKanbanNo());
        scan.setPartId(kanban.getPartId());
        scan.setPartCode(kanban.getPartCode());
        scan.setPartName(kanban.getPartName());
        scan.setQuantity(BigDecimal.valueOf(kanban.getQuantity()));
        scan.setScanTime(LocalDateTime.now());
        scan.setOperatorId(operatorId != null ? Long.valueOf(operatorId) : null);
        if (orderId != null) {
            OutboundOrder order = orderMapper.selectById(orderId);
            if (order != null) scan.setOutboundOrderNo(order.getOrderNo());
        }
        scanMapper.insert(scan);

        // 5. 更新看板状态为已出库
        kanban.setStatus(2);
        kanbanMapper.updateById(kanban);

        // 6. 更新出库单明细
        if (detail != null) {
            detail.setActualQty(detail.getActualQty().add(scan.getQuantity()));
            detailMapper.updateById(detail);
        }

        // 7. 扣减入库库存（FIFO：从最早的入库明细开始扣）
        deductInboundStock(kanban.getPartId(), scan.getQuantity());

        // 8. 更新出库单状态
        if (orderId != null) recalculateStatus(orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("kanbanNo", kanban.getKanbanNo());
        result.put("partCode", kanban.getPartCode());
        result.put("partName", kanban.getPartName());
        result.put("quantity", kanban.getQuantity());
        if (detail != null) {
            result.put("plannedQty", detail.getPlannedQty());
            result.put("actualQty", detail.getActualQty());
        }
        return result;
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
        List<Kanban> kanbans = kanbanMapper.selectList(
                new QueryWrapper<Kanban>().eq("part_id", partId).eq("status", 1));
        if (!kanbans.isEmpty()) {
            return kanbans.stream()
                    .map(k -> BigDecimal.valueOf(k.getQuantity()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        // fallback: 从入库明细汇总
        List<InboundOrderDetail> details = inboundDetailMapper.selectList(
                new QueryWrapper<InboundOrderDetail>().eq("part_id", partId));
        return details.stream()
                .map(d -> d.getActualQty() != null ? d.getActualQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ========== 状态 ==========

    private void recalculateStatus(Long orderId) {
        List<OutboundOrderDetail> details = detailMapper.selectList(
                new QueryWrapper<OutboundOrderDetail>().eq("outbound_order_id", orderId));
        if (details.isEmpty()) {
            orderMapper.updateById(setStatus(orderId, 0));
            return;
        }
        boolean allDone = details.stream().allMatch(
                d -> d.getActualQty().compareTo(d.getPlannedQty()) >= 0);
        boolean anyDone = details.stream().anyMatch(
                d -> d.getActualQty().compareTo(BigDecimal.ZERO) > 0);
        int status = allDone ? 2 : (anyDone ? 1 : 0);
        orderMapper.updateById(setStatus(orderId, status));
    }

    private OutboundOrder setStatus(Long id, int status) {
        OutboundOrder o = new OutboundOrder();
        o.setId(id);
        o.setStatus(status);
        return o;
    }

    public static String getStatusText(int status) {
        switch (status) {
            case 0: return "待出库";
            case 1: return "部分出库";
            case 2: return "已完成";
            case 3: return "作废";
            default: return "未知";
        }
    }
}
