package com.warehouse.controller;

import com.warehouse.model.dto.Result;
import com.warehouse.model.entity.Customer;
import com.warehouse.service.CustomerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/customer/list")
    public Result<List<Customer>> list() {
        return Result.ok(customerService.list());
    }

    @PostMapping("/customer/save")
    public Result<Customer> save(@RequestBody Customer customer) {
        return Result.ok(customerService.save(customer));
    }

    @PostMapping("/customer/delete")
    public Result<?> delete(@RequestBody Map<String, Long> body) {
        customerService.delete(body.get("id"));
        return Result.ok(null);
    }
}
