package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.InboundOrderMapper;
import com.warehouse.mapper.KanbanMapper;
import com.warehouse.model.dto.InboundDetailDTO;
import com.warehouse.model.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class KanbanService {

    private final KanbanMapper kanbanMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final PartService partService;
    private final WarehouseAreaService warehouseAreaService;

    public KanbanService(KanbanMapper kanbanMapper, InboundOrderMapper inboundOrderMapper,
                         PartService partService, WarehouseAreaService warehouseAreaService) {
        this.kanbanMapper = kanbanMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.partService = partService;
        this.warehouseAreaService = warehouseAreaService;
    }

    /**
     * 生成看板号: R-yyyy-MM-dd-订单流水-P零件号C-箱序号
     * 示例: R-2026-06-11-1001-P001C-0
     */
    private String generateKanbanNo(InboundOrder order, Part part, int boxSeq) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return String.format("R-%s-%s-%sC-%d", dateStr, order.getOrderNo(), part.getCode(), boxSeq);
    }

    /**
     * 为入库单批量生成看板
     */
    @Transactional
    public List<Kanban> generateForOrder(Long orderId, List<InboundDetailDTO> details) {
        InboundOrder order = inboundOrderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("入库单不存在");

        // 删除旧看板
        kanbanMapper.delete(new QueryWrapper<Kanban>().eq("inbound_order_id", orderId));

        List<Kanban> kanbans = new ArrayList<>();
        for (InboundDetailDTO dto : details) {
            Part part = partService.getById(dto.getPartId());
            int boxCount = dto.getBoxCount() != null ? dto.getBoxCount() : 0;
            for (int seq = 0; seq < boxCount; seq++) {
                Kanban k = new Kanban();
                k.setKanbanNo(generateKanbanNo(order, part, seq));
                k.setInboundOrderId(orderId);
                k.setInboundOrderNo(order.getOrderNo());
                k.setPartId(part.getId());
                k.setPartCode(part.getCode());
                k.setPartName(part.getName());
                k.setSupplierName(order.getSupplierName());
                k.setQuantity(part.getPackageCapacity() != null ? part.getPackageCapacity() : 1);
                k.setBoxSeq(seq);
                // 库区
                Long areaId = dto.getWarehouseAreaId() != null
                        ? dto.getWarehouseAreaId() : part.getWarehouseAreaId();
                k.setWarehouseAreaId(areaId);
                if (areaId != null) {
                    WarehouseArea area = warehouseAreaService.getById(areaId);
                    k.setWarehouseAreaName(area != null ? area.getName() : null);
                }
                k.setStatus(0); // 待入库
                kanbanMapper.insert(k);
                kanbans.add(k);
            }
        }
        return kanbans;
    }

    public List<Kanban> listByOrderId(Long orderId) {
        return kanbanMapper.selectList(
                new QueryWrapper<Kanban>().eq("inbound_order_id", orderId).orderByAsc("box_seq"));
    }

    public List<Kanban> listByPartId(Long partId) {
        return kanbanMapper.selectList(
                new QueryWrapper<Kanban>().eq("part_id", partId).orderByAsc("create_time"));
    }
}
