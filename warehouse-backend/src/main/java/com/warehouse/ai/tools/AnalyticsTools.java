package com.warehouse.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.service.AnalyticsService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Analytics and reporting tools.
 */
@Component
public class AnalyticsTools {

    private final ToolRegistry registry;
    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    public AnalyticsTools(ToolRegistry registry, AnalyticsService analyticsService,
                          ObjectMapper objectMapper) {
        this.registry = registry;
        this.analyticsService = analyticsService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void register() {
        registry.register("get_today_summary",
                "获取今日运营摘要。返回今日入库笔数/数量、出库笔数/数量、当前库存总量等关键指标。",
                Map.of("type", "object", "properties", Map.of()),
                args -> {
                    try {
                        return objectMapper.writeValueAsString(analyticsService.getTodaySummary());
                    } catch (Exception e) {
                        return "{\"error\": \"获取今日摘要失败: " + e.getMessage() + "\"}";
                    }
                });

        registry.register("get_recent_trend",
                "获取最近N天的出入库趋势数据。返回每天的入库数量和出库数量。",
                Map.of("type", "object",
                        "properties", Map.of(
                                "days", Map.of("type", "integer", "description", "天数，默认7天，最大90天")
                        )),
                args -> {
                    try {
                        Map<String, Object> params = objectMapper.readValue(args, Map.class);
                        int days = params.get("days") != null
                                ? ((Number) params.get("days")).intValue() : 7;
                        return objectMapper.writeValueAsString(analyticsService.getRecentTrend(days));
                    } catch (Exception e) {
                        return "{\"error\": \"获取趋势数据失败: " + e.getMessage() + "\"}";
                    }
                });
    }
}
