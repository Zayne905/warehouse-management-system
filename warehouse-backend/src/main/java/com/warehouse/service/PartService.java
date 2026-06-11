package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.PartMapper;
import com.warehouse.mapper.SupplierMapper;
import com.warehouse.mapper.SupplierPartMapper;
import com.warehouse.mapper.WarehouseAreaMapper;
import com.warehouse.model.entity.Part;
import com.warehouse.model.entity.Supplier;
import com.warehouse.model.entity.SupplierPart;
import com.warehouse.model.entity.WarehouseArea;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PartService {

    private final PartMapper partMapper;
    private final SupplierPartMapper supplierPartMapper;
    private final WarehouseAreaMapper warehouseAreaMapper;
    private final SupplierMapper supplierMapper;

    public PartService(PartMapper partMapper, SupplierPartMapper supplierPartMapper,
                       WarehouseAreaMapper warehouseAreaMapper, SupplierMapper supplierMapper) {
        this.partMapper = partMapper;
        this.supplierPartMapper = supplierPartMapper;
        this.warehouseAreaMapper = warehouseAreaMapper;
        this.supplierMapper = supplierMapper;
    }

    public List<Part> list() {
        List<Part> parts = partMapper.selectList(null);
        enrichParts(parts);
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
        enrichParts(parts);
        return parts;
    }

    public Part getById(Long id) {
        Part part = partMapper.selectById(id);
        if (part != null) {
            List<Part> wrapper = List.of(part);
            enrichParts(wrapper);
        }
        return part;
    }

    @Transactional
    public Part save(Part part) {
        if (part.getId() != null) {
            partMapper.updateById(part);
        } else {
            partMapper.insert(part);
        }
        // 保存供应商关联
        if (part.getSupplierId() != null) {
            // 删除旧关联
            supplierPartMapper.delete(
                    new QueryWrapper<SupplierPart>().eq("part_id", part.getId()));
            // 插入新关联
            SupplierPart sp = new SupplierPart();
            sp.setSupplierId(part.getSupplierId());
            sp.setPartId(part.getId());
            supplierPartMapper.insert(sp);
        }
        return part;
    }

    public void delete(Long id) {
        // 删除供应商关联
        supplierPartMapper.delete(
                new QueryWrapper<SupplierPart>().eq("part_id", id));
        partMapper.deleteById(id);
    }

    private void enrichParts(List<Part> parts) {
        if (parts.isEmpty()) return;

        // 填充库区名
        for (Part part : parts) {
            if (part.getWarehouseAreaId() != null) {
                WarehouseArea area = warehouseAreaMapper.selectById(part.getWarehouseAreaId());
                if (area != null) {
                    part.setWarehouseAreaName(area.getName());
                }
            }
        }

        // 填充供应商信息（取第一个关联的供应商）
        List<Long> partIds = parts.stream().map(Part::getId).collect(Collectors.toList());
        List<SupplierPart> links = supplierPartMapper.selectList(
                new QueryWrapper<SupplierPart>().in("part_id", partIds));
        Map<Long, Long> partSupplierMap = links.stream()
                .collect(Collectors.toMap(SupplierPart::getPartId, SupplierPart::getSupplierId, (a, b) -> a));

        for (Part part : parts) {
            Long supId = partSupplierMap.get(part.getId());
            if (supId != null) {
                part.setSupplierId(supId);
                Supplier supplier = supplierMapper.selectById(supId);
                if (supplier != null) {
                    part.setSupplierName(supplier.getName());
                }
            }
        }
    }
}
