package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.mapper.InboundOrderDetailMapper;
import com.warehouse.mapper.InboundOrderMapper;
import com.warehouse.model.dto.*;
import com.warehouse.model.entity.*;
import com.warehouse.model.enums.InboundStatus;
import com.warehouse.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InboundOrderService {

    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderDetailMapper detailMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final SupplierService supplierService;
    private final PartService partService;
    private final WarehouseAreaService warehouseAreaService;
    private final SupplierPartService supplierPartService;
    private final KanbanService kanbanService;

    public InboundOrderService(InboundOrderMapper inboundOrderMapper,
                               InboundOrderDetailMapper detailMapper,
                               OrderNoGenerator orderNoGenerator,
                               SupplierService supplierService,
                               PartService partService,
                               WarehouseAreaService warehouseAreaService,
                               SupplierPartService supplierPartService,
                               KanbanService kanbanService) {
        this.inboundOrderMapper = inboundOrderMapper;
        this.detailMapper = detailMapper;
        this.orderNoGenerator = orderNoGenerator;
        this.supplierService = supplierService;
        this.partService = partService;
        this.warehouseAreaService = warehouseAreaService;
        this.supplierPartService = supplierPartService;
        this.kanbanService = kanbanService;
    }

    // ==================== 列表查询 ====================

    public PageResult<InboundOrderVO> list(InboundOrderQuery query) {
        QueryWrapper<InboundOrder> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(query.getOrderNo())) {
            wrapper.like("order_no", query.getOrderNo());
        }
        if (query.getSupplierId() != null) {
            wrapper.eq("supplier_id", query.getSupplierId());
        }
        if (StringUtils.hasText(query.getOrderNumber())) {
            wrapper.like("order_number", query.getOrderNumber());
        }
        if (query.getStatus() != null) {
            wrapper.eq("status", query.getStatus());
        }
        wrapper.orderByDesc("create_time");

        Page<InboundOrder> page = new Page<>(query.getCurrent(), query.getSize());
        Page<InboundOrder> result = inboundOrderMapper.selectPage(page, wrapper);

        List<InboundOrderVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), result.getCurrent(), result.getSize());
    }

    // ==================== 详情 ====================

    public InboundOrder getOrderById(Long id) {
        return inboundOrderMapper.selectById(id);
    }

    public InboundOrderVO getDetail(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }
        InboundOrderVO vo = toVO(order);
        List<InboundOrderDetail> details = detailMapper.selectList(
                new QueryWrapper<InboundOrderDetail>()
                        .eq("inbound_order_id", id)
                        .orderByAsc("line_no"));
        vo.setDetails(details.stream().map(this::toDetailVO).collect(Collectors.toList()));
        return vo;
    }

    public InboundOrderVO getDetailByOrderNo(String orderNo) {
        InboundOrder order = inboundOrderMapper.selectOne(
                new QueryWrapper<InboundOrder>().eq("order_no", orderNo));
        if (order == null) {
            throw new RuntimeException("入库单不存在: " + orderNo);
        }
        return getDetail(order.getId());
    }

    // ==================== 保存（新增/修改） ====================

    @Transactional
    public InboundOrderVO save(InboundOrderSaveDTO dto) {
        if (dto.getId() != null) {
            return update(dto);
        }
        return create(dto);
    }

    private InboundOrderVO create(InboundOrderSaveDTO dto) {
        // 校验供应商
        Supplier supplier = supplierService.getById(dto.getSupplierId());
        if (supplier == null) {
            throw new RuntimeException("供应商不存在");
        }

        // 生成单号
        String orderNo = orderNoGenerator.generate();

        // 保存主表
        InboundOrder order = new InboundOrder();
        order.setOrderNo(orderNo);
        order.setSupplierId(dto.getSupplierId());
        order.setSupplierName(supplier.getName());
        order.setOrderNumber(dto.getOrderNumber());
        order.setRemark(dto.getRemark());
        order.setStatus(InboundStatus.PENDING.getCode());
        order.setCreateUserId(SecurityUtils.getCurrentUserId());
        inboundOrderMapper.insert(order);

        // 保存明细
        if (dto.getDetails() != null) {
            saveDetails(order.getId(), dto.getDetails(), dto.getSupplierId());
        }

        // 自动生成看板
        if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
            kanbanService.generateForOrder(order.getId(), dto.getDetails());
        }

        return getDetail(order.getId());
    }

    private InboundOrderVO update(InboundOrderSaveDTO dto) {
        InboundOrder order = inboundOrderMapper.selectById(dto.getId());
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }

        // 权限检查
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!InboundStatus.canEdit(order.getStatus(), isAdmin)) {
            throw new RuntimeException("当前状态不允许修改");
        }

        // 更新主表
        order.setSupplierId(dto.getSupplierId());
        Supplier supplier = supplierService.getById(dto.getSupplierId());
        order.setSupplierName(supplier != null ? supplier.getName() : null);
        order.setOrderNumber(dto.getOrderNumber());
        order.setRemark(dto.getRemark());
        inboundOrderMapper.updateById(order);

        // 保存旧明细的 actual_qty（已实际入库的数量不可丢失）
        Map<Long, BigDecimal> preservedQty = new HashMap<>();
        Map<Long, String> preservedBatch = new HashMap<>();
        List<InboundOrderDetail> oldDetails = detailMapper.selectList(
                new QueryWrapper<InboundOrderDetail>()
                        .eq("inbound_order_id", order.getId()));
        for (InboundOrderDetail od : oldDetails) {
            preservedQty.put(od.getPartId(), od.getActualQty());
            preservedBatch.put(od.getPartId(), od.getBatchNo());
        }

        // 删除旧明细，重新插入
        detailMapper.delete(new QueryWrapper<InboundOrderDetail>()
                .eq("inbound_order_id", order.getId()));

        if (dto.getDetails() != null) {
            saveDetails(order.getId(), dto.getDetails(), dto.getSupplierId(), preservedQty, preservedBatch);
        }

        // 重新生成看板
        if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
            kanbanService.generateForOrder(order.getId(), dto.getDetails());
        }

        return getDetail(order.getId());
    }

    // ==================== 提交 ====================

    @Transactional
    public InboundOrderVO submit(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }
        // 提交只是标记，不改变状态；状态仍为0(未入库)
        return getDetail(id);
    }

    // ==================== 删除 ====================

    @Transactional
    public void delete(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!InboundStatus.canEdit(order.getStatus(), isAdmin)) {
            throw new RuntimeException("当前状态不允许删除");
        }
        // 删除明细
        detailMapper.delete(new QueryWrapper<InboundOrderDetail>()
                .eq("inbound_order_id", id));
        // 删除主表
        inboundOrderMapper.deleteById(id);
    }

    // ==================== 作废 ====================

    @Transactional
    public void cancel(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("入库单不存在");
        }
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isAdmin) {
            throw new RuntimeException("只有管理员才能作废入库单");
        }
        if (order.getStatus() == null) {
            throw new RuntimeException("入库单状态异常，无法作废");
        }
        if (order.getStatus() == InboundStatus.CANCELLED.getCode()) {
            throw new RuntimeException("该入库单已经是作废状态");
        }
        order.setStatus(InboundStatus.CANCELLED.getCode());
        inboundOrderMapper.updateById(order);
    }

    // ==================== 批量操作 ====================

    @Transactional
    public void batchCopyParts(BatchOperationDTO dto) {
        if (dto.getTargetOrderId() == null || dto.getSourceOrderId() == null) {
            throw new RuntimeException("目标单号和源单号不能为空");
        }
        // 校验目标单的供应商
        InboundOrder targetOrder = inboundOrderMapper.selectById(dto.getTargetOrderId());
        if (targetOrder == null) throw new RuntimeException("目标入库单不存在");

        Long targetSupplierId = targetOrder.getSupplierId();
        List<InboundOrderDetail> sourceDetails = detailMapper.selectList(
                new QueryWrapper<InboundOrderDetail>()
                        .eq("inbound_order_id", dto.getSourceOrderId())
                        .orderByAsc("line_no"));

        // 获取目标单当前最大行号
        List<InboundOrderDetail> targetDetails = detailMapper.selectList(
                new QueryWrapper<InboundOrderDetail>()
                        .eq("inbound_order_id", dto.getTargetOrderId())
                        .orderByDesc("line_no")
                        .last("LIMIT 1"));
        int startLine = 1;
        if (!targetDetails.isEmpty() && targetDetails.get(0).getLineNo() != null) {
            startLine = targetDetails.get(0).getLineNo() + 1;
        }

        // 加载目标单现有明细，用于合并
        List<InboundOrderDetail> existingDetails = detailMapper.selectList(
                new QueryWrapper<InboundOrderDetail>()
                        .eq("inbound_order_id", dto.getTargetOrderId()));
        Map<Long, InboundOrderDetail> existingByPartId = existingDetails.stream()
                .collect(Collectors.toMap(InboundOrderDetail::getPartId, d -> d, (a, b) -> a));

        for (InboundOrderDetail src : sourceDetails) {
            // 只复制属于目标供应商的零件，跳过不相关的
            if (!supplierPartService.isPartBelongsToSupplier(targetSupplierId, src.getPartId())) {
                continue;
            }
            InboundOrderDetail existing = existingByPartId.get(src.getPartId());
            if (existing != null) {
                // 相同零件：合并数量
                existing.setPlannedQty(existing.getPlannedQty().add(src.getPlannedQty()));
                detailMapper.updateById(existing);
            } else {
                // 新零件：插入
                InboundOrderDetail detail = new InboundOrderDetail();
                detail.setInboundOrderId(dto.getTargetOrderId());
                detail.setPartId(src.getPartId());
                detail.setPartCode(src.getPartCode());
                detail.setPartName(src.getPartName());
                detail.setUnit(src.getUnit());
                detail.setPlannedQty(src.getPlannedQty());
                detail.setActualQty(BigDecimal.ZERO);
                detail.setWarehouseAreaId(src.getWarehouseAreaId());
                detail.setBatchNo(src.getBatchNo());
                detail.setLineNo(startLine++);
                detailMapper.insert(detail);
            }
        }
    }

    @Transactional
    public void batchSetArea(BatchOperationDTO dto) {
        if (dto.getDetailIds() == null || dto.getDetailIds().isEmpty()) {
            throw new RuntimeException("请选择要设置的明细行");
        }
        for (Long detailId : dto.getDetailIds()) {
            InboundOrderDetail detail = detailMapper.selectById(detailId);
            if (detail != null) {
                detail.setWarehouseAreaId(dto.getWarehouseAreaId());
                detailMapper.updateById(detail);
            }
        }
    }

    // ==================== 状态更新 ====================

    public void updateStatus(Long orderId, Integer newStatus) {
        InboundOrder order = inboundOrderMapper.selectById(orderId);
        if (order != null) {
            order.setStatus(newStatus);
            inboundOrderMapper.updateById(order);
        }
    }

    /**
     * 根据明细的实入数量重新计算入库单状态
     */
    public void recalculateStatus(Long orderId) {
        List<InboundOrderDetail> details = detailMapper.selectList(
                new QueryWrapper<InboundOrderDetail>()
                        .eq("inbound_order_id", orderId));

        if (details.isEmpty()) {
            updateStatus(orderId, InboundStatus.PENDING.getCode());
            return;
        }

        BigDecimal zero = BigDecimal.ZERO;
        boolean allCompleted = details.stream().allMatch(
                d -> d.getActualQty() != null && d.getActualQty().compareTo(d.getPlannedQty()) >= 0);
        boolean anyPartial = details.stream().anyMatch(
                d -> d.getActualQty() != null && d.getActualQty().compareTo(zero) > 0);

        if (allCompleted) {
            updateStatus(orderId, InboundStatus.COMPLETED.getCode());
        } else if (anyPartial) {
            updateStatus(orderId, InboundStatus.PARTIAL.getCode());
        } else {
            updateStatus(orderId, InboundStatus.PENDING.getCode());
        }
    }

    // ==================== 内部方法 ====================

    private void saveDetails(Long orderId, List<InboundDetailDTO> detailDTOs, Long supplierId) {
        saveDetails(orderId, detailDTOs, supplierId, null, null);
    }

    private void saveDetails(Long orderId, List<InboundDetailDTO> detailDTOs, Long supplierId,
                             Map<Long, BigDecimal> preservedQty, Map<Long, String> preservedBatch) {
        // 检查零件是否重复
        java.util.Set<Long> seenPartIds = new java.util.HashSet<>();
        for (InboundDetailDTO dto : detailDTOs) {
            if (!seenPartIds.add(dto.getPartId())) {
                throw new RuntimeException("零件重复: partId=" + dto.getPartId() + "，同一入库单中不能出现相同零件");
            }
        }

        int lineNo = 1;
        for (InboundDetailDTO dto : detailDTOs) {
            Part part = partService.getById(dto.getPartId());
            if (part == null) {
                throw new RuntimeException("物料不存在: " + dto.getPartId());
            }

            // 校验零件属于供应商
            if (!supplierPartService.isPartBelongsToSupplier(supplierId, dto.getPartId())) {
                throw new RuntimeException("物料 " + part.getCode() + " 不属于所选供应商");
            }

            InboundOrderDetail detail = new InboundOrderDetail();
            detail.setInboundOrderId(orderId);
            detail.setPartId(part.getId());
            detail.setPartCode(part.getCode());
            detail.setPartName(part.getName());
            detail.setUnit(dto.getUnit() != null ? dto.getUnit() : part.getUnit());

            // 计算 plannedQty = 包装容量 × 箱数
            int capacity = part.getPackageCapacity() != null ? part.getPackageCapacity() : 1;
            int boxCount = dto.getBoxCount() != null ? dto.getBoxCount() : 0;
            BigDecimal plannedQty = BigDecimal.valueOf(capacity).multiply(BigDecimal.valueOf(boxCount));
            detail.setPlannedQty(plannedQty);
            detail.setBoxCount(boxCount);

            // 保留旧的实际入库数量和批次号
            if (preservedQty != null && preservedQty.containsKey(dto.getPartId())) {
                detail.setActualQty(preservedQty.get(dto.getPartId()));
            } else if (dto.getActualQty() != null && dto.getActualQty().compareTo(BigDecimal.ZERO) > 0) {
                detail.setActualQty(dto.getActualQty());
            } else {
                detail.setActualQty(BigDecimal.ZERO);
            }
            if (preservedBatch != null && preservedBatch.containsKey(dto.getPartId())) {
                detail.setBatchNo(preservedBatch.get(dto.getPartId()));
            } else {
                detail.setBatchNo(dto.getBatchNo());
            }
            // 默认库区：优先用 DTO 传入的，没有则用零件的默认库区
            if (dto.getWarehouseAreaId() != null) {
                detail.setWarehouseAreaId(dto.getWarehouseAreaId());
            } else if (part.getWarehouseAreaId() != null) {
                detail.setWarehouseAreaId(part.getWarehouseAreaId());
            } else {
                detail.setWarehouseAreaId(null);
            }
            detail.setLineNo(lineNo++);
            detailMapper.insert(detail);
        }
    }

    private InboundOrderVO toVO(InboundOrder order) {
        InboundOrderVO vo = new InboundOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setSupplierId(order.getSupplierId());
        vo.setSupplierName(order.getSupplierName());
        vo.setOrderNumber(order.getOrderNumber());
        vo.setStatus(order.getStatus());
        vo.setStatusText(InboundStatus.getLabelByCode(order.getStatus()));
        vo.setRemark(order.getRemark());
        if (order.getCreateTime() != null) {
            vo.setCreateTime(order.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (order.getUpdateTime() != null) {
            vo.setUpdateTime(order.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return vo;
    }

    private InboundDetailVO toDetailVO(InboundOrderDetail detail) {
        InboundDetailVO vo = new InboundDetailVO();
        vo.setId(detail.getId());
        vo.setPartId(detail.getPartId());
        vo.setPartCode(detail.getPartCode());
        vo.setPartName(detail.getPartName());
        vo.setUnit(detail.getUnit());
        vo.setPlannedQty(detail.getPlannedQty());
        vo.setActualQty(detail.getActualQty());
        vo.setWarehouseAreaId(detail.getWarehouseAreaId());
        if (detail.getWarehouseAreaId() != null) {
            WarehouseArea area = warehouseAreaService.getById(detail.getWarehouseAreaId());
            vo.setWarehouseAreaName(area != null ? area.getName() : null);
        }
        vo.setBatchNo(detail.getBatchNo());
        vo.setBoxCount(detail.getBoxCount());
        // 填充包装容量（从 Part 获取）
        if (detail.getPartId() != null) {
            Part part = partService.getById(detail.getPartId());
            if (part != null) {
                vo.setPackageCapacity(part.getPackageCapacity() != null ? part.getPackageCapacity() : 1);
            }
        }
        vo.setLineNo(detail.getLineNo());
        return vo;
    }
}
