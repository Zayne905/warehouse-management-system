package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.InboundOrderDetailMapper;
import com.warehouse.mapper.InboundOrderMapper;
import com.warehouse.mapper.KanbanMapper;
import com.warehouse.mapper.ScanRecordMapper;
import com.warehouse.model.dto.KanbanScanDTO;
import com.warehouse.model.dto.ScanDuplicateCheckDTO;
import com.warehouse.model.dto.ScanSubmitDTO;
import com.warehouse.model.entity.*;
import com.warehouse.model.enums.InboundStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ScanService {

    private final ScanRecordMapper scanRecordMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderDetailMapper detailMapper;
    private final InboundOrderService inboundOrderService;
    private final KanbanMapper kanbanMapper;

    public ScanService(ScanRecordMapper scanRecordMapper,
                       InboundOrderMapper inboundOrderMapper,
                       InboundOrderDetailMapper detailMapper,
                       InboundOrderService inboundOrderService,
                       KanbanMapper kanbanMapper) {
        this.scanRecordMapper = scanRecordMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.detailMapper = detailMapper;
        this.inboundOrderService = inboundOrderService;
        this.kanbanMapper = kanbanMapper;
    }

    // ==================== 检查重复 ====================

    public Map<String, Object> checkDuplicate(ScanDuplicateCheckDTO dto) {
        // 查询该入库单+零件的明细
        InboundOrderDetail detail = findDetail(dto.getInboundOrderId(), dto.getPartCode());
        if (detail == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("isDuplicate", true);
            result.put("message", "该物料不在入库单明细中");
            return result;
        }

        // 使用明细中的 actual_qty 作为已入库量（包含历史直接录入 + 扫码记录）
        BigDecimal actualQty = detail.getActualQty() != null ? detail.getActualQty() : BigDecimal.ZERO;
        BigDecimal remaining = detail.getPlannedQty().subtract(actualQty);

        Map<String, Object> result = new HashMap<>();
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            result.put("isDuplicate", true);
            result.put("message", "该物料已全部入库");
        } else {
            result.put("isDuplicate", false);
            result.put("message", "可以扫描，剩余: " + remaining);
        }
        result.put("remainingQty", remaining);
        result.put("plannedQty", detail.getPlannedQty());
        result.put("scannedTotal", actualQty);
        return result;
    }

    // ==================== 提交扫描 ====================

    @Transactional
    public Map<String, Object> submitScan(ScanSubmitDTO dto) {
        InboundOrder order = inboundOrderMapper.selectById(dto.getInboundOrderId());
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (order.getStatus() == InboundStatus.CANCELLED.getCode()) {
            throw new RuntimeException("已作废的入库单不可扫描");
        }

        // 查找明细
        InboundOrderDetail detail = findDetail(dto.getInboundOrderId(), dto.getPartCode());
        if (detail == null) {
            throw new RuntimeException("该物料不在入库单明细中");
        }

        // 检查剩余数量
        BigDecimal scannedTotal = getScannedTotal(dto.getInboundOrderId(),
                detail.getPartId(), dto.getBatchNo());
        BigDecimal remaining = detail.getPlannedQty().subtract(scannedTotal);
        BigDecimal scanQty = dto.getScanQty() != null ? dto.getScanQty() : BigDecimal.ONE;

        if (scanQty.compareTo(remaining) > 0) {
            throw new RuntimeException("扫描数量超过剩余数量: " + remaining);
        }

        // 插入扫描记录
        ScanRecord record = new ScanRecord();
        record.setInboundOrderId(dto.getInboundOrderId());
        record.setInboundOrderNo(order.getOrderNo());
        record.setPartId(detail.getPartId());
        record.setPartCode(detail.getPartCode());
        record.setPartName(detail.getPartName());
        record.setBatchNo(dto.getBatchNo());
        record.setScanQty(scanQty);
        record.setOperatorId(dto.getOperatorId());
        scanRecordMapper.insert(record);

        // 更新明细实入数量
        detail.setActualQty(detail.getActualQty().add(scanQty));
        detailMapper.updateById(detail);

        // 重新计算入库单状态
        inboundOrderService.recalculateStatus(dto.getInboundOrderId());

        // 返回回显信息
        InboundOrder updated = inboundOrderMapper.selectById(dto.getInboundOrderId());
        BigDecimal newScannedTotal = scannedTotal.add(scanQty);
        BigDecimal newRemaining = detail.getPlannedQty().subtract(newScannedTotal);

        Map<String, Object> result = new HashMap<>();
        result.put("scannedTotal", newScannedTotal);
        result.put("plannedQty", detail.getPlannedQty());
        result.put("remainingQty", newRemaining.compareTo(BigDecimal.ZERO) > 0 ? newRemaining : BigDecimal.ZERO);
        result.put("orderStatus", updated.getStatus());
        result.put("orderStatusText", InboundStatus.getLabelByCode(updated.getStatus()));
        return result;
    }

    // ==================== 扫描列表 ====================

    public List<ScanRecord> listScans(Long inboundOrderId) {
        return scanRecordMapper.selectList(
                new QueryWrapper<ScanRecord>()
                        .eq("inbound_order_id", inboundOrderId)
                        .orderByDesc("scan_time"));
    }

    // ==================== 删除扫描记录 ====================

    @Transactional
    public void deleteScan(Long scanRecordId) {
        ScanRecord record = scanRecordMapper.selectById(scanRecordId);
        if (record == null) {
            throw new RuntimeException("扫描记录不存在");
        }

        // 反向更新实入数量
        InboundOrderDetail detail = findDetail(record.getInboundOrderId(), record.getPartCode());
        if (detail != null) {
            detail.setActualQty(detail.getActualQty().subtract(record.getScanQty()));
            if (detail.getActualQty().compareTo(BigDecimal.ZERO) < 0) {
                detail.setActualQty(BigDecimal.ZERO);
            }
            detailMapper.updateById(detail);
        }

        scanRecordMapper.deleteById(scanRecordId);

        // 重新计算状态
        inboundOrderService.recalculateStatus(record.getInboundOrderId());
    }

    // ==================== 回显信息 ====================

    public Map<String, Object> getFeedback(String orderNo, String partCode) {
        InboundOrder order = inboundOrderMapper.selectOne(
                new QueryWrapper<InboundOrder>().eq("order_no", orderNo));
        if (order == null) {
            throw new RuntimeException("入库单不存在: " + orderNo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("supplierName", order.getSupplierName());
        result.put("orderNumber", order.getOrderNumber());
        result.put("status", order.getStatus());
        result.put("statusText", InboundStatus.getLabelByCode(order.getStatus()));

        if (partCode != null) {
            InboundOrderDetail detail = findDetail(order.getId(), partCode);
            if (detail != null) {
                BigDecimal scannedTotal = getScannedTotal(order.getId(), detail.getPartId(), null);
                Map<String, Object> partFeedback = new HashMap<>();
                partFeedback.put("partCode", detail.getPartCode());
                partFeedback.put("partName", detail.getPartName());
                partFeedback.put("plannedQty", detail.getPlannedQty());
                partFeedback.put("scannedTotal", scannedTotal);
                partFeedback.put("unit", detail.getUnit());
                result.put("partFeedback", partFeedback);
            }
        }

        return result;
    }

    // ==================== 内部方法 ====================

    private InboundOrderDetail findDetail(Long orderId, String partCode) {
        return detailMapper.selectOne(
                new QueryWrapper<InboundOrderDetail>()
                        .eq("inbound_order_id", orderId)
                        .eq("part_code", partCode));
    }

    private BigDecimal getScannedTotal(Long orderId, Long partId, String batchNo) {
        QueryWrapper<ScanRecord> wrapper = new QueryWrapper<ScanRecord>()
                .eq("inbound_order_id", orderId)
                .eq("part_id", partId);
        if (batchNo != null) {
            wrapper.eq("batch_no", batchNo);
        }
        List<ScanRecord> records = scanRecordMapper.selectList(wrapper);
        return records.stream()
                .map(r -> r.getScanQty() != null ? r.getScanQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ==================== 看板扫码入库 ====================

    /**
     * 扫看板码自动收货一箱
     */
    @Transactional
    public Map<String, Object> scanKanban(KanbanScanDTO dto) {
        Map<String, Object> result = new HashMap<>();

        // 1. 校验看板号是否已扫过
        Long existCount = scanRecordMapper.selectCount(
                new QueryWrapper<ScanRecord>().eq("kanban_no", dto.getKanbanNo()));
        if (existCount > 0) {
            throw new RuntimeException("该箱已入库，看板号: " + dto.getKanbanNo());
        }

        // 2. 查找入库单
        InboundOrder order = inboundOrderMapper.selectOne(
                new QueryWrapper<InboundOrder>().eq("order_no", dto.getInboundOrderNo()));
        if (order == null) {
            throw new RuntimeException("入库单不存在: " + dto.getInboundOrderNo());
        }
        if (order.getStatus().equals(InboundStatus.CANCELLED.getCode())) {
            throw new RuntimeException("入库单已作废，无法入库");
        }

        // 3. 查找明细行
        InboundOrderDetail detail = findDetail(order.getId(), dto.getPartCode());
        if (detail == null) {
            throw new RuntimeException("该物料不在入库单明细中");
        }

        // 4. 创建扫描记录
        ScanRecord record = new ScanRecord();
        record.setInboundOrderId(order.getId());
        record.setInboundOrderNo(order.getOrderNo());
        record.setPartId(detail.getPartId());
        record.setPartCode(dto.getPartCode());
        record.setPartName(dto.getPartName());
        record.setKanbanNo(dto.getKanbanNo());
        int scanQty = dto.getQuantity() != null ? dto.getQuantity() : 0;
        if (scanQty <= 0) {
            throw new RuntimeException("扫描数量必须大于0");
        }
        record.setScanQty(BigDecimal.valueOf(scanQty));
        record.setScanTime(java.time.LocalDateTime.now());
        record.setOperatorId(dto.getOperatorId());
        scanRecordMapper.insert(record);

        // 5. 更新明细实入数量（防御性null处理）
        BigDecimal currentQty = detail.getActualQty() != null ? detail.getActualQty() : BigDecimal.ZERO;
        detail.setActualQty(currentQty.add(record.getScanQty()));
        detailMapper.updateById(detail);

        // 6. 更新看板状态
        Kanban kanban = kanbanMapper.selectOne(
                new QueryWrapper<Kanban>().eq("kanban_no", dto.getKanbanNo()));
        if (kanban != null) {
            kanban.setStatus(1); // 已入库
            kanbanMapper.updateById(kanban);
        }

        // 7. 重新计算订单状态
        inboundOrderService.recalculateStatus(order.getId());

        // 8. 返回该零件的收货进度
        int boxTotal = 0;
        if (kanban != null) {
            boxTotal = kanbanMapper.selectCount(
                    new QueryWrapper<Kanban>()
                            .eq("inbound_order_id", order.getId())
                            .eq("part_id", detail.getPartId())).intValue();
        }
        // fallback: 数据库无看板记录时从明细的boxCount估算总箱数
        if (boxTotal == 0 && detail.getBoxCount() != null) {
            boxTotal = detail.getBoxCount();
        }
        int boxScanned = (int) scanRecordMapper.selectCount(
                new QueryWrapper<ScanRecord>()
                        .eq("inbound_order_id", order.getId())
                        .eq("part_id", detail.getPartId())
                        .isNotNull("kanban_no")).longValue();

        result.put("success", true);
        result.put("kanbanNo", dto.getKanbanNo());
        result.put("partCode", dto.getPartCode());
        result.put("partName", dto.getPartName());
        result.put("quantity", dto.getQuantity());
        result.put("boxSeq", dto.getBoxSeq());
        result.put("boxScanned", boxScanned);
        result.put("boxTotal", boxTotal);
        result.put("orderStatus", order.getStatus());

        return result;
    }
}
