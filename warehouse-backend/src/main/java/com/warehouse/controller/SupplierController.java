package com.warehouse.controller;

import com.warehouse.model.dto.Result;
import com.warehouse.model.entity.Supplier;
import com.warehouse.service.SupplierService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping("/supplier/list")
    public Result<List<Supplier>> list() {
        return Result.ok(supplierService.list());
    }

    @PostMapping("/supplier/save")
    public Result<Supplier> save(@RequestBody Supplier supplier) {
        return Result.ok(supplierService.save(supplier));
    }

    @PostMapping("/supplier/delete")
    public Result<?> delete(@RequestBody Map<String, Long> body) {
        supplierService.delete(body.get("id"));
        return Result.ok(null);
    }
}
