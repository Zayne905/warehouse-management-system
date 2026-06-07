package com.warehouse.controller;

import com.warehouse.model.dto.Result;
import com.warehouse.model.entity.Part;
import com.warehouse.service.PartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @GetMapping("/part/list")
    public Result<List<Part>> list(@RequestParam(required = false) Long supplierId) {
        return Result.ok(partService.listBySupplier(supplierId));
    }
}
