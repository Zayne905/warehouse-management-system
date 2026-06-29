package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.WarehouseAreaMapper;
import com.warehouse.mapper.WarehouseMapper;
import com.warehouse.model.entity.WarehouseArea;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarehouseAreaService {

    private final WarehouseAreaMapper warehouseAreaMapper;
    private final WarehouseMapper warehouseMapper;

    public WarehouseAreaService(WarehouseAreaMapper warehouseAreaMapper, WarehouseMapper warehouseMapper) {
        this.warehouseAreaMapper = warehouseAreaMapper;
        this.warehouseMapper = warehouseMapper;
    }

    public List<WarehouseArea> list() {
        List<WarehouseArea> areas = warehouseAreaMapper.selectList(
                new QueryWrapper<WarehouseArea>().orderByAsc("code"));
        // 填充仓库名称
        areas.forEach(area -> {
            if (area.getWarehouseId() != null) {
                var wh = warehouseMapper.selectById(area.getWarehouseId());
                if (wh != null) {
                    area.setWarehouseName(wh.getName());
                }
            }
        });
        return areas;
    }

    public WarehouseArea getById(Long id) {
        return warehouseAreaMapper.selectById(id);
    }

    @Transactional
    public WarehouseArea save(WarehouseArea area) {
        if (area.getId() != null) {
            warehouseAreaMapper.updateById(area);
        } else {
            warehouseAreaMapper.insert(area);
        }
        return area;
    }

    @Transactional
    public void delete(Long id) {
        warehouseAreaMapper.deleteById(id);
    }
}
