package com.warehouse.service;

import com.warehouse.mapper.SupplierMapper;
import com.warehouse.model.entity.Supplier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierMapper supplierMapper;

    public SupplierService(SupplierMapper supplierMapper) {
        this.supplierMapper = supplierMapper;
    }

    public List<Supplier> list() {
        return supplierMapper.selectList(null);
    }

    public Supplier getById(Long id) {
        return supplierMapper.selectById(id);
    }
}
