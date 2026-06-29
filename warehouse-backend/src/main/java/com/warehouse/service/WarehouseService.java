package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.WarehouseMapper;
import com.warehouse.model.entity.Warehouse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarehouseService {

    private final WarehouseMapper warehouseMapper;

    public WarehouseService(WarehouseMapper warehouseMapper) {
        this.warehouseMapper = warehouseMapper;
    }

    public List<Warehouse> list() {
        return warehouseMapper.selectList(
                new QueryWrapper<Warehouse>().orderByAsc("code"));
    }

    public Warehouse getById(Long id) {
        return warehouseMapper.selectById(id);
    }

    @Transactional
    public Warehouse save(Warehouse warehouse) {
        if (warehouse.getId() != null) {
            warehouseMapper.updateById(warehouse);
        } else {
            warehouseMapper.insert(warehouse);
        }
        return warehouse;
    }

    @Transactional
    public void delete(Long id) {
        warehouseMapper.deleteById(id);
    }
}
