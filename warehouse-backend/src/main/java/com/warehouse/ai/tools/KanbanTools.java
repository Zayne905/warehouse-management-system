package com.warehouse.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.mapper.KanbanMapper;
import com.warehouse.mapper.PartMapper;
import com.warehouse.mapper.SupplierMapper;
import com.warehouse.model.entity.Kanban;
import com.warehouse.model.entity.Part;
import com.warehouse.model.entity.Supplier;
import com.warehouse.service.KanbanService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Kanban, part, and supplier search tools.
 */
@Component
public class KanbanTools {

    private final ToolRegistry registry;
    private final KanbanMapper kanbanMapper;
    private final KanbanService kanbanService;
    private final PartMapper partMapper;
    private final SupplierMapper supplierMapper;
    private final ObjectMapper objectMapper;

    public KanbanTools(ToolRegistry registry, KanbanMapper kanbanMapper,
                       KanbanService kanbanService, PartMapper partMapper,
                       SupplierMapper supplierMapper, ObjectMapper objectMapper) {
        this.registry = registry;
        this.kanbanMapper = kanbanMapper;
        this.kanbanService = kanbanService;
        this.partMapper = partMapper;
        this.supplierMapper = supplierMapper;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void register() {
        registry.register("query_kanban_detail",
                "查询单个看板的完整信息和生命周期。输入看板号，返回看板当前状态、数量、所属零件、库区以及完整的生命周期事件（创建→入库→转包→出库）。",
                Map.of("type", "object",
                        "properties", Map.of(
                                "kanbanNo", Map.of("type", "string", "description", "看板号")
                        ),
                        "required", List.of("kanbanNo")),
                this::queryKanbanDetail);

        registry.register("search_part",
                "按名称或编码搜索零件信息。返回零件编码、名称、规格、单位、包装容量等。",
                Map.of("type", "object",
                        "properties", Map.of(
                                "keyword", Map.of("type", "string", "description", "零件名称或编码关键字")
                        ),
                        "required", List.of("keyword")),
                this::searchPart);

        registry.register("search_supplier",
                "搜索供应商信息。返回供应商编码、名称、联系人、电话、地址等。",
                Map.of("type", "object",
                        "properties", Map.of(
                                "keyword", Map.of("type", "string", "description", "供应商名称或编码关键字")
                        ),
                        "required", List.of("keyword")),
                this::searchSupplier);
    }

    private String queryKanbanDetail(String argsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            String kanbanNo = (String) args.get("kanbanNo");

            QueryWrapper<Kanban> qw = new QueryWrapper<>();
            qw.eq("kanban_no", kanbanNo);
            Kanban kanban = kanbanMapper.selectOne(qw);

            if (kanban == null) {
                return "{\"error\": \"未找到看板: " + kanbanNo + "\"}";
            }

            String statusText = switch (kanban.getStatus()) {
                case 0 -> "待入库";
                case 1 -> "在库可用";
                case 2 -> "待出库(锁定)";
                case 3 -> "已出库";
                case 4 -> "封存";
                case 5 -> "部分转出";
                case 6 -> "已清空";
                default -> "未知";
            };

            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("kanbanNo", kanban.getKanbanNo());
            result.put("partCode", kanban.getPartCode());
            result.put("partName", kanban.getPartName());
            result.put("supplierName", kanban.getSupplierName());
            result.put("quantity", kanban.getQuantity());
            result.put("originalQty", kanban.getOriginalQty());
            result.put("boxSeq", kanban.getBoxSeq());
            result.put("warehouseAreaName", kanban.getWarehouseAreaName());
            result.put("status", kanban.getStatus());
            result.put("statusText", statusText);
            result.put("inboundOrderNo", kanban.getInboundOrderNo());
            result.put("outboundOrderNo", kanban.getOutboundOrderNo());
            result.put("createTime", kanban.getCreateTime() != null ? kanban.getCreateTime().toString() : null);

            // Try to get lifecycle
            try {
                var lifecycle = kanbanService.getLifecycle(kanban.getKanbanNo());
                result.put("lifecycle", lifecycle);
            } catch (Exception ignored) {
                // lifecycle not available for this kanban
            }

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\": \"查询看板失败: " + e.getMessage() + "\"}";
        }
    }

    private String searchPart(String argsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            String keyword = (String) args.get("keyword");

            QueryWrapper<Part> qw = new QueryWrapper<>();
            qw.like("code", keyword).or().like("name", keyword);
            List<Part> parts = partMapper.selectList(qw);

            List<Map<String, Object>> items = parts.stream().map(p -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("code", p.getCode());
                m.put("name", p.getName());
                m.put("spec", p.getSpec());
                m.put("unit", p.getUnit());
                m.put("packageCapacity", p.getPackageCapacity());
                return m;
            }).toList();

            return objectMapper.writeValueAsString(Map.of("count", items.size(), "items", items));
        } catch (Exception e) {
            return "{\"error\": \"搜索零件失败: " + e.getMessage() + "\"}";
        }
    }

    private String searchSupplier(String argsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            String keyword = (String) args.get("keyword");

            QueryWrapper<Supplier> qw = new QueryWrapper<>();
            qw.like("code", keyword).or().like("name", keyword);
            List<Supplier> suppliers = supplierMapper.selectList(qw);

            List<Map<String, Object>> items = suppliers.stream().map(s -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("code", s.getCode());
                m.put("name", s.getName());
                m.put("contact", s.getContact());
                m.put("phone", s.getPhone());
                m.put("address", s.getAddress());
                return m;
            }).toList();

            return objectMapper.writeValueAsString(Map.of("count", items.size(), "items", items));
        } catch (Exception e) {
            return "{\"error\": \"搜索供应商失败: " + e.getMessage() + "\"}";
        }
    }
}
