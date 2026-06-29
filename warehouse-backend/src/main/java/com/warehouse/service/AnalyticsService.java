package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.*;
import com.warehouse.model.entity.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics service for dashboards, trends, and reporting.
 */
@Service
public class AnalyticsService {

    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderDetailMapper inboundDetailMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderDetailMapper outboundDetailMapper;
    private final OutboundScanMapper outboundScanMapper;
    private final ScanRecordMapper scanRecordMapper;
    private final KanbanMapper kanbanMapper;
    private final PartMapper partMapper;

    public AnalyticsService(InboundOrderMapper inboundOrderMapper,
                            InboundOrderDetailMapper inboundDetailMapper,
                            OutboundOrderMapper outboundOrderMapper,
                            OutboundOrderDetailMapper outboundDetailMapper,
                            OutboundScanMapper outboundScanMapper,
                            ScanRecordMapper scanRecordMapper,
                            KanbanMapper kanbanMapper,
                            PartMapper partMapper) {
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundDetailMapper = inboundDetailMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.outboundDetailMapper = outboundDetailMapper;
        this.outboundScanMapper = outboundScanMapper;
        this.scanRecordMapper = scanRecordMapper;
        this.kanbanMapper = kanbanMapper;
        this.partMapper = partMapper;
    }

    /**
     * Get today's operational summary.
     */
    public Map<String, Object> getTodaySummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        // Today's inbound
        QueryWrapper<InboundOrder> inboundQw = new QueryWrapper<>();
        inboundQw.between("create_time", todayStart, todayEnd);
        List<InboundOrder> todayInbound = inboundOrderMapper.selectList(inboundQw);

        long inboundCount = todayInbound.size();
        long inboundCompleted = todayInbound.stream().filter(o -> o.getStatus() == 2).count();
        long inboundPending = todayInbound.stream().filter(o -> o.getStatus() == 0 || o.getStatus() == 1).count();

