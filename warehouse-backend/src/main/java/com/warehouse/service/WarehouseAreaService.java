package com.warehouse.service;

import com.warehouse.mapper.WarehouseAreaMapper;
import com.warehouse.model.entity.WarehouseArea;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarehouseAreaService {

    private final WarehouseAreaMapper warehouseAreaMapper;

    public WarehouseAreaService(WarehouseAreaMapper warehouseAreaMapper) {
        this.warehouseAreaMapper = warehouseAreaMapper;
    }

    public List<WarehouseArea> list() {
        return warehouseAreaMapper.selectList(null);
    }

    public WarehouseArea getById(Long id) {
        return warehouseAreaMapper.selectById(id);
    }
}
