package com.warehouse.ai.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.mapper.*;
import com.warehouse.model.dto.InventoryVO;
import com.warehouse.model.entity.*;
import com.warehouse.service.InventoryService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Inventory and stock query tools.
 */
@Component
public class InventoryTools {

    private final ToolRegistry registry;
    private final InventoryService inventoryService;
    private final KanbanMapper kanbanMapper;
    private final PartMapper partMapper;
    private final ObjectMapper objectMapper;

    public InventoryTools(ToolRegistry registry, InventoryService inventoryService,
                          KanbanMapper kanbanMapper, PartMapper partMapper,
                          ObjectMapper objectMapper) {
        this.registry = registry;
        this.inventoryService = inventoryService;
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

        // 2. get_low_stock_alerts - 低库存预警
        registry.register("get_low_stock_alerts", "获取低库存预警列表。返回当前库存数量低于阈值的零件清单。",
                Map.of("type", "object",
                        "properties", Map.of(
                                "threshold", Map.of("type", "integer", "description", "库存预警阈值，默认10，不传则使用默认值")
                        )),
                this::getLowStockAlerts);

        // 3. get_inventory_distribution - 库区库存分布
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

    private String getLowStockAlerts(String argsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argsJson, new TypeReference<>() {});
            int threshold = args.get("threshold") != null
                    ? ((Number) args.get("threshold")).intValue() : 10;

            List<InventoryVO> all = inventoryService.listStock(null, null);
            List<InventoryVO> lowStock = all.stream()
                    .filter(v -> v.getTotalStock().intValue() < threshold)
                    .toList();

            return objectMapper.writeValueAsString(Map.of(
                    "threshold", threshold,
                    "alertCount", lowStock.size(),
                    "items", lowStock
            ));
        } catch (Exception e) {
            return "{\"error\": \"查询低库存预警失败: " + e.getMessage() + "\"}";
        }
    }

    private String getInventoryDistribution() {
        try {
            // Group kanbans by warehouse area
            var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Kanban>()
                    .in("status", Kanban.STATUS_AVAILABLE, Kanban.STATUS_BLOCKED, Kanban.STATUS_PARTIAL_REPACK);
            List<Kanban> kanbans = kanbanMapper.selectList(wrapper);

            Map<Long, Long> areaPartCount = new java.util.HashMap<>();
            Map<Long, java.math.BigDecimal> areaQty = new java.util.HashMap<>();
            Map<Long, String> areaNames = new java.util.HashMap<>();

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
