package com.warehouse.controller;

import com.warehouse.model.dto.Result;
import com.warehouse.model.entity.Warehouse;
import com.warehouse.service.WarehouseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping("/warehouse/list")
    public Result<List<Warehouse>> list() {
        return Result.ok(warehouseService.list());
    }

    @PostMapping("/warehouse/save")
    public Result<Warehouse> save(@RequestBody Warehouse warehouse) {
        return Result.ok(warehouseService.save(warehouse));
    }

    @PostMapping("/warehouse/delete")
    public Result<?> delete(@RequestBody Map<String, Long> body) {
        warehouseService.delete(body.get("id"));
        return Result.ok(null);
    }
}
