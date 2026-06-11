package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.PartMapper;
import com.warehouse.mapper.SupplierPartMapper;
import com.warehouse.mapper.WarehouseAreaMapper;
import com.warehouse.model.entity.Part;
import com.warehouse.model.entity.SupplierPart;
import com.warehouse.model.entity.WarehouseArea;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PartService {

    private final PartMapper partMapper;
    private final SupplierPartMapper supplierPartMapper;
    private final WarehouseAreaMapper warehouseAreaMapper;

    public PartService(PartMapper partMapper, SupplierPartMapper supplierPartMapper,
                       WarehouseAreaMapper warehouseAreaMapper) {
        this.partMapper = partMapper;
        this.supplierPartMapper = supplierPartMapper;
        this.warehouseAreaMapper = warehouseAreaMapper;
    }

    public List<Part> list() {
        List<Part> parts = partMapper.selectList(null);
        enrichWithAreaName(parts);
        return parts;
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
        List<Part> parts = partMapper.selectBatchIds(partIds);
        enrichWithAreaName(parts);
        return parts;
    }

    public Part getById(Long id) {
        return partMapper.selectById(id);
    }

    public Part save(Part part) {
        if (part.getId() != null) {
            partMapper.updateById(part);
        } else {
            partMapper.insert(part);
        }
        return part;
    }

    public void delete(Long id) {
        partMapper.deleteById(id);
    }

    private void enrichWithAreaName(List<Part> parts) {
        for (Part part : parts) {
            if (part.getWarehouseAreaId() != null) {
                WarehouseArea area = warehouseAreaMapper.selectById(part.getWarehouseAreaId());
                if (area != null) {
                    part.setWarehouseAreaName(area.getName());
                }
            }
        }
    }
}
