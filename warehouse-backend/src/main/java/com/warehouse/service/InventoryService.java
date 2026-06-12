package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.InboundOrderDetailMapper;
import com.warehouse.mapper.PartMapper;
import com.warehouse.mapper.WarehouseAreaMapper;
import com.warehouse.model.dto.InventoryVO;
import com.warehouse.model.entity.InboundOrderDetail;
import com.warehouse.model.entity.Part;
import com.warehouse.model.entity.WarehouseArea;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InboundOrderDetailMapper detailMapper;
    private final PartMapper partMapper;
    private final WarehouseAreaMapper areaMapper;

    public InventoryService(InboundOrderDetailMapper detailMapper,
                            PartMapper partMapper,
                            WarehouseAreaMapper areaMapper) {
        this.detailMapper = detailMapper;
        this.partMapper = partMapper;
        this.areaMapper = areaMapper;
    }

    /**
     * 库存总览 — 按零件汇总所有入库单的实际入库量
     */
    public List<InventoryVO> listStock(String keyword) {
        // 1. 查询所有有实际入库量的明细
        List<InboundOrderDetail> allDetails = detailMapper.selectList(
                new QueryWrapper<InboundOrderDetail>()
                        .gt("actual_qty", 0)
                        .orderByAsc("part_code"));

        if (allDetails.isEmpty()) {
            return List.of();
        }

        // 2. 加载所有零件
        List<Part> allParts = partMapper.selectList(null);
        Map<Long, Part> partMap = allParts.stream()
                .collect(Collectors.toMap(Part::getId, p -> p));

        // 3. 加载所有库区
        List<WarehouseArea> allAreas = areaMapper.selectList(null);
        Map<Long, String> areaNameMap = allAreas.stream()
                .collect(Collectors.toMap(WarehouseArea::getId, WarehouseArea::getName));

        // 4. 按零件+库区分组汇总
        Map<Long, Map<Long, BigDecimal>> partAreaStock = new LinkedHashMap<>(); // partId -> { areaId -> qty }
        for (InboundOrderDetail d : allDetails) {
            Long areaId = d.getWarehouseAreaId();
            partAreaStock
                    .computeIfAbsent(d.getPartId(), k -> new LinkedHashMap<>())
                    .merge(areaId, d.getActualQty() != null ? d.getActualQty() : BigDecimal.ZERO, BigDecimal::add);
        }

        // 5. 构造 VO 列表
        List<InventoryVO> result = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, BigDecimal>> entry : partAreaStock.entrySet()) {
            Long partId = entry.getKey();
            Part part = partMap.get(partId);
            if (part == null) continue;

            // 关键词过滤
            if (StringUtils.hasText(keyword)) {
                String kw = keyword.toLowerCase();
                if (!part.getCode().toLowerCase().contains(kw)
                        && !part.getName().toLowerCase().contains(kw)) {
                    continue;
                }
            }

            InventoryVO vo = new InventoryVO();
            vo.setPartId(partId);
            vo.setPartCode(part.getCode());
            vo.setPartName(part.getName());
            vo.setSpec(part.getSpec());
            vo.setUnit(part.getUnit());
            vo.setPackageCapacity(part.getPackageCapacity() != null ? part.getPackageCapacity() : 1);

            Map<Long, BigDecimal> areaStocks = entry.getValue();
            BigDecimal total = areaStocks.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setTotalStock(total);

            List<InventoryVO.AreaStock> areaList = new ArrayList<>();
            for (Map.Entry<Long, BigDecimal> as : areaStocks.entrySet()) {
                InventoryVO.AreaStock areaStock = new InventoryVO.AreaStock();
                areaStock.setAreaId(as.getKey());
                areaStock.setAreaName(areaNameMap.getOrDefault(as.getKey(), "未知"));
                areaStock.setQuantity(as.getValue());
                areaList.add(areaStock);
            }
            vo.setAreaStocks(areaList);
            result.add(vo);
        }

        return result;
    }
}
