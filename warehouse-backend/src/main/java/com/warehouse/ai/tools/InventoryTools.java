package com.warehouse.ai.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.mapper.*;
import com.warehouse.model.dto.InventoryVO;
import com.warehouse.model.entity.*;
import com.warehouse.service.AnalyticsService;
import com.warehouse.service.InventoryService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Inventory and stock query tools — including threshold-based alerts.
 */
@Component
public class InventoryTools {

    private final ToolRegistry registry;
    private final InventoryService inventoryService;
    private final AnalyticsService analyticsService;
    private final KanbanMapper kanbanMapper;
    private final PartMapper partMapper;
    private final ObjectMapper objectMapper;

    public InventoryTools(ToolRegistry registry, InventoryService inventoryService,
                          AnalyticsService analyticsService,
                          KanbanMapper kanbanMapper, PartMapper partMapper,
                          ObjectMapper objectMapper) {
        this.registry = registry;
        this.inventoryService = inventoryService;
        this.analyticsService = analyticsService;
        this.kanbanMapper = kanbanMapper;
        this.partMapper = partMapper;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void register() {
        // 1. query_inventory - 查询库存
        registry.register("query_inventory", "查询当前库存状况。可按零件名称/编码关键字搜索，也可按库区筛选。返回每个零件的库存数量、箱数、库区分布。",
                Map.of("type", "object",
                        "properties", Map.of(
                                "keyword", Map.of("type", "string", "description", "零件名称或编码关键字，可选"),
                                "warehouseAreaId", Map.of("type", "integer", "description", "库区ID，可选")
                        )),
                this::queryInventory);

        // 2. get_low_stock_alerts - 低库存预警（基于每物料配置的最低储备阈值）
        registry.register("get_low_stock_alerts",
                "获取低储预警列表。返回库存数量低于该物料「最低储备」阈值的零件清单。使用每物料自身配置的minStock阈值，而非固定值。",
                Map.of("type", "object", "properties", Map.of()),
                this::getLowStockAlerts);

        // 3. get_high_stock_alerts - 高库存预警（基于每物料配置的最高储备阈值）
        registry.register("get_high_stock_alerts",
                "获取高储预警列表。返回库存数量高于该物料「最高储备」阈值的零件清单。使用每物料自身配置的maxStock阈值。",
                Map.of("type", "object", "properties", Map.of()),
                this::getHighStockAlerts);

        // 4. get_inventory_alerts - 综合预警（同时返回低储和高储）
        registry.register("get_inventory_alerts",
                "获取全部库存预警摘要。同时返回低储预警和高储预警的零件数量和明细列表。当用户问「库存预警」「哪些零件需要补货」「库存是否正常」时优先使用此工具。",
                Map.of("type", "object", "properties", Map.of()),
                this::getInventoryAlerts);

        // 5. get_inventory_distribution - 库区库存分布
        registry.register("get_inventory_distribution", "获取各库区的库存分布概览，显示每个库区的零件种类数和库存总量。",
                Map.of("type", "object", "properties", Map.of()),
                args -> getInventoryDistribution());
    }

    private String queryInventory(String argsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argsJson, new TypeReference<>() {});
            String keyword = (String) args.get("keyword");
            Long areaId = args.get("warehouseAreaId") != null
                    ? ((Number) args.get("warehouseAreaId")).longValue() : null;

            List<InventoryVO> stock = inventoryService.listStock(keyword, areaId);
            return objectMapper.writeValueAsString(Map.of(
                    "count", stock.size(),
                    "items", stock
            ));
        } catch (Exception e) {
            return "{\"error\": \"查询库存失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 低储预警 — 使用每物料配置的 minStock 阈值。
     * 当前库存 ≤ minStock 且 minStock > 0 时触发。
     */
    private String getLowStockAlerts(String argsJson) {
        try {
            Map<String, Object> kpi = analyticsService.getKpiData();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> details = (List<Map<String, Object>>) kpi.get("lowStockDetails");
            int count = ((Number) kpi.get("lowStockCount")).intValue();

            return objectMapper.writeValueAsString(Map.of(
                    "type", "低储预警",
                    "description", "库存数量低于该物料最低储备阈值的零件",
                    "alertCount", count,
                    "items", details != null ? details : List.of()
            ));
        } catch (Exception e) {
            return "{\"error\": \"查询低储预警失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 高储预警 — 使用每物料配置的 maxStock 阈值。
     * 当前库存 ≥ maxStock 且 maxStock > 0 时触发。
     */
    private String getHighStockAlerts(String argsJson) {
        try {
            Map<String, Object> kpi = analyticsService.getKpiData();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> details = (List<Map<String, Object>>) kpi.get("highStockDetails");
            int count = ((Number) kpi.get("highStockCount")).intValue();

            return objectMapper.writeValueAsString(Map.of(
                    "type", "高储预警",
                    "description", "库存数量高于该物料最高储备阈值的零件",
                    "alertCount", count,
                    "items", details != null ? details : List.of()
            ));
        } catch (Exception e) {
            return "{\"error\": \"查询高储预警失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 综合预警 — 一次返回低储和高储的全部信息。
     */
    private String getInventoryAlerts(String argsJson) {
        try {
            Map<String, Object> kpi = analyticsService.getKpiData();
            int lowCount = ((Number) kpi.get("lowStockCount")).intValue();
            int highCount = ((Number) kpi.get("highStockCount")).intValue();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lowDetails = (List<Map<String, Object>>) kpi.get("lowStockDetails");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> highDetails = (List<Map<String, Object>>) kpi.get("highStockDetails");

            return objectMapper.writeValueAsString(Map.of(
                    "summary", lowCount == 0 && highCount == 0
                            ? "✅ 所有零件库存正常，无高低储预警"
                            : String.format("⚠️ 低储预警 %d 个，高储预警 %d 个", lowCount, highCount),
                    "lowStockCount", lowCount,
                    "highStockCount", highCount,
                    "lowStockDetails", lowDetails != null ? lowDetails : List.of(),
                    "highStockDetails", highDetails != null ? highDetails : List.of()
            ));
        } catch (Exception e) {
            return "{\"error\": \"查询库存预警失败: " + e.getMessage() + "\"}";
        }
    }

    private String getInventoryDistribution() {
        try {
            var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Kanban>()
                    .in("status", Kanban.STATUS_AVAILABLE, Kanban.STATUS_BLOCKED, Kanban.STATUS_PARTIAL_REPACK);
            List<Kanban> kanbans = kanbanMapper.selectList(wrapper);

            Map<Long, Long> areaPartCount = new HashMap<>();
            Map<Long, java.math.BigDecimal> areaQty = new HashMap<>();
            Map<Long, String> areaNames = new HashMap<>();

            for (Kanban k : kanbans) {
                Long areaId = k.getWarehouseAreaId();
                if (areaId == null) continue;
                areaPartCount.merge(areaId, 1L, Long::sum);
                areaQty.merge(areaId, k.getQuantity() != null ? k.getQuantity() : java.math.BigDecimal.ZERO,
                        java.math.BigDecimal::add);
                areaNames.putIfAbsent(areaId, k.getWarehouseAreaName());
            }

            List<Map<String, Object>> distribution = areaPartCount.entrySet().stream()
                    .map(e -> Map.<String, Object>of(
                            "warehouseAreaId", e.getKey(),
                            "warehouseAreaName", areaNames.getOrDefault(e.getKey(), "未知"),
                            "partTypeCount", e.getValue(),
                            "totalQuantity", areaQty.getOrDefault(e.getKey(), java.math.BigDecimal.ZERO)
                    ))
                    .toList();

            return objectMapper.writeValueAsString(Map.of(
                    "areas", distribution
            ));
        } catch (Exception e) {
            return "{\"error\": \"查询库区分布失败: " + e.getMessage() + "\"}";
        }
    }
}
