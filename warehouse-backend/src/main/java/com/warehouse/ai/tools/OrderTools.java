package com.warehouse.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.mapper.*;
import com.warehouse.model.entity.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Order query tools — inbound and outbound orders.
 */
@Component
public class OrderTools {

    private final ToolRegistry registry;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderDetailMapper inboundDetailMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderDetailMapper outboundDetailMapper;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public OrderTools(ToolRegistry registry, InboundOrderMapper inboundOrderMapper,
                      InboundOrderDetailMapper inboundDetailMapper,
                      OutboundOrderMapper outboundOrderMapper,
                      OutboundOrderDetailMapper outboundDetailMapper,
                      ObjectMapper objectMapper) {
        this.registry = registry;
        this.inboundOrderMapper = inboundOrderMapper;
        this.inboundDetailMapper = inboundDetailMapper;
        this.outboundOrderMapper = outboundOrderMapper;
        this.outboundDetailMapper = outboundDetailMapper;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void register() {
        registry.register("query_inbound_orders",
                "查询入库单列表。可按供应商名称、状态、日期范围筛选。返回入库单号、供应商、状态、创建时间等信息。",
                Map.of("type", "object",
                        "properties", Map.of(
                                "supplierName", Map.of("type", "string", "description", "供应商名称，可选"),
                                "status", Map.of("type", "integer", "description", "状态: 0=未入库 1=部分入库 2=已入库，可选"),
                                "startDate", Map.of("type", "string", "description", "开始日期 yyyy-MM-dd，可选"),
                                "endDate", Map.of("type", "string", "description", "结束日期 yyyy-MM-dd，可选"),
                                "limit", Map.of("type", "integer", "description", "返回数量限制，默认20")
                        )),
                this::queryInboundOrders);

        registry.register("query_outbound_orders",
                "查询出库单列表。可按客户名称、状态、日期范围筛选。返回出库单号、客户、状态、创建时间等信息。",
                Map.of("type", "object",
                        "properties", Map.of(
                                "customerName", Map.of("type", "string", "description", "客户名称，可选"),
                                "status", Map.of("type", "integer", "description", "状态: 0=未出库 1=部分出库 2=已出库 3=作废，可选"),
                                "startDate", Map.of("type", "string", "description", "开始日期 yyyy-MM-dd，可选"),
                                "endDate", Map.of("type", "string", "description", "结束日期 yyyy-MM-dd，可选"),
                                "limit", Map.of("type", "integer", "description", "返回数量限制，默认20")
                        )),
                this::queryOutboundOrders);

        registry.register("get_pending_orders",
                "获取所有待处理的入库单和出库单（状态为未完成或部分完成）。",
                Map.of("type", "object", "properties", Map.of()),
                args -> getPendingOrders());
    }

    private String statusText(int status, boolean isInbound) {
        if (isInbound) {
            return switch (status) {
                case 0 -> "未入库";
                case 1 -> "部分入库";
                case 2 -> "已入库";
                default -> "未知";
            };
        } else {
            return switch (status) {
                case 0 -> "未出库";
                case 1 -> "部分出库";
                case 2 -> "已出库";
                case 3 -> "作废";
                default -> "未知";
            };
        }
    }

    private String queryInboundOrders(String argsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argsJson, new TypeReference<>() {});
            QueryWrapper<InboundOrder> qw = new QueryWrapper<>();

            String supplierName = (String) args.get("supplierName");
            if (supplierName != null && !supplierName.isBlank()) {
                qw.like("supplier_name", supplierName);
            }
            if (args.get("status") != null) {
                qw.eq("status", ((Number) args.get("status")).intValue());
            }
            if (args.get("startDate") != null) {
                qw.ge("create_time", LocalDate.parse((String) args.get("startDate"), DATE_FMT).atStartOfDay());
            }
            if (args.get("endDate") != null) {
                qw.le("create_time", LocalDate.parse((String) args.get("endDate"), DATE_FMT).plusDays(1).atStartOfDay());
            }
            qw.orderByDesc("create_time");
            int limit = args.get("limit") != null ? ((Number) args.get("limit")).intValue() : 20;

            List<InboundOrder> orders = inboundOrderMapper.selectList(qw.last("LIMIT " + limit));
            List<Map<String, Object>> items = orders.stream().map(o -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("orderNo", o.getOrderNo());
                m.put("supplierName", o.getSupplierName());
                m.put("orderNumber", o.getOrderNumber());
                m.put("status", o.getStatus());
                m.put("statusText", statusText(o.getStatus(), true));
                m.put("createTime", o.getCreateTime() != null ? o.getCreateTime().toString() : null);
                return m;
            }).toList();

