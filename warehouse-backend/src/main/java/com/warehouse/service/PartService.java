package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.PartMapper;
import com.warehouse.mapper.SupplierPartMapper;
import com.warehouse.model.entity.Part;
import com.warehouse.model.entity.SupplierPart;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PartService {

    private final PartMapper partMapper;
    private final SupplierPartMapper supplierPartMapper;

    public PartService(PartMapper partMapper, SupplierPartMapper supplierPartMapper) {
        this.partMapper = partMapper;
        this.supplierPartMapper = supplierPartMapper;
    }

    public List<Part> list() {
        return partMapper.selectList(null);
    }

    public List<Part> listBySupplier(Long supplierId) {
        if (supplierId == null) {
            return list();
        }
        List<SupplierPart> links = supplierPartMapper.selectList(
                new QueryWrapper<SupplierPart>().eq("supplier_id", supplierId));
        List<Long> partIds = links.stream()
                .map(SupplierPart::getPartId)
                .collect(Collectors.toList());
        if (partIds.isEmpty()) {
            return List.of();
        }
        return partMapper.selectBatchIds(partIds);
    }

    public Part getById(Long id) {
        return partMapper.selectById(id);
    }
}
