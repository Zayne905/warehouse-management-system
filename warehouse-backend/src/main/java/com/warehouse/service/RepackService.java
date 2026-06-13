package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warehouse.mapper.*;
import com.warehouse.model.dto.*;
import com.warehouse.model.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RepackService {

    private final RepackOrderMapper repackOrderMapper;
    private final RepackOrderDetailMapper repackOrderDetailMapper;
    private final RepackRelationMapper repackRelationMapper;
    private final KanbanMapper kanbanMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderDetailMapper inboundOrderDetailMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final PartService partService;
    private final WarehouseAreaService warehouseAreaService;

    public RepackService(RepackOrderMapper repackOrderMapper,
                         RepackOrderDetailMapper repackOrderDetailMapper,
                         RepackRelationMapper repackRelationMapper,
                         KanbanMapper kanbanMapper,
                         OutboundOrderMapper outboundOrderMapper,
                         InboundOrderMapper inboundOrderMapper,
                         InboundOrderDetailMapper inboundOrderDetailMapper,
                         OrderNoGenerator orderNoGenerator,
                         PartService partService,
                         WarehouseAreaService warehouseAreaService) {
        this.repackOrderMapper = repackOrderMapper;
        this.repackOrderDetailMapper = repackOrderDetailMapper;
        this.repackRelationMapper = repackRelationMapper;
        this.kanbanMapper = kanbanMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundOrderDetailMapper = inboundOrderDetailMapper;
        this.orderNoGenerator = orderNoGenerator;
        this.partService = partService;
        this.warehouseAreaService = warehouseAreaService;
    }

    // ==================== 单号生成 ====================

    public synchronized String generateOrderNo() {
        String todayPrefix = "RP" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        QueryWrapper<RepackOrder> wrapper = new QueryWrapper<>();
        wrapper.apply("order_no LIKE CONCAT({0}, '%')", todayPrefix).orderByDesc("order_no");
        List<RepackOrder> list = repackOrderMapper.selectList(wrapper);
        int seq = 1;
        if (list != null && !list.isEmpty()) {
            String lastNo = list.get(0).getOrderNo();
            if (lastNo != null && lastNo.length() >= 12) {
                try { seq = Integer.parseInt(lastNo.substring(10)) + 1; } catch (NumberFormatException e) { seq = 1; }
            }
        }
        return todayPrefix + String.format("%03d", seq);
    }

    private String generateTargetKanbanNo(String orderNo, String partCode) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String prefix = "R-" + dateStr + "-" + orderNo + "-" + partCode + "C-";
        return prefix + kanbanMapper.selectCount(new QueryWrapper<Kanban>().likeRight("kanban_no", prefix));
    }

    // ==================== 创建转包单（必须指定零件+库区） ====================

    @Transactional
    public RepackOrderVO createRepackOrder(RepackOrderSaveDTO dto) {
        if (dto.getRepackType() == null || dto.getRepackType().isEmpty())
            throw new RuntimeException("转包类型不能为空");
        if (dto.getPartId() == null) throw new RuntimeException("请选择零件");
        if (dto.getWarehouseAreaId() == null) throw new RuntimeException("请选择库区");

        Part part = partService.getById(dto.getPartId());
        if (part == null) throw new RuntimeException("零件不存在");
        WarehouseArea area = warehouseAreaService.getById(dto.getWarehouseAreaId());
        if (area == null) throw new RuntimeException("库区不存在");

        String orderNo = generateOrderNo();
        RepackOrder order = new RepackOrder();
        order.setOrderNo(orderNo);
        order.setStatus(RepackOrder.STATUS_PENDING);
        order.setRepackType(dto.getRepackType());
        order.setPartId(part.getId());
        order.setPartCode(part.getCode());
        order.setPartName(part.getName());
        order.setWarehouseAreaId(area.getId());
        order.setWarehouseAreaName(area.getName());
        order.setTargetBoxCapacity(dto.getTargetBoxCapacity());
        order.setRemark(dto.getRemark());
        order.setOperatorId(dto.getOperatorId());
        repackOrderMapper.insert(order);

        return buildVO(order);
    }

    // ==================== 逐行添加明细 ====================

    @Transactional
    public RepackOrderVO addDetailLine(Long orderId, String sourceKanbanNo, Integer transferQty) {
        RepackOrder order = getPendingOrder(orderId);
        if (sourceKanbanNo == null || sourceKanbanNo.isEmpty()) throw new RuntimeException("源看板号不能为空");

        Kanban source = getAndValidateSource(sourceKanbanNo, order);
        String type = order.getRepackType();
        List<RepackOrderDetail> existDetails = getDetails(orderId);

        // 向上转包：去重，自动取全部剩余量
        if ("CONSOLIDATE".equals(type)) {
            boolean already = existDetails.stream().anyMatch(d -> d.getSourceKanbanNo().equals(sourceKanbanNo));
            if (already) throw new RuntimeException("已添加过该看板[" + sourceKanbanNo + "]，不能重复添加");
            transferQty = source.getQuantity(); // 自动取全部剩余量
        }

        if (transferQty == null || transferQty <= 0) throw new RuntimeException("转出数量必须大于0");

        if ("REMAINDER".equals(type)) {
            if (!existDetails.isEmpty()) throw new RuntimeException("带余量转包只能有1个目标包装");
            if (transferQty >= source.getQuantity())
                throw new RuntimeException("转出数量(" + transferQty + ")必须 < 源数量(" + source.getQuantity() + ")");
        }

        // 向下转包：所有行同一源
        if ("BREAKDOWN".equals(type) && !existDetails.isEmpty()) {
            if (!existDetails.get(0).getSourceKanbanNo().equals(sourceKanbanNo))
                throw new RuntimeException("向下转包所有目标必须拆自同一源[" + existDetails.get(0).getSourceKanbanNo() + "]");
            int sum = existDetails.stream().mapToInt(d -> d.getTransferQty() != null ? d.getTransferQty() : 0).sum();
            if (sum + transferQty > source.getQuantity())
                throw new RuntimeException("总拆出量(" + (sum + transferQty) + ")超过源数量(" + source.getQuantity() + ")");
        }

        int nextLine = existDetails.stream().mapToInt(RepackOrderDetail::getLineNo).max().orElse(0) + 1;
        RepackOrderDetail entity = new RepackOrderDetail();
        entity.setRepackOrderId(orderId);
        entity.setSourceKanbanId(source.getId()); entity.setSourceKanbanNo(source.getKanbanNo());
        entity.setPartId(source.getPartId()); entity.setPartCode(source.getPartCode()); entity.setPartName(source.getPartName());
        entity.setTransferQty(transferQty); entity.setRemainderQty(0); entity.setLineNo(nextLine);
        repackOrderDetailMapper.insert(entity);
        return buildVO(order);
    }

    @Transactional
    public RepackOrderVO removeDetailLine(Long detailId) {
        RepackOrderDetail detail = repackOrderDetailMapper.selectById(detailId);
        if (detail == null) throw new RuntimeException("明细不存在");
        RepackOrder order = getPendingOrder(detail.getRepackOrderId());
        repackOrderDetailMapper.deleteById(detailId);
        return buildVO(order);
    }

    // ==================== 向下转包：扫源箱+容量 → 一步生成 ====================

    @Transactional
    public RepackOrderVO breakdownAdd(Long orderId, Integer targetBoxCapacity) {
        RepackOrder order = getPendingOrder(orderId);
        if (!"BREAKDOWN".equals(order.getRepackType())) throw new RuntimeException("仅向下转包支持此操作");
        if (targetBoxCapacity == null || targetBoxCapacity <= 0) throw new RuntimeException("目标箱容量必须>0");

        // 必须有且只有1行明细（用户已扫码的源箱）
        List<RepackOrderDetail> existDetails = getDetails(orderId);
        if (existDetails.isEmpty()) throw new RuntimeException("请先扫码添加源箱");
        if (existDetails.size() > 1) {
            // 已有拆包方案，删除重建
            repackOrderDetailMapper.delete(new QueryWrapper<RepackOrderDetail>().eq("repack_order_id", orderId));
            existDetails = getDetails(orderId);
            if (existDetails.isEmpty()) throw new RuntimeException("请先扫码添加源箱");
        }

        RepackOrderDetail sourceDetail = existDetails.get(0);
        Kanban source = reloadAndRecheck(sourceDetail);
        int totalQty = source.getQuantity();  // 源箱总数量（拆包必须全拆）

        int capacity = targetBoxCapacity;
        int boxCount = Math.max(2, (int) Math.ceil((double) totalQty / capacity));
        if (boxCount < 2) boxCount = 2; // 最少2箱

        order.setTargetBoxCapacity(capacity);
        repackOrderMapper.updateById(order);

        repackOrderDetailMapper.delete(new QueryWrapper<RepackOrderDetail>().eq("repack_order_id", orderId));

        int remaining = totalQty;
        for (int i = 0; i < boxCount; i++) {
            int qty = (i == boxCount - 1) ? remaining : Math.min(capacity, remaining - (boxCount - i - 1));
            if (qty <= 0) break;
            remaining -= qty;
            RepackOrderDetail entity = new RepackOrderDetail();
            entity.setRepackOrderId(order.getId());
            entity.setSourceKanbanId(source.getId()); entity.setSourceKanbanNo(source.getKanbanNo());
            entity.setPartId(source.getPartId()); entity.setPartCode(source.getPartCode()); entity.setPartName(source.getPartName());
            entity.setTransferQty(qty); entity.setLineNo(i + 1);
            repackOrderDetailMapper.insert(entity);
        }
        return buildVO(order);
    }

    // ==================== 确认执行 ====================

    @Transactional
    public RepackOrderVO confirmRepack(Long orderId) {
        RepackOrder order = getPendingOrder(orderId);
        List<RepackOrderDetail> details = getDetails(orderId);
        if (details.isEmpty()) throw new RuntimeException("转包单无明细");

        String type = order.getRepackType();
        if ("BREAKDOWN".equals(type) && details.size() < 2) throw new RuntimeException("向下转包至少需要2个目标包装");
        if ("CONSOLIDATE".equals(type) && details.size() < 2) throw new RuntimeException("向上转包至少需要2个源包装");

        List<Kanban> newKanbans;
        switch (type) {
            case "BREAKDOWN":  newKanbans = execBreakdown(details, order); break;
            case "CONSOLIDATE": newKanbans = execConsolidate(details, order); break;
            case "REMAINDER":   newKanbans = execRemainder(details, order); break;
            default: throw new RuntimeException("不支持的转包类型: " + type);
        }

        // 自动创建入库单，完成入库
        autoInbound(order, newKanbans);

        order.setStatus(RepackOrder.STATUS_COMPLETED);
        repackOrderMapper.updateById(order);
        return buildVO(order);
    }

    // ---- 向下转包 ----

    private List<Kanban> execBreakdown(List<RepackOrderDetail> details, RepackOrder order) {
        Kanban source = reloadAndRecheck(details.get(0));
        int total = details.stream().mapToInt(RepackOrderDetail::getTransferQty).sum();
        if (total > source.getQuantity()) throw new RuntimeException("总拆出量(" + total + ")超过源数量(" + source.getQuantity() + ")");

        List<Kanban> newKanbans = new ArrayList<>();
        for (RepackOrderDetail d : details) {
            String no = generateTargetKanbanNo(order.getOrderNo(), source.getPartCode());
            Kanban k = newTargetKanban(no, source, d.getTransferQty());
            kanbanMapper.insert(k);
            newKanbans.add(k);
            saveRelation(source.getKanbanNo(), no, order, d.getTransferQty(), source);
            updateDetailResult(d, no, source.getQuantity() - total);
        }
        source.setQuantity(source.getQuantity() - total);
        source.setStatus(source.getQuantity() <= 0 ? Kanban.STATUS_CLEARED : Kanban.STATUS_PARTIAL_REPACK);
        kanbanMapper.updateById(source);
        return newKanbans;
    }

    // ---- 向上转包 ----

    private List<Kanban> execConsolidate(List<RepackOrderDetail> details, RepackOrder order) {
        List<Kanban> sources = new ArrayList<>();
        int totalQty = 0;
        for (RepackOrderDetail d : details) {
            Kanban s = reloadAndRecheck(d);
            sources.add(s);
            totalQty += d.getTransferQty();
        }
        Kanban first = sources.get(0);
        String no = generateTargetKanbanNo(order.getOrderNo(), first.getPartCode());
        Kanban target = newTargetKanban(no, first, totalQty);
        kanbanMapper.insert(target);

        for (int i = 0; i < sources.size(); i++) {
            Kanban s = sources.get(i);
            RepackOrderDetail d = details.get(i);
            int rem = s.getQuantity() - d.getTransferQty();
            s.setQuantity(rem);
            s.setStatus(rem <= 0 ? Kanban.STATUS_CLEARED : Kanban.STATUS_PARTIAL_REPACK);
            kanbanMapper.updateById(s);
            saveRelation(s.getKanbanNo(), no, order, d.getTransferQty(), s);
            updateDetailResult(d, no, rem);
        }
        return Collections.singletonList(target);
    }

    // ---- 带余量转包 ----

    private List<Kanban> execRemainder(List<RepackOrderDetail> details, RepackOrder order) {
        RepackOrderDetail d = details.get(0);
        Kanban source = reloadAndRecheck(d);
        int remaining = source.getQuantity() - d.getTransferQty();
        if (remaining <= 0) throw new RuntimeException("带余量转包必须有剩余，当前转出" + d.getTransferQty() + "，源" + source.getQuantity());

        String no = generateTargetKanbanNo(order.getOrderNo(), source.getPartCode());
        Kanban target = newTargetKanban(no, source, d.getTransferQty());
        kanbanMapper.insert(target);

        source.setQuantity(remaining);
        source.setStatus(Kanban.STATUS_PARTIAL_REPACK);
        kanbanMapper.updateById(source);
        saveRelation(source.getKanbanNo(), no, order, d.getTransferQty(), source);
        updateDetailResult(d, no, remaining);
        return Collections.singletonList(target);
    }

    // ==================== 自动入库 ====================

    private void autoInbound(RepackOrder order, List<Kanban> newKanbans) {
        if (newKanbans.isEmpty()) return;

        // 创建入库单
        String inboundNo = orderNoGenerator.generate();
        InboundOrder ib = new InboundOrder();
        ib.setOrderNo(inboundNo);
        ib.setSupplierId(0L); ib.setSupplierName("转包生成");
        ib.setOrderNumber(order.getOrderNo()); // 关联转包单号
        ib.setStatus(2); // 已入库
        ib.setRemark("转包单[" + order.getOrderNo() + "]自动生成");
        ib.setCreateUserId(order.getOperatorId());
        inboundOrderMapper.insert(ib);

        // 创建入库明细 + 更新看板入库信息
        for (Kanban k : newKanbans) {
            InboundOrderDetail detail = new InboundOrderDetail();
            detail.setInboundOrderId(ib.getId());
            detail.setPartId(k.getPartId()); detail.setPartCode(k.getPartCode()); detail.setPartName(k.getPartName());
            detail.setUnit("件");
            detail.setPlannedQty(java.math.BigDecimal.valueOf(k.getQuantity()));
            detail.setActualQty(java.math.BigDecimal.valueOf(k.getQuantity()));
            detail.setWarehouseAreaId(k.getWarehouseAreaId());
            detail.setBoxCount(1);
            detail.setLineNo(1);
            inboundOrderDetailMapper.insert(detail);

            // 更新看板指向新入库单
            k.setInboundOrderId(ib.getId());
            k.setInboundOrderNo(inboundNo);
            kanbanMapper.updateById(k);
        }
    }

    // ==================== 辅助方法 ====================

    private RepackOrder getPendingOrder(Long id) {
        RepackOrder o = repackOrderMapper.selectById(id);
        if (o == null) throw new RuntimeException("转包单不存在");
        if (o.getStatus() != RepackOrder.STATUS_PENDING) throw new RuntimeException("只有待转包状态的转包单才能操作");
        return o;
    }

    private List<RepackOrderDetail> getDetails(Long orderId) {
        return repackOrderDetailMapper.selectList(
                new QueryWrapper<RepackOrderDetail>().eq("repack_order_id", orderId).orderByAsc("line_no"));
    }

    /** 校验看板：状态 + 零件匹配 + 库区匹配 */
    private Kanban getAndValidateSource(String kanbanNo, RepackOrder order) {
        Kanban s = kanbanMapper.selectOne(new QueryWrapper<Kanban>().eq("kanban_no", kanbanNo));
        if (s == null) throw new RuntimeException("看板[" + kanbanNo + "]不存在");
        if (s.getStatus() != Kanban.STATUS_AVAILABLE && s.getStatus() != Kanban.STATUS_PARTIAL_REPACK)
            throw new RuntimeException("看板[" + kanbanNo + "]状态[" + s.getStatusText() + "]，不可转包");
        if (!Objects.equals(s.getPartId(), order.getPartId()))
            throw new RuntimeException("零件不匹配！转包单限定[" + order.getPartCode() + "]，当前看板[" + s.getPartCode() + "]");
        if (!Objects.equals(s.getWarehouseAreaId(), order.getWarehouseAreaId()))
            throw new RuntimeException("库区不匹配！转包单限定[" + order.getWarehouseAreaName() + "]，当前看板[" + s.getWarehouseAreaName() + "]");
        return s;
    }

    private Kanban newTargetKanban(String kanbanNo, Kanban source, int quantity) {
        Kanban t = new Kanban();
        t.setKanbanNo(kanbanNo);
        t.setInboundOrderId(null); t.setInboundOrderNo(null); // 待自动入库时填充
        t.setPartId(source.getPartId()); t.setPartCode(source.getPartCode()); t.setPartName(source.getPartName());
        t.setSupplierName(source.getSupplierName());
        t.setQuantity(quantity); t.setOriginalQty(quantity);
        t.setBoxSeq(0);
        t.setWarehouseAreaId(source.getWarehouseAreaId()); t.setWarehouseAreaName(source.getWarehouseAreaName());
        t.setStatus(Kanban.STATUS_AVAILABLE);
        return t;
    }

    private Kanban reloadAndRecheck(RepackOrderDetail detail) {
        Kanban s = kanbanMapper.selectById(detail.getSourceKanbanId());
        if (s == null) throw new RuntimeException("源看板[" + detail.getSourceKanbanNo() + "]不存在");
        if (s.getStatus() != Kanban.STATUS_AVAILABLE && s.getStatus() != Kanban.STATUS_PARTIAL_REPACK)
            throw new RuntimeException("源看板[" + s.getKanbanNo() + "]状态[" + s.getStatusText() + "]，不允许转出");
        if (detail.getTransferQty() > s.getQuantity())
            throw new RuntimeException("源看板[" + s.getKanbanNo() + "]数量(" + s.getQuantity() + ")不足");
        return s;
    }

    private void saveRelation(String parent, String child, RepackOrder order, int qty, Kanban s) {
        RepackRelation r = new RepackRelation();
        r.setParentKanbanNo(parent); r.setChildKanbanNo(child);
        r.setRepackOrderId(order.getId()); r.setRepackOrderNo(order.getOrderNo());
        r.setTransferQty(qty); r.setPartId(s.getPartId()); r.setPartCode(s.getPartCode()); r.setPartName(s.getPartName());
        r.setOperatorId(order.getOperatorId()); r.setRepackTime(java.time.LocalDateTime.now());
        repackRelationMapper.insert(r);
    }

    private void updateDetailResult(RepackOrderDetail d, String targetNo, int remaining) {
        d.setTargetKanbanNo(targetNo); d.setSourceRemainingQty(remaining);
        repackOrderDetailMapper.updateById(d);
    }

    // ==================== 取消/删除 ====================

    @Transactional
    public void cancelRepack(Long orderId) { RepackOrder o = getPendingOrder(orderId); o.setStatus(RepackOrder.STATUS_CANCELLED); repackOrderMapper.updateById(o); }

    @Transactional
    public void deleteRepack(Long orderId) {
        RepackOrder o = repackOrderMapper.selectById(orderId);
        if (o == null) throw new RuntimeException("转包单不存在");
        if (o.getStatus() == RepackOrder.STATUS_COMPLETED) throw new RuntimeException("已完成的转包单不能删除");
        repackOrderDetailMapper.delete(new QueryWrapper<RepackOrderDetail>().eq("repack_order_id", orderId));
        repackRelationMapper.delete(new QueryWrapper<RepackRelation>().eq("repack_order_id", orderId));
        repackOrderMapper.deleteById(orderId);
    }

    // ==================== 列表/详情/预览/追溯 ====================

    public PageResult<RepackOrderVO> listRepackOrders(RepackOrderQuery query) {
        QueryWrapper<RepackOrder> w = new QueryWrapper<>();
        if (query.getOrderNo() != null && !query.getOrderNo().isEmpty()) w.like("order_no", query.getOrderNo());
        if (query.getStatus() != null) w.eq("status", query.getStatus());
        if (query.getRepackType() != null && !query.getRepackType().isEmpty()) w.eq("repack_type", query.getRepackType());
        w.orderByDesc("create_time");
        Page<RepackOrder> page = new Page<>(query.getCurrent() != null ? query.getCurrent() : 1, query.getSize() != null ? query.getSize() : 10);
        Page<RepackOrder> r = repackOrderMapper.selectPage(page, w);
        PageResult<RepackOrderVO> pr = new PageResult<>();
        pr.setRecords(r.getRecords().stream().map(this::buildVO).collect(Collectors.toList()));
        pr.setTotal(r.getTotal()); pr.setCurrent(r.getCurrent()); pr.setSize(r.getSize());
        return pr;
    }

    public RepackOrderVO getRepackDetail(Long id) { RepackOrder o = repackOrderMapper.selectById(id); if (o == null) throw new RuntimeException("转包单不存在"); return buildVO(o); }

    public Map<String, Object> previewRepack(String kanbanNo) {
        Kanban k = kanbanMapper.selectOne(new QueryWrapper<Kanban>().eq("kanban_no", kanbanNo));
        if (k == null) throw new RuntimeException("看板不存在: " + kanbanNo);
        Map<String, Object> r = new HashMap<>();
        r.put("kanbanNo", k.getKanbanNo()); r.put("partId", k.getPartId()); r.put("partCode", k.getPartCode()); r.put("partName", k.getPartName());
        r.put("quantity", k.getQuantity()); r.put("warehouseAreaId", k.getWarehouseAreaId()); r.put("warehouseAreaName", k.getWarehouseAreaName());
        r.put("supplierName", k.getSupplierName()); r.put("status", k.getStatus()); r.put("statusText", k.getStatusText());
        r.put("inboundOrderNo", k.getInboundOrderNo()); r.put("transferableQty", k.getQuantity());
        List<RepackRelation> asP = repackRelationMapper.selectList(new QueryWrapper<RepackRelation>().eq("parent_kanban_no", kanbanNo));
        List<RepackRelation> asC = repackRelationMapper.selectList(new QueryWrapper<RepackRelation>().eq("child_kanban_no", kanbanNo));
        r.put("hasTransferHistory", !asP.isEmpty() || !asC.isEmpty());
        r.put("transferOutCount", asP.size()); r.put("transferInCount", asC.size());
        return r;
    }

    public RepackRelationVO traceByKanban(String kanbanNo) {
        Kanban k = kanbanMapper.selectOne(new QueryWrapper<Kanban>().eq("kanban_no", kanbanNo));
        if (k == null) throw new RuntimeException("看板不存在: " + kanbanNo);
        RepackRelationVO vo = new RepackRelationVO();
        vo.setKanbanNo(kanbanNo);
        RepackRelationVO.KanbanInfo info = new RepackRelationVO.KanbanInfo();
        info.setKanbanNo(k.getKanbanNo()); info.setPartCode(k.getPartCode()); info.setPartName(k.getPartName());
        info.setQuantity(k.getQuantity()); info.setWarehouseAreaName(k.getWarehouseAreaName());
        info.setSupplierName(k.getSupplierName()); info.setStatusText(k.getStatusText());
        info.setInboundOrderNo(k.getInboundOrderNo()); info.setCreateTime(k.getCreateTime());
        vo.setCurrentKanban(info);

        // 向上追溯：找所有父看板（支持conolidate多父）
        List<RepackRelationVO.RepackRelationNode> parents = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(kanbanNo);
        collectParents(kanbanNo, 1, visited, parents);
        vo.setParentChain(parents);

        // 向下追溯：找所有子看板（支持breakdown多子）
        List<RepackRelationVO.RepackRelationNode> children = new ArrayList<>();
        visited.clear(); visited.add(kanbanNo);
        collectChildren(kanbanNo, 1, visited, children);
        vo.setChildChain(children);

        return vo;
    }

    private void collectParents(String childNo, int level, Set<String> visited,
                                 List<RepackRelationVO.RepackRelationNode> chain) {
        List<RepackRelation> rels = repackRelationMapper.selectList(
                new QueryWrapper<RepackRelation>().eq("child_kanban_no", childNo).orderByAsc("repack_time"));
        for (RepackRelation rel : rels) {
            if (!visited.add(rel.getParentKanbanNo())) continue;
            chain.add(makeNode(rel, level));
            collectParents(rel.getParentKanbanNo(), level + 1, visited, chain);
        }
    }

    private void collectChildren(String parentNo, int level, Set<String> visited,
                                  List<RepackRelationVO.RepackRelationNode> chain) {
        List<RepackRelation> rels = repackRelationMapper.selectList(
                new QueryWrapper<RepackRelation>().eq("parent_kanban_no", parentNo).orderByAsc("repack_time"));
        for (RepackRelation rel : rels) {
            if (!visited.add(rel.getChildKanbanNo())) continue;
            chain.add(makeNode(rel, level));
            collectChildren(rel.getChildKanbanNo(), level + 1, visited, chain);
        }
    }

    private RepackRelationVO.RepackRelationNode makeNode(RepackRelation rel, int lv) {
        RepackRelationVO.RepackRelationNode n = new RepackRelationVO.RepackRelationNode();
        n.setRelationId(rel.getId()); n.setParentKanbanNo(rel.getParentKanbanNo()); n.setChildKanbanNo(rel.getChildKanbanNo());
        n.setRepackOrderNo(rel.getRepackOrderNo()); n.setTransferQty(rel.getTransferQty());
        n.setPartCode(rel.getPartCode()); n.setPartName(rel.getPartName()); n.setRepackTime(rel.getRepackTime()); n.setLevel(lv);
        return n;
    }

    private RepackOrderVO buildVO(RepackOrder order) {
        RepackOrderVO vo = new RepackOrderVO();
        vo.setId(order.getId()); vo.setOrderNo(order.getOrderNo()); vo.setStatus(order.getStatus()); vo.setStatusText(order.getStatusText());
        vo.setRepackType(order.getRepackType()); vo.setRepackTypeText(order.getRepackTypeText());
        vo.setPartId(order.getPartId()); vo.setPartCode(order.getPartCode()); vo.setPartName(order.getPartName());
        vo.setWarehouseAreaId(order.getWarehouseAreaId()); vo.setWarehouseAreaName(order.getWarehouseAreaName());
        vo.setTargetBoxCapacity(order.getTargetBoxCapacity());
        vo.setOutboundOrderId(order.getOutboundOrderId()); vo.setOutboundOrderNo(order.getOutboundOrderNo());
        vo.setRemark(order.getRemark()); vo.setOperatorId(order.getOperatorId());
        vo.setCreateTime(order.getCreateTime()); vo.setUpdateTime(order.getUpdateTime());

        List<RepackOrderDetail> details = getDetails(order.getId());
        List<RepackDetailVO> dvs = new ArrayList<>();
        int totalQty = 0;
        for (RepackOrderDetail d : details) {
            RepackDetailVO dv = new RepackDetailVO();
            dv.setId(d.getId()); dv.setRepackOrderId(d.getRepackOrderId()); dv.setSourceKanbanId(d.getSourceKanbanId());
            dv.setSourceKanbanNo(d.getSourceKanbanNo()); dv.setTargetKanbanNo(d.getTargetKanbanNo());
            dv.setPartId(d.getPartId()); dv.setPartCode(d.getPartCode()); dv.setPartName(d.getPartName());
            dv.setTransferQty(d.getTransferQty()); dv.setRemainderQty(d.getRemainderQty()); dv.setSourceRemainingQty(d.getSourceRemainingQty());
            dv.setLineNo(d.getLineNo()); dv.setCreateTime(d.getCreateTime());
            dvs.add(dv); totalQty += d.getTransferQty() != null ? d.getTransferQty() : 0;
        }
        vo.setDetails(dvs); vo.setDetailCount(dvs.size()); vo.setTotalTransferQty(totalQty);
        return vo;
    }

    public Kanban getKanbanByNo(String kanbanNo) {
        return kanbanMapper.selectOne(new QueryWrapper<Kanban>().eq("kanban_no", kanbanNo));
    }
}