        // Today's inbound scan quantity
        QueryWrapper<ScanRecord> scanQw = new QueryWrapper<>();
        scanQw.between("scan_time", todayStart, todayEnd);
        List<ScanRecord> todayScans = scanRecordMapper.selectList(scanQw);
        BigDecimal inboundQty = todayScans.stream()
                .map(s -> s.getScanQty() != null ? s.getScanQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Today's outbound
        QueryWrapper<OutboundOrder> outboundQw = new QueryWrapper<>();
        outboundQw.between("create_time", todayStart, todayEnd);
        List<OutboundOrder> todayOutbound = outboundOrderMapper.selectList(outboundQw);

        long outboundCount = todayOutbound.size();
        long outboundCompleted = todayOutbound.stream().filter(o -> o.getStatus() == 2).count();

        // Today's outbound scan quantity
        QueryWrapper<OutboundScan> outScanQw = new QueryWrapper<>();
        outScanQw.between("scan_time", todayStart, todayEnd);
        List<OutboundScan> todayOutScans = outboundScanMapper.selectList(outScanQw);
        BigDecimal outboundQty = todayOutScans.stream()
                .map(s -> s.getQuantity() != null ? s.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Current active inventory (available kanbans)
        QueryWrapper<Kanban> inventoryQw = new QueryWrapper<>();
        inventoryQw.in("status", Kanban.STATUS_AVAILABLE, Kanban.STATUS_BLOCKED, Kanban.STATUS_PARTIAL_REPACK);
        List<Kanban> activeKanbans = kanbanMapper.selectList(inventoryQw);
        long totalBoxCount = activeKanbans.size();
        BigDecimal totalStock = activeKanbans.stream()
                .map(k -> k.getQuantity() != null ? k.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("date", LocalDate.now().toString());
        summary.put("inboundOrderCount", inboundCount);
        summary.put("inboundCompletedCount", inboundCompleted);
        summary.put("inboundPendingCount", inboundPending);
        summary.put("inboundQuantity", inboundQty);
        summary.put("outboundOrderCount", outboundCount);
        summary.put("outboundCompletedCount", outboundCompleted);
        summary.put("outboundQuantity", outboundQty);
        summary.put("totalBoxCount", totalBoxCount);
        summary.put("totalStock", totalStock);
        return summary;
    }

    /**
     * Get recent N days inbound/outbound trend data.
     */
    public Map<String, Object> getRecentTrend(int days) {
        days = Math.min(days, 90); // cap at 90 days
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<Map<String, Object>> trend = new ArrayList<>();

        // Query all inbound orders in range
        QueryWrapper<InboundOrder> inboundQw = new QueryWrapper<>();
        inboundQw.between("create_time", startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        Map<LocalDate, Long> inboundByDay = inboundOrderMapper.selectList(inboundQw).stream()
                .collect(Collectors.groupingBy(
                        o -> o.getCreateTime().toLocalDate(),
                        Collectors.counting()));

        // Query all outbound orders in range
        QueryWrapper<OutboundOrder> outboundQw = new QueryWrapper<>();
        outboundQw.between("create_time", startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        Map<LocalDate, Long> outboundByDay = outboundOrderMapper.selectList(outboundQw).stream()
                .collect(Collectors.groupingBy(
                        o -> o.getCreateTime().toLocalDate(),
                        Collectors.counting()));

        // Query inbound scan quantities by day
        QueryWrapper<ScanRecord> scanQw = new QueryWrapper<>();
        scanQw.between("scan_time", startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        Map<LocalDate, BigDecimal> inboundQtyByDay = scanRecordMapper.selectList(scanQw).stream()
                .collect(Collectors.groupingBy(
                        s -> s.getScanTime() != null ? s.getScanTime().toLocalDate() : s.getCreateTime().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO,
                                s -> s.getScanQty() != null ? s.getScanQty() : BigDecimal.ZERO,
                                BigDecimal::add)));

        // Query outbound scan quantities by day
        QueryWrapper<OutboundScan> outScanQw = new QueryWrapper<>();
        outScanQw.between("scan_time", startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        Map<LocalDate, BigDecimal> outboundQtyByDay = outboundScanMapper.selectList(outScanQw).stream()
                .collect(Collectors.groupingBy(
                        s -> s.getScanTime() != null ? s.getScanTime().toLocalDate() : s.getCreateTime().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO,
                                s -> s.getQuantity() != null ? s.getQuantity() : BigDecimal.ZERO,
                                BigDecimal::add)));

        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", d.toString());
            dayData.put("inboundOrders", inboundByDay.getOrDefault(d, 0L));
            dayData.put("outboundOrders", outboundByDay.getOrDefault(d, 0L));
            dayData.put("inboundQuantity", inboundQtyByDay.getOrDefault(d, BigDecimal.ZERO));
            dayData.put("outboundQuantity", outboundQtyByDay.getOrDefault(d, BigDecimal.ZERO));
            trend.add(dayData);
        }

        return Map.of("days", days, "trend", trend);
    }

    /**
     * Get KPI data for the dashboard cards.
     */
    public Map<String, Object> getKpiData() {
        Map<String, Object> todaySummary = getTodaySummary();

        // Month-to-date totals
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        QueryWrapper<InboundOrder> monthInQw = new QueryWrapper<>();
        monthInQw.ge("create_time", monthStart);
        long monthInboundCount = inboundOrderMapper.selectCount(monthInQw);

        QueryWrapper<OutboundOrder> monthOutQw = new QueryWrapper<>();
        monthOutQw.ge("create_time", monthStart);
        long monthOutboundCount = outboundOrderMapper.selectCount(monthOutQw);

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("todayInbound", todaySummary.get("inboundOrderCount"));
        kpi.put("todayOutbound", todaySummary.get("outboundOrderCount"));
        kpi.put("todayInboundQty", todaySummary.get("inboundQuantity"));
        kpi.put("todayOutboundQty", todaySummary.get("outboundQuantity"));
        kpi.put("monthInbound", monthInboundCount);
        kpi.put("monthOutbound", monthOutboundCount);
        kpi.put("totalStock", todaySummary.get("totalStock"));
        kpi.put("totalBoxCount", todaySummary.get("totalBoxCount"));
        kpi.put("pendingInbound", todaySummary.get("inboundPendingCount"));

        // High/Low stock threshold computation
        computeThresholdAlerts(kpi);

        return kpi;
    }

    /**
     * Compute high/low stock alert counts and details from per-part thresholds.
     */
    private void computeThresholdAlerts(Map<String, Object> kpi) {
        // 1. Load all parts that have thresholds configured
        List<Part> allParts = partMapper.selectList(null);
        Map<Long, Part> partMap = new HashMap<>();
        for (Part p : allParts) {
            partMap.put(p.getId(), p);
        }

        // 2. Aggregate current stock per part from active kanbans
        QueryWrapper<Kanban> inventoryQw = new QueryWrapper<>();
        inventoryQw.in("status", Kanban.STATUS_AVAILABLE, Kanban.STATUS_BLOCKED, Kanban.STATUS_PARTIAL_REPACK);
        List<Kanban> activeKanbans = kanbanMapper.selectList(inventoryQw);

        Map<Long, Integer> partStock = new HashMap<>();
        for (Kanban k : activeKanbans) {
            Long pid = k.getPartId();
            int qty = k.getQuantity() != null ? k.getQuantity().intValue() : 0;
            partStock.merge(pid, qty, Integer::sum);
        }

        // 3. Compare stock against thresholds
        int lowStockCount = 0;
        int highStockCount = 0;
        List<Map<String, Object>> lowStockDetails = new ArrayList<>();
        List<Map<String, Object>> highStockDetails = new ArrayList<>();

        for (Part part : allParts) {
            int minStock = part.getMinStock() != null ? part.getMinStock() : 0;
            int maxStock = part.getMaxStock() != null ? part.getMaxStock() : 0;
            int stock = partStock.getOrDefault(part.getId(), 0);

            if (minStock > 0 && stock <= minStock) {
                lowStockCount++;
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("partId", part.getId());
                detail.put("partCode", part.getCode());
                detail.put("partName", part.getName());
                detail.put("currentStock", stock);
                detail.put("threshold", minStock);
                lowStockDetails.add(detail);
            }

            if (maxStock > 0 && stock >= maxStock) {
                highStockCount++;
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("partId", part.getId());
                detail.put("partCode", part.getCode());
                detail.put("partName", part.getName());
                detail.put("currentStock", stock);
                detail.put("threshold", maxStock);
                highStockDetails.add(detail);
            }
        }

        kpi.put("lowStockCount", lowStockCount);
        kpi.put("highStockCount", highStockCount);
        kpi.put("lowStockDetails", lowStockDetails);
        kpi.put("highStockDetails", highStockDetails);
    }
}
