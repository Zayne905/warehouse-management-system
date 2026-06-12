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
}
