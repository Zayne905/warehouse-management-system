package com.warehouse.controller;

import com.warehouse.model.dto.Result;
import com.warehouse.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Dashboard KPI data.
     */
    @GetMapping("/kpi")
    public Result<Map<String, Object>> kpi() {
        return Result.ok(analyticsService.getKpiData());
    }

    /**
     * Trend data for charts. Query param: days (default 7).
     */
    @GetMapping("/trend")
    public Result<Map<String, Object>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(analyticsService.getRecentTrend(days));
    }

    /**
     * Today's operational summary.
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(analyticsService.getTodaySummary());
    }
}
