package com.warehouse.controller;

import com.warehouse.model.dto.Result;
import com.warehouse.model.entity.Kanban;
import com.warehouse.service.KanbanService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class KanbanController {

    private final KanbanService kanbanService;

    public KanbanController(KanbanService kanbanService) {
        this.kanbanService = kanbanService;
    }

    @PostMapping("/kanban/list-by-order")
    public Result<List<Kanban>> listByOrder(@RequestBody Map<String, Long> body) {
        return Result.ok(kanbanService.listByOrderId(body.get("orderId")));
    }

    /** 查询某零件的所有看板（库存明细） */
    @PostMapping("/kanban/list-by-part")
    public Result<List<Kanban>> listByPart(@RequestBody Map<String, Long> body) {
        return Result.ok(kanbanService.listByPartId(body.get("partId")));
    }

    /** 封存看板 */
    @PostMapping("/kanban/block")
    public Result<?> block(@RequestBody Map<String, String> body) {
        kanbanService.blockKanban(body.get("kanbanNo"));
        return Result.ok(null);
    }

    /** 解封看板 */
    @PostMapping("/kanban/unblock")
    public Result<?> unblock(@RequestBody Map<String, String> body) {
        kanbanService.unblockKanban(body.get("kanbanNo"));
        return Result.ok(null);
    }

    /** 扫码翻转封存/解封 */
    @PostMapping("/kanban/toggle-block")
    public Result<Map<String, Object>> toggleBlock(@RequestBody Map<String, String> body) {
        return Result.ok(kanbanService.toggleBlock(body.get("kanbanNo")));
    }

    /** 按零件批量封存 */
    @PostMapping("/kanban/block-by-part")
    public Result<Integer> blockByPart(@RequestBody Map<String, Long> body) {
        return Result.ok(kanbanService.blockByPartId(body.get("partId")));
    }

    /** 批量解封 */
    @PostMapping("/kanban/batch-unblock")
    public Result<Integer> batchUnblock(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> kanbanNos = (List<String>) body.get("kanbanNos");
        return Result.ok(kanbanService.batchUnblock(kanbanNos));
    }
}
