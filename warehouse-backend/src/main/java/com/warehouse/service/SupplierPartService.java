package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.SupplierPartMapper;
import com.warehouse.model.entity.SupplierPart;
import org.springframework.stereotype.Service;

@Service
public class SupplierPartService {

    private final SupplierPartMapper supplierPartMapper;

    public SupplierPartService(SupplierPartMapper supplierPartMapper) {
        this.supplierPartMapper = supplierPartMapper;
    }

    public boolean isPartBelongsToSupplier(Long supplierId, Long partId) {
        return supplierPartMapper.selectCount(
                new QueryWrapper<SupplierPart>()
                        .eq("supplier_id", supplierId)
                        .eq("part_id", partId)) > 0;
    }
}
