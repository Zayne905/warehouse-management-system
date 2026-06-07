package com.warehouse.controller;

import com.warehouse.model.dto.Result;
import com.warehouse.model.entity.WarehouseArea;
import com.warehouse.service.WarehouseAreaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class WarehouseAreaController {

    private final WarehouseAreaService warehouseAreaService;

    public WarehouseAreaController(WarehouseAreaService warehouseAreaService) {
        this.warehouseAreaService = warehouseAreaService;
    }

    @GetMapping("/warehouse-area/list")
    public Result<List<WarehouseArea>> list() {
        return Result.ok(warehouseAreaService.list());
    }
}