            return objectMapper.writeValueAsString(Map.of("count", items.size(), "items", items));
        } catch (Exception e) {
            return "{\"error\": \"查询入库单失败: " + e.getMessage() + "\"}";
        }
    }

    private String queryOutboundOrders(String argsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argsJson, new TypeReference<>() {});
            QueryWrapper<OutboundOrder> qw = new QueryWrapper<>();

            String customerName = (String) args.get("customerName");
            if (customerName != null && !customerName.isBlank()) {
                qw.like("customer_name", customerName);
            }
            if (args.get("status") != null) {
                qw.eq("status", ((Number) args.get("status")).intValue());
            }
            if (args.get("startDate") != null) {
                qw.ge("create_time", LocalDate.parse((String) args.get("startDate"), DATE_FMT).atStartOfDay());
            }
            if (args.get("endDate") != null) {
                qw.le("create_time", LocalDate.parse((String) args.get("endDate"), DATE_FMT).plusDays(1).atStartOfDay());
            }
            qw.orderByDesc("create_time");
            int limit = args.get("limit") != null ? ((Number) args.get("limit")).intValue() : 20;

            List<OutboundOrder> orders = outboundOrderMapper.selectList(qw.last("LIMIT " + limit));
            List<Map<String, Object>> items = orders.stream().map(o -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("orderNo", o.getOrderNo());
                m.put("customerName", o.getCustomerName());
                m.put("status", o.getStatus());
                m.put("statusText", statusText(o.getStatus(), false));
                m.put("remark", o.getRemark());
                m.put("createTime", o.getCreateTime() != null ? o.getCreateTime().toString() : null);
                return m;
            }).toList();

            return objectMapper.writeValueAsString(Map.of("count", items.size(), "items", items));
        } catch (Exception e) {
            return "{\"error\": \"查询出库单失败: " + e.getMessage() + "\"}";
        }
    }

    private String getPendingOrders() {
        try {
            QueryWrapper<InboundOrder> iqw = new QueryWrapper<>();
            iqw.in("status", 0, 1).orderByDesc("create_time");
            List<InboundOrder> pendingIn = inboundOrderMapper.selectList(iqw);

            QueryWrapper<OutboundOrder> oqw = new QueryWrapper<>();
            oqw.in("status", 0, 1).orderByDesc("create_time");
            List<OutboundOrder> pendingOut = outboundOrderMapper.selectList(oqw);

            return objectMapper.writeValueAsString(Map.of(
                    "pendingInboundCount", pendingIn.size(),
                    "pendingInbound", pendingIn.stream().map(o -> Map.of(
                            "orderNo", o.getOrderNo(),
                            "supplierName", o.getSupplierName(),
                            "statusText", statusText(o.getStatus(), true),
                            "createTime", o.getCreateTime() != null ? o.getCreateTime().toString() : null
                    )).toList(),
                    "pendingOutboundCount", pendingOut.size(),
                    "pendingOutbound", pendingOut.stream().map(o -> Map.of(
                            "orderNo", o.getOrderNo(),
                            "customerName", o.getCustomerName(),
                            "statusText", statusText(o.getStatus(), false),
                            "createTime", o.getCreateTime() != null ? o.getCreateTime().toString() : null
                    )).toList()
            ));
        } catch (Exception e) {
            return "{\"error\": \"查询待处理订单失败: " + e.getMessage() + "\"}";
        }
    }
}
