package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.InboundOrderMapper;
import com.warehouse.mapper.KanbanMapper;
import com.warehouse.mapper.OutboundScanMapper;
import com.warehouse.model.dto.InboundDetailDTO;
import com.warehouse.model.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KanbanService {

    private final KanbanMapper kanbanMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundScanMapper outboundScanMapper;
    private final PartService partService;
    private final WarehouseAreaService warehouseAreaService;

    public KanbanService(KanbanMapper kanbanMapper, InboundOrderMapper inboundOrderMapper,
                         OutboundScanMapper outboundScanMapper,
                         PartService partService, WarehouseAreaService warehouseAreaService) {
        this.kanbanMapper = kanbanMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.outboundScanMapper = outboundScanMapper;
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
                k.setStatus(Kanban.STATUS_AVAILABLE); // 在库可用
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
        List<Kanban> list = kanbanMapper.selectList(
                new QueryWrapper<Kanban>().eq("part_id", partId).orderByAsc("create_time"));

        // 为已出库看板填充扫码时间
        List<String> outboundNos = new ArrayList<>();
        for (Kanban k : list) {
            if (k.getStatus() == Kanban.STATUS_OUTBOUND && k.getKanbanNo() != null) {
                outboundNos.add(k.getKanbanNo());
            }
        }
        if (!outboundNos.isEmpty()) {
            List<OutboundScan> scans = outboundScanMapper.selectList(
                    new QueryWrapper<OutboundScan>().in("kanban_no", outboundNos).orderByDesc("scan_time"));
            Map<String, java.time.LocalDateTime> scanTimeMap = new HashMap<>();
            for (OutboundScan s : scans) {
                scanTimeMap.putIfAbsent(s.getKanbanNo(), s.getScanTime());
            }
            for (Kanban k : list) {
                if (k.getStatus() == Kanban.STATUS_OUTBOUND) {
                    k.setOutboundScanTime(scanTimeMap.get(k.getKanbanNo()));
                }
            }
        }
        return list;
    }

    // ========== 封存 / 解封 ==========

    /** 扫码翻转子：在库可用→封存，封存→解封，其他状态报错 */
    @Transactional
    public Map<String, Object> toggleBlock(String kanbanNo) {
        Kanban kanban = kanbanMapper.selectOne(
                new QueryWrapper<Kanban>().eq("kanban_no", kanbanNo));
        if (kanban == null) throw new RuntimeException("看板不存在: " + kanbanNo);

        int oldStatus = kanban.getStatus();
        Map<String, Object> result = new HashMap<>();
        result.put("kanbanNo", kanban.getKanbanNo());
        result.put("partName", kanban.getPartName());
        result.put("partCode", kanban.getPartCode());
        result.put("previousStatus", oldStatus);
        result.put("previousStatusText", kanban.getStatusText());

        if (kanban.getStatus() == Kanban.STATUS_AVAILABLE) {
            kanban.setStatus(Kanban.STATUS_BLOCKED);
            kanbanMapper.updateById(kanban);
            result.put("action", "封存");
            result.put("newStatus", Kanban.STATUS_BLOCKED);
        } else if (kanban.getStatus() == Kanban.STATUS_BLOCKED) {
            kanban.setStatus(Kanban.STATUS_AVAILABLE);
            kanbanMapper.updateById(kanban);
            result.put("action", "解封");
            result.put("newStatus", Kanban.STATUS_AVAILABLE);
        } else {
            throw new RuntimeException("当前状态[" + kanban.getStatusText() + "]不允许封存或解封");
        }
        return result;
    }

    @Transactional
    public void blockKanban(String kanbanNo) {
        Kanban kanban = kanbanMapper.selectOne(
                new QueryWrapper<Kanban>().eq("kanban_no", kanbanNo));
        if (kanban == null) throw new RuntimeException("看板不存在: " + kanbanNo);
        if (kanban.getStatus() != Kanban.STATUS_AVAILABLE) {
            throw new RuntimeException("只有在库可用状态的条码才能封存，当前状态：" + kanban.getStatusText());
        }
        kanban.setStatus(Kanban.STATUS_BLOCKED);
        kanbanMapper.updateById(kanban);
    }

    @Transactional
    public void unblockKanban(String kanbanNo) {
        Kanban kanban = kanbanMapper.selectOne(
                new QueryWrapper<Kanban>().eq("kanban_no", kanbanNo));
        if (kanban == null) throw new RuntimeException("看板不存在: " + kanbanNo);
        if (kanban.getStatus() != Kanban.STATUS_BLOCKED) {
            throw new RuntimeException("只有封存状态的条码才能解封，当前状态：" + kanban.getStatusText());
        }
        kanban.setStatus(Kanban.STATUS_AVAILABLE);
        kanbanMapper.updateById(kanban);
    }

    @Transactional
    public int blockByPartId(Long partId) {
        List<Kanban> kanbans = kanbanMapper.selectList(
                new QueryWrapper<Kanban>()
                        .eq("part_id", partId)
                        .eq("status", Kanban.STATUS_AVAILABLE));
        for (Kanban k : kanbans) {
            k.setStatus(Kanban.STATUS_BLOCKED);
            kanbanMapper.updateById(k);
        }
        return kanbans.size();
    }

    /** 批量解封 */
    @Transactional
    public int batchUnblock(List<String> kanbanNos) {
        int count = 0;
        for (String no : kanbanNos) {
            Kanban kanban = kanbanMapper.selectOne(
                    new QueryWrapper<Kanban>().eq("kanban_no", no));
            if (kanban != null && kanban.getStatus() == Kanban.STATUS_BLOCKED) {
                kanban.setStatus(Kanban.STATUS_AVAILABLE);
                kanbanMapper.updateById(kanban);
                count++;
            }
        }
        return count;
    }
}
