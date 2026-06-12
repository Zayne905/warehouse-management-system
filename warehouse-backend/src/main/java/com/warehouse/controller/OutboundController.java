package com.warehouse.controller;

import com.warehouse.model.dto.PageResult;
import com.warehouse.model.dto.Result;
import com.warehouse.service.OutboundService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class OutboundController {

    private final OutboundService outboundService;

    public OutboundController(OutboundService outboundService) {
        this.outboundService = outboundService;
    }

    @PostMapping("/outbound-order/list")
    public Result<PageResult<Map<String, Object>>> list(@RequestBody Map<String, Object> body) {
        int current = body.get("current") != null ? Integer.parseInt(body.get("current").toString()) : 1;
        int size = body.get("size") != null ? Integer.parseInt(body.get("size").toString()) : 10;
        String orderNo = (String) body.get("orderNo");
        Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : null;
        return Result.ok(outboundService.list(current, size, orderNo, status));
    }

    @PostMapping("/outbound-order/detail")
    public Result<Map<String, Object>> detail(@RequestBody Map<String, Long> body) {
        return Result.ok(outboundService.getDetail(body.get("id")));
    }

    @PostMapping("/outbound-order/save")
    public Result<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        return Result.ok(outboundService.save(body));
    }

    @PostMapping("/outbound-order/delete")
    public Result<?> delete(@RequestBody Map<String, Long> body) {
        outboundService.delete(body.get("id"));
        return Result.ok(null);
    }

    @PostMapping("/outbound-order/cancel")
    public Result<?> cancel(@RequestBody Map<String, Long> body) {
        outboundService.cancel(body.get("id"));
        return Result.ok(null);
    }

    /**
     * 扫码出库（FIFO自动选最早入库的看板）
     */
    @PostMapping("/outbound/scan")
    public Result<Map<String, Object>> scanOutbound(@RequestBody Map<String, Object> body) {
        Long orderId = body.get("orderId") != null
                ? Long.valueOf(body.get("orderId").toString()) : null;
        String kanbanNo = (String) body.get("kanbanNo");
        Integer operatorId = body.get("operatorId") != null
                ? Integer.parseInt(body.get("operatorId").toString()) : null;
        return Result.ok(outboundService.scanOutbound(orderId, kanbanNo, operatorId));
    }

    /**
     * 查询零件可用库存
     */
    @PostMapping("/outbound/available-stock")
    public Result<?> availableStock(@RequestBody Map<String, Long> body) {
        return Result.ok(outboundService.getAvailableStock(body.get("partId")));
    }
}
