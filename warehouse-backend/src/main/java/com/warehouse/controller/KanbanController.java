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
}
