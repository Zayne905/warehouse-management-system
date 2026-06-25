package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.InboundOrderMapper;
import com.warehouse.mapper.KanbanMapper;
import com.warehouse.mapper.OutboundScanMapper;
import com.warehouse.mapper.RepackRelationMapper;
import com.warehouse.mapper.ScanRecordMapper;
import com.warehouse.model.dto.InboundDetailDTO;
import com.warehouse.model.entity.InboundOrder;
import com.warehouse.model.entity.Kanban;
import com.warehouse.model.entity.OutboundScan;
import com.warehouse.model.entity.Part;
import com.warehouse.model.entity.RepackRelation;
import com.warehouse.model.entity.ScanRecord;
import com.warehouse.model.entity.WarehouseArea;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KanbanService {

    private final KanbanMapper kanbanMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundScanMapper outboundScanMapper;
    private final ScanRecordMapper scanRecordMapper;
    private final RepackRelationMapper repackRelationMapper;
    private final PartService partService;
    private final WarehouseAreaService warehouseAreaService;

    public KanbanService(KanbanMapper kanbanMapper,
                         InboundOrderMapper inboundOrderMapper,
                         OutboundScanMapper outboundScanMapper,
                         ScanRecordMapper scanRecordMapper,
                         RepackRelationMapper repackRelationMapper,
                         PartService partService,
                         WarehouseAreaService warehouseAreaService) {
        this.kanbanMapper = kanbanMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.outboundScanMapper = outboundScanMapper;
        this.scanRecordMapper = scanRecordMapper;
        this.repackRelationMapper = repackRelationMapper;
        this.partService = partService;
        this.warehouseAreaService = warehouseAreaService;
    }

    /**
     * 生成看板编号。使用订单创建日期（而非当天日期），确保编号永久稳定。
     * 格式: R-yyyy-MM-dd-{orderNo}-{partCode}C-{boxSeq}
     */
    private String generateKanbanNo(InboundOrder order, Part part, int boxSeq) {
        String dateStr;
        if (order.getCreateTime() != null) {
            dateStr = order.getCreateTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        } else {
            dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return String.format("R-%s-%s-%sC-%d", dateStr, order.getOrderNo(), part.getCode(), boxSeq);
    }

    /**
     * 构建看板二维码JSON内容。与前端扫描器约定的字段完全一致。
     * 此内容在看板创建时生成并持久化，全生命周期不可变。
     */
    private String buildQrContent(Kanban kanban) {
        Map<String, Object> qr = new LinkedHashMap<>();
        qr.put("kanbanNo", kanban.getKanbanNo());
        qr.put("inboundOrderNo", kanban.getInboundOrderNo());
        qr.put("partCode", kanban.getPartCode());
        qr.put("partName", kanban.getPartName());
        qr.put("quantity", kanban.getQuantity());
        qr.put("boxSeq", kanban.getBoxSeq());
        qr.put("supplierName", kanban.getSupplierName());
        qr.put("warehouseArea", kanban.getWarehouseAreaName());
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(qr);
        } catch (Exception e) {
            // fallback: manual JSON
            return "{\"kanbanNo\":\"" + kanban.getKanbanNo() + "\","
                    + "\"inboundOrderNo\":\"" + (kanban.getInboundOrderNo() != null ? kanban.getInboundOrderNo() : "") + "\","
                    + "\"partCode\":\"" + (kanban.getPartCode() != null ? kanban.getPartCode() : "") + "\","
                    + "\"partName\":\"" + (kanban.getPartName() != null ? kanban.getPartName() : "") + "\","
                    + "\"quantity\":" + (kanban.getQuantity() != null ? kanban.getQuantity() : 0) + ","
                    + "\"boxSeq\":" + (kanban.getBoxSeq() != null ? kanban.getBoxSeq() : 0) + ","
                    + "\"supplierName\":\"" + (kanban.getSupplierName() != null ? kanban.getSupplierName() : "") + "\","
                    + "\"warehouseArea\":\"" + (kanban.getWarehouseAreaName() != null ? kanban.getWarehouseAreaName() : "") + "\"}";
        }
    }

    /**
     * 入库看板生成：增量更新，不删除已有看板。
     * - 已有看板保留原 kanbanNo，已扫描/已使用的不受影响
     * - 新增箱数为已有看板补建新看板
     * - 减少的箱仅删除状态为 PENDING_INBOUND 且无扫描记录的
     */
    @Transactional
    public List<Kanban> generateForOrder(Long orderId, List<InboundDetailDTO> details) {
        InboundOrder order = inboundOrderMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("入库单不存在");

        // 获取已有看板，按 (partId, boxSeq) 分组
        List<Kanban> existingKb = kanbanMapper.selectList(
                new QueryWrapper<Kanban>().eq("inbound_order_id", orderId).orderByAsc("box_seq"));
        // key: partId -> 该零件的已有看板列表 (按 boxSeq 排序)
        Map<Long, List<Kanban>> existingByPart = new LinkedHashMap<>();
        for (Kanban kb : existingKb) {
            Long pid = kb.getPartId();
            if (pid == null) continue;
            existingByPart.computeIfAbsent(pid, k -> new ArrayList<>()).add(kb);
        }

        List<Kanban> result = new ArrayList<>();
        // 记录本次需要的看板 key: partId -> 需要的 seeq 集合
        Map<Long, java.util.Set<Integer>> neededSeqs = new HashMap<>();

        for (InboundDetailDTO dto : details) {
            Part part = partService.getById(dto.getPartId());
            if (part == null) continue;

            BigDecimal boxCount = dto.getBoxCount() != null ? dto.getBoxCount() : BigDecimal.ZERO;
            if (boxCount.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal capacity = BigDecimal.valueOf(part.getPackageCapacity() != null ? part.getPackageCapacity() : 1);
            BigDecimal totalQty = dto.getPlannedQty() != null ? dto.getPlannedQty() : capacity.multiply(boxCount);
            int labelCount = boxCount.setScale(0, RoundingMode.CEILING).intValue();
            BigDecimal remaining = totalQty;

            neededSeqs.computeIfAbsent(dto.getPartId(), k -> new java.util.HashSet<>());

            for (int seq = 0; seq < labelCount && remaining.compareTo(BigDecimal.ZERO) > 0; seq++) {
                neededSeqs.get(dto.getPartId()).add(seq);
                BigDecimal boxQty = remaining.min(capacity);

                // 检查该 partId + seq 是否已有看板
                Kanban existing = null;
                List<Kanban> partKbs = existingByPart.get(dto.getPartId());
                if (partKbs != null) {
                    for (Kanban kb : partKbs) {
                        if (kb.getBoxSeq() != null && kb.getBoxSeq() == seq) {
                            existing = kb;
                            break;
                        }
                    }
                }

                if (existing != null) {
                    // 已有看板：更新数量和库区信息，但保持 kanbanNo 不变
                    existing.setQuantity(boxQty);
                    existing.setOriginalQty(capacity);
                    existing.setSupplierName(order.getSupplierName());
                    Long areaId = dto.getWarehouseAreaId() != null ? dto.getWarehouseAreaId() : part.getWarehouseAreaId();
                    existing.setWarehouseAreaId(areaId);
                    if (areaId != null) {
                        WarehouseArea area = warehouseAreaService.getById(areaId);
                        existing.setWarehouseAreaName(area != null ? area.getName() : null);
                    }
                    kanbanMapper.updateById(existing);
                    result.add(existing);
                } else {
                    // 新看板
                    Kanban kanban = new Kanban();
                    kanban.setKanbanNo(generateKanbanNo(order, part, seq));
                    kanban.setInboundOrderId(orderId);
                    kanban.setInboundOrderNo(order.getOrderNo());
                    kanban.setPartId(part.getId());
                    kanban.setPartCode(part.getCode());
                    kanban.setPartName(part.getName());
                    kanban.setSupplierName(order.getSupplierName());
                    kanban.setQuantity(boxQty);
                    kanban.setOriginalQty(capacity);
                    kanban.setBoxSeq(seq);
                    Long areaId = dto.getWarehouseAreaId() != null ? dto.getWarehouseAreaId() : part.getWarehouseAreaId();
                    kanban.setWarehouseAreaId(areaId);
                    if (areaId != null) {
                        WarehouseArea area = warehouseAreaService.getById(areaId);
                        kanban.setWarehouseAreaName(area != null ? area.getName() : null);
                    }
                    kanban.setStatus(Kanban.STATUS_PENDING_INBOUND);
                    kanban.setQrContent(buildQrContent(kanban));
                    kanbanMapper.insert(kanban);
                    result.add(kanban);
                }
                remaining = remaining.subtract(boxQty);
            }
        }

        // 删除本次不需要的看板（仅限 PENDING_INBOUND 且无扫描记录的）
        for (Kanban kb : existingKb) {
            if (kb.getPartId() == null) continue;
            java.util.Set<Integer> needed = neededSeqs.get(kb.getPartId());
            if (needed == null || !needed.contains(kb.getBoxSeq() != null ? kb.getBoxSeq() : -1)) {
                // 只删除待入库状态的
                if (kb.getStatus() != null && kb.getStatus() == Kanban.STATUS_PENDING_INBOUND) {
                    // 检查是否有扫描记录
                    Long scanCount = scanRecordMapper.selectCount(
                            new QueryWrapper<ScanRecord>().eq("kanban_no", kb.getKanbanNo()));
                    if (scanCount == null || scanCount == 0) {
                        kanbanMapper.deleteById(kb.getId());
                    }
                }
            }
        }

        return result;
    }

    public List<Kanban> listByOrderId(Long orderId) {
        return kanbanMapper.selectList(
                new QueryWrapper<Kanban>().eq("inbound_order_id", orderId).orderByAsc("box_seq"));
    }

    public List<Kanban> listByPartId(Long partId) {
        List<Kanban> list = kanbanMapper.selectList(
                new QueryWrapper<Kanban>().eq("part_id", partId).orderByAsc("create_time"));
        List<String> outboundNos = list.stream()
                .filter(k -> k.getStatus() == Kanban.STATUS_OUTBOUND && k.getKanbanNo() != null)
                .map(Kanban::getKanbanNo).toList();
        if (!outboundNos.isEmpty()) {
            List<OutboundScan> scans = outboundScanMapper.selectList(
                    new QueryWrapper<OutboundScan>().in("kanban_no", outboundNos).orderByDesc("scan_time"));
            Map<String, LocalDateTime> scanTimes = new HashMap<>();
            for (OutboundScan scan : scans) scanTimes.putIfAbsent(scan.getKanbanNo(), scan.getScanTime());
            for (Kanban kanban : list) kanban.setOutboundScanTime(scanTimes.get(kanban.getKanbanNo()));
        }
        return list;
    }

    public List<Map<String, Object>> list(Map<String, Object> query) {
        String keyword = text(query.get("keyword"));
        String supplierName = text(query.get("supplierName"));
        String warehouseAreaName = text(query.get("warehouseAreaName"));
        Integer status = query.get("status") == null || "".equals(query.get("status"))
                ? null : Integer.valueOf(query.get("status").toString());

        QueryWrapper<Kanban> wrapper = new QueryWrapper<>();
        if (!keyword.isEmpty()) {
            wrapper.and(w -> w.like("inbound_order_no", keyword)
                    .or().like("outbound_order_no", keyword)
                    .or().like("kanban_no", keyword)
                    .or().like("part_code", keyword));
        }
        if (!supplierName.isEmpty()) wrapper.eq("supplier_name", supplierName);
        if (!warehouseAreaName.isEmpty()) wrapper.eq("warehouse_area_name", warehouseAreaName);
        if (status != null) wrapper.eq("status", status);
        wrapper.orderByDesc("create_time");

        List<Map<String, Object>> result = new ArrayList<>();
        for (Kanban kanban : kanbanMapper.selectList(wrapper)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", kanban.getId());
            row.put("kanbanNo", kanban.getKanbanNo());
            row.put("qrContent", kanban.getQrContent());
            row.put("inboundOrderNo", kanban.getInboundOrderNo());
            row.put("outboundOrderNo", kanban.getOutboundOrderNo());
            row.put("partCode", kanban.getPartCode());
            row.put("partName", kanban.getPartName());
            row.put("supplierName", kanban.getSupplierName());
            row.put("status", kanban.getStatus());
            row.put("statusText", kanban.getStatusText());
            row.put("quantity", kanban.getQuantity());
            row.put("originalQty", kanban.getOriginalQty());
            row.put("warehouseName", "主仓库");
            row.put("warehouseAreaName", kanban.getWarehouseAreaName());
            row.put("createTime", kanban.getCreateTime());
            result.add(row);
        }
        return result;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    /**
     *看板生命周期
     */
    public Map<String, Object> getLifecycle(String kanbanNo) {
        Kanban kanban = findRequired(kanbanNo);
        Part part = partService.getById(kanban.getPartId());
        ScanRecord inboundScan = scanRecordMapper.selectOne(
                new QueryWrapper<ScanRecord>().eq("kanban_no", kanbanNo).last("LIMIT 1"));
        if (inboundScan == null && kanban.getInboundOrderId() != null) {
            inboundScan = scanRecordMapper.selectOne(
                    new QueryWrapper<ScanRecord>()
                            .eq("inbound_order_id", kanban.getInboundOrderId())
                            .eq("part_id", kanban.getPartId())
                            .orderByAsc("scan_time")
                            .last("LIMIT 1"));
        }
        OutboundScan outboundScan = outboundScanMapper.selectOne(
                new QueryWrapper<OutboundScan>().eq("kanban_no", kanbanNo).orderByDesc("scan_time").last("LIMIT 1"));
        List<RepackRelation> repacks = repackRelationMapper.selectList(
                new QueryWrapper<RepackRelation>()
                        .and(w -> w.eq("parent_kanban_no", kanbanNo).or().eq("child_kanban_no", kanbanNo))
                        .orderByAsc("repack_time"));

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("kanbanNo", kanban.getKanbanNo());
        info.put("qrContent", kanban.getQrContent());
        info.put("inboundOrderNo", kanban.getInboundOrderNo());
        info.put("outboundOrderNo", kanban.getOutboundOrderNo());
        info.put("partCode", kanban.getPartCode());
        info.put("partName", kanban.getPartName());
        info.put("supplierName", kanban.getSupplierName());
        info.put("status", kanban.getStatus());
        info.put("statusText", kanban.getStatusText());
        info.put("quantity", kanban.getQuantity());
        info.put("originalQty", kanban.getOriginalQty());
        info.put("warehouseName", "主仓库");
        info.put("warehouseAreaName", kanban.getWarehouseAreaName());
        info.put("containerModel", part != null ? part.getSpec() : null);
        info.put("boxSeq", kanban.getBoxSeq());
        info.put("createTime", kanban.getCreateTime());
        info.put("inboundTime", inboundScan != null ? inboundScan.getScanTime() : null);
        info.put("outboundTime", outboundScan != null ? outboundScan.getScanTime() : null);
        info.put("repackRecords", repacks);

        List<Map<String, Object>> events = new ArrayList<>();
        addEvent(events, kanban.getCreateTime(), "CREATED", "看板创建", kanban.getInboundOrderNo());
        if (inboundScan != null) addEvent(events, inboundScan.getScanTime(), "INBOUND", "扫码入库", inboundScan.getInboundOrderNo());
        for (RepackRelation relation : repacks) {
            String action = kanbanNo.equals(relation.getParentKanbanNo()) ? "转包转出" : "转包生成";
            addEvent(events, relation.getRepackTime(), "REPACK", action, relation.getRepackOrderNo());
        }
        if (outboundScan != null) addEvent(events, outboundScan.getScanTime(), "OUTBOUND", "扫码出库", outboundScan.getOutboundOrderNo());
        events.sort(Comparator.comparing(e -> (LocalDateTime) e.get("time"), Comparator.nullsLast(Comparator.naturalOrder())));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kanban", info);
        result.put("events", events);
        return result;
    }

    private void addEvent(List<Map<String, Object>> events, LocalDateTime time, String type, String title, String orderNo) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("time", time);
        event.put("type", type);
        event.put("title", title);
        event.put("orderNo", orderNo);
        events.add(event);
    }

    private Kanban findRequired(String kanbanNo) {
        Kanban kanban = kanbanMapper.selectOne(new QueryWrapper<Kanban>().eq("kanban_no", kanbanNo));
        if (kanban == null) throw new RuntimeException("看板不存在: " + kanbanNo);
        return kanban;
    }

    @Transactional
    public Map<String, Object> toggleBlock(String kanbanNo) {
        Kanban kanban = findRequired(kanbanNo);
        int oldStatus = kanban.getStatus();
        if (oldStatus == Kanban.STATUS_AVAILABLE) kanban.setStatus(Kanban.STATUS_BLOCKED);
        else if (oldStatus == Kanban.STATUS_BLOCKED) kanban.setStatus(Kanban.STATUS_AVAILABLE);
        else throw new RuntimeException("当前状态不允许封存或解封: " + kanban.getStatusText());
        kanbanMapper.updateById(kanban);
        Map<String, Object> result = new HashMap<>();
        result.put("kanbanNo", kanban.getKanbanNo());
        result.put("partName", kanban.getPartName());
        result.put("partCode", kanban.getPartCode());
        result.put("previousStatus", oldStatus);
        result.put("previousStatusText", oldStatus == Kanban.STATUS_AVAILABLE ? "在库可用" : "封存");
        result.put("action", kanban.getStatus() == Kanban.STATUS_BLOCKED ? "封存" : "解封");
        result.put("newStatus", kanban.getStatus());
        return result;
    }

    @Transactional
    public void blockKanban(String kanbanNo) {
        Kanban kanban = findRequired(kanbanNo);
        if (kanban.getStatus() != Kanban.STATUS_AVAILABLE) throw new RuntimeException("只有在库可用看板才能封存");
        kanban.setStatus(Kanban.STATUS_BLOCKED);
        kanbanMapper.updateById(kanban);
    }

    @Transactional
    public void unblockKanban(String kanbanNo) {
        Kanban kanban = findRequired(kanbanNo);
        if (kanban.getStatus() != Kanban.STATUS_BLOCKED) throw new RuntimeException("只有封存看板才能解封");
        kanban.setStatus(Kanban.STATUS_AVAILABLE);
        kanbanMapper.updateById(kanban);
    }

    @Transactional
    public int blockByPartId(Long partId) {
        List<Kanban> list = kanbanMapper.selectList(
                new QueryWrapper<Kanban>().eq("part_id", partId).eq("status", Kanban.STATUS_AVAILABLE));
        list.forEach(k -> {
            k.setStatus(Kanban.STATUS_BLOCKED);
            kanbanMapper.updateById(k);
        });
        return list.size();
    }

    @Transactional
    public int batchBlock(List<String> kanbanNos) {
        return updateSelectedStatus(kanbanNos, Kanban.STATUS_AVAILABLE, Kanban.STATUS_BLOCKED);
    }

    @Transactional
    public int batchUnblock(List<String> kanbanNos) {
        return updateSelectedStatus(kanbanNos, Kanban.STATUS_BLOCKED, Kanban.STATUS_AVAILABLE);
    }

    private int updateSelectedStatus(List<String> kanbanNos, int from, int to) {
        if (kanbanNos == null || kanbanNos.isEmpty()) return 0;
        int count = 0;
        for (String no : kanbanNos) {
            Kanban kanban = kanbanMapper.selectOne(new QueryWrapper<Kanban>().eq("kanban_no", no));
            if (kanban != null && kanban.getStatus() == from) {
                kanban.setStatus(to);
                kanbanMapper.updateById(kanban);
                count++;
            }
        }
        return count;
    }
}
