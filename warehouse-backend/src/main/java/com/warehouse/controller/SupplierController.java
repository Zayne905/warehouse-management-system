package com.warehouse.controller;

import com.warehouse.model.dto.Result;
import com.warehouse.model.entity.Supplier;
import com.warehouse.service.SupplierService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
