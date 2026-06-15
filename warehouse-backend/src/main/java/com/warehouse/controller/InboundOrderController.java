package com.warehouse.controller;

import com.warehouse.model.dto.*;
import com.warehouse.service.InboundOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;

    public InboundOrderController(InboundOrderService inboundOrderService) {
        this.inboundOrderService = inboundOrderService;
    }

    @PostMapping("/inbound-order/list")
    public Result<PageResult<InboundOrderVO>> list(@RequestBody InboundOrderQuery query) {
        return Result.ok(inboundOrderService.list(query));
    }

    @PostMapping("/inbound-order/save")
    public Result<InboundOrderVO> save(@RequestBody InboundOrderSaveDTO dto) {
        return Result.ok(inboundOrderService.save(dto));
    }

    @PostMapping("/inbound-order/submit")
    public Result<InboundOrderVO> submit(@RequestBody Map<String, Long> body) {
        return Result.ok(inboundOrderService.submit(body.get("id")));
    }

    @PostMapping("/inbound-order/detail")
    public Result<InboundOrderVO> detail(@RequestBody Map<String, Long> body) {
        return Result.ok(inboundOrderService.getDetail(body.get("id")));
    }

    @PostMapping("/inbound-order/detail-by-no")
    public Result<InboundOrderVO> detailByNo(@RequestBody Map<String, String> body) {
        return Result.ok(inboundOrderService.getDetailByOrderNo(body.get("orderNo")));
    }

    @PostMapping("/inbound-order/delete")
    public Result<?> delete(@RequestBody Map<String, Long> body) {
        inboundOrderService.delete(body.get("id"));
        return Result.ok(null);
    }

    @PostMapping("/inbound-order/cancel")
    public Result<?> cancel(@RequestBody Map<String, Long> body) {
        inboundOrderService.cancel(body.get("id"));
        return Result.ok(null);
    }

    @PostMapping("/inbound-order/batch-copy-parts")
    public Result<?> batchCopyParts(@RequestBody BatchOperationDTO dto) {
        inboundOrderService.batchCopyParts(dto);
        return Result.ok(null);
    }

    @PostMapping("/inbound-order/batch-set-area")
    public Result<?> batchSetArea(@RequestBody BatchOperationDTO dto) {
        inboundOrderService.batchSetArea(dto);
        return Result.ok(null);
    }
}
