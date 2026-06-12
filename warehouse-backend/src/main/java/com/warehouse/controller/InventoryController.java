package com.warehouse.controller;

import com.warehouse.model.dto.InventoryVO;
import com.warehouse.model.dto.Result;
import com.warehouse.service.InventoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/inventory/stock-list")
    public Result<List<InventoryVO>> stockList(@RequestBody Map<String, String> body) {
        return Result.ok(inventoryService.listStock(body.get("keyword")));
    }
}
