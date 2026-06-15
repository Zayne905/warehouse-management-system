package com.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.warehouse.mapper.InboundOrderDetailMapper;
import com.warehouse.mapper.KanbanMapper;
import com.warehouse.mapper.PartMapper;
import com.warehouse.mapper.WarehouseAreaMapper;
import com.warehouse.model.dto.InventoryVO;
import com.warehouse.model.entity.InboundOrderDetail;
import com.warehouse.model.entity.Kanban;
import com.warehouse.model.entity.Part;
import com.warehouse.model.entity.WarehouseArea;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InboundOrderDetailMapper detailMapper;
    private final PartMapper partMapper;
    private final WarehouseAreaMapper areaMapper;
    private final KanbanMapper kanbanMapper;

    public InventoryService(InboundOrderDetailMapper detailMapper,
                            PartMapper partMapper,
                            WarehouseAreaMapper areaMapper,
                            KanbanMapper kanbanMapper) {
        this.detailMapper = detailMapper;
        this.partMapper = partMapper;
        this.areaMapper = areaMapper;
        this.kanbanMapper = kanbanMapper;
    }

    /**
     * 库存总览 — 按零件汇总仍在仓库中的看板，包含可用、封存和部分转出状态。
     */
    public List<InventoryVO> listStock(String keyword, Long warehouseAreaId) {
        // 1. 加载所有零件
        List<Part> allParts = partMapper.selectList(null);
        Map<Long, Part> partMap = allParts.stream()
                .collect(Collectors.toMap(Part::getId, p -> p));

        // 2. 加载所有库区
        List<WarehouseArea> allAreas = areaMapper.selectList(null);
        Map<Long, String> areaNameMap = allAreas.stream()
                .collect(Collectors.toMap(WarehouseArea::getId, WarehouseArea::getName));

        // 3. 查询所有仍在仓库中的看板。封存只影响可用性，不应让零件从库存总览消失。
        QueryWrapper<Kanban> inventoryQuery = new QueryWrapper<Kanban>()
                .in("status", Kanban.STATUS_AVAILABLE, Kanban.STATUS_BLOCKED, Kanban.STATUS_PARTIAL_REPACK)
                .orderByAsc("part_code");
        if (warehouseAreaId != null) inventoryQuery.eq("warehouse_area_id", warehouseAreaId);
        List<Kanban> inventoryKanbans = kanbanMapper.selectList(inventoryQuery);

        // 4. 按零件分组统计
        //     partId → { boxCount: n, totalQty: sum, areaMap: { areaId → qty } }
        Map<Long, Integer> partBoxCount = new LinkedHashMap<>();
        Map<Long, BigDecimal> partTotalQty = new LinkedHashMap<>();
        Map<Long, Map<Long, BigDecimal>> partAreaQty = new LinkedHashMap<>();

        for (Kanban k : inventoryKanbans) {
            Long pid = k.getPartId();
            partBoxCount.merge(pid, 1, Integer::sum);
            partTotalQty.merge(pid, k.getQuantity() != null ? k.getQuantity() : BigDecimal.ZERO, BigDecimal::add);

            Long areaId = k.getWarehouseAreaId();
            if (areaId != null) {
                partAreaQty.computeIfAbsent(pid, x -> new LinkedHashMap<>())
                        .merge(areaId, k.getQuantity() != null ? k.getQuantity() : BigDecimal.ZERO, BigDecimal::add);
            }
        }

        // 5. 构造 VO
        List<InventoryVO> result = new ArrayList<>();
        for (Long partId : partBoxCount.keySet()) {
            Part part = partMap.get(partId);
            if (part == null) continue;

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

            int boxCount = partBoxCount.getOrDefault(partId, 0);
            BigDecimal totalQty = partTotalQty.getOrDefault(partId, BigDecimal.ZERO);

            vo.setKanbanCount(boxCount);
            vo.setTotalStock(totalQty);
            vo.setAvgQtyPerBox(boxCount > 0
                    ? totalQty.divide(BigDecimal.valueOf(boxCount), 0, RoundingMode.HALF_UP).intValue()
                    : 0);

            // 库区分布
            Map<Long, BigDecimal> areaQty = partAreaQty.getOrDefault(partId, Map.of());
            List<InventoryVO.AreaStock> areaList = new ArrayList<>();
            for (Map.Entry<Long, BigDecimal> ae : areaQty.entrySet()) {
                InventoryVO.AreaStock as = new InventoryVO.AreaStock();
                as.setAreaId(ae.getKey());
                as.setAreaName(areaNameMap.getOrDefault(ae.getKey(), "未知"));
                as.setQuantity(ae.getValue());
                areaList.add(as);
            }
            vo.setAreaStocks(areaList);
            result.add(vo);
        }

        return result;
    }
}
