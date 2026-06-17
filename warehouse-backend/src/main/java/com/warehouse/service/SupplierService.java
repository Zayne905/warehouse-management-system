package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.SupplierMapper;
import com.warehouse.model.entity.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierMapper supplierMapper;

    public SupplierService(SupplierMapper supplierMapper) {
        this.supplierMapper = supplierMapper;
    }

    public List<Supplier> list() {
        return supplierMapper.selectList(
                new QueryWrapper<Supplier>().orderByAsc("code"));
    }

    public Supplier getById(Long id) {
        return supplierMapper.selectById(id);
    }

    @Transactional
    public Supplier save(Supplier supplier) {
        if (supplier.getId() != null) {
            supplierMapper.updateById(supplier);
        } else {
            supplierMapper.insert(supplier);
        }
        return supplier;
    }

    @Transactional
    public void delete(Long id) {
        supplierMapper.deleteById(id);
    }
}
