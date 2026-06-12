package com.warehouse.controller;

import com.warehouse.model.dto.KanbanScanDTO;
import com.warehouse.model.dto.Result;
import com.warehouse.model.dto.ScanDuplicateCheckDTO;
import com.warehouse.model.dto.ScanSubmitDTO;
import com.warehouse.model.entity.ScanRecord;
import com.warehouse.service.ScanService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping("/scan/check-duplicate")
    public Result<Map<String, Object>> checkDuplicate(@RequestBody ScanDuplicateCheckDTO dto) {
        return Result.ok(scanService.checkDuplicate(dto));
    }

    @PostMapping("/scan/submit")
    public Result<Map<String, Object>> submit(@RequestBody ScanSubmitDTO dto) {
        return Result.ok(scanService.submitScan(dto));
    }

    @PostMapping("/scan/list")
    public Result<List<ScanRecord>> list(@RequestBody Map<String, Long> body) {
        return Result.ok(scanService.listScans(body.get("inboundOrderId")));
    }

    @PostMapping("/scan/delete")
    public Result<?> delete(@RequestBody Map<String, Long> body) {
        scanService.deleteScan(body.get("scanRecordId"));
        return Result.ok(null);
    }

    @PostMapping("/scan/feedback")
    public Result<Map<String, Object>> feedback(@RequestBody Map<String, String> body) {
        return Result.ok(scanService.getFeedback(
                body.get("orderNo"),
                body.get("partCode")));
    }

    /**
     * 看板扫码入库 — 扫看板QR码自动收货一箱
     */
    @PostMapping("/scan/kanban")
    public Result<Map<String, Object>> scanKanban(@RequestBody KanbanScanDTO dto) {
        return Result.ok(scanService.scanKanban(dto));
    }
}
