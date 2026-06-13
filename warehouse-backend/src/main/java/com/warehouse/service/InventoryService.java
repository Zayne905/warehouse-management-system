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
     * 库存总览 — 按零件汇总可用看板，展示实际库存
     */
    public List<InventoryVO> listStock(String keyword) {
        // 1. 加载所有零件
        List<Part> allParts = partMapper.selectList(null);
        Map<Long, Part> partMap = allParts.stream()
                .collect(Collectors.toMap(Part::getId, p -> p));

        // 2. 加载所有库区
        List<WarehouseArea> allAreas = areaMapper.selectList(null);
        Map<Long, String> areaNameMap = allAreas.stream()
                .collect(Collectors.toMap(WarehouseArea::getId, WarehouseArea::getName));

        // 3. 查询所有在库可用 + 部分转出的看板（每个看板=1箱，quantity=该箱件数）
        List<Kanban> activeKanbans = kanbanMapper.selectList(
                new QueryWrapper<Kanban>()
                        .in("status", Kanban.STATUS_AVAILABLE, Kanban.STATUS_PARTIAL_REPACK)
                        .orderByAsc("part_code"));

        // 4. 按零件分组统计
        //     partId → { boxCount: n, totalQty: sum, areaMap: { areaId → qty } }
        Map<Long, Integer> partBoxCount = new LinkedHashMap<>();
        Map<Long, Integer> partTotalQty = new LinkedHashMap<>();
        Map<Long, Map<Long, Integer>> partAreaQty = new LinkedHashMap<>();

        for (Kanban k : activeKanbans) {
            Long pid = k.getPartId();
            partBoxCount.merge(pid, 1, Integer::sum);
            partTotalQty.merge(pid, k.getQuantity() != null ? k.getQuantity() : 0, Integer::sum);

            Long areaId = k.getWarehouseAreaId();
            if (areaId != null) {
                partAreaQty.computeIfAbsent(pid, x -> new LinkedHashMap<>())
                        .merge(areaId, k.getQuantity() != null ? k.getQuantity() : 0, Integer::sum);
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
            int totalQty = partTotalQty.getOrDefault(partId, 0);

            vo.setKanbanCount(boxCount);
            vo.setTotalStock(BigDecimal.valueOf(totalQty));
            vo.setAvgQtyPerBox(boxCount > 0 ? totalQty / boxCount : 0);

            // 库区分布
            Map<Long, Integer> areaQty = partAreaQty.getOrDefault(partId, Map.of());
            List<InventoryVO.AreaStock> areaList = new ArrayList<>();
            for (Map.Entry<Long, Integer> ae : areaQty.entrySet()) {
                InventoryVO.AreaStock as = new InventoryVO.AreaStock();
                as.setAreaId(ae.getKey());
                as.setAreaName(areaNameMap.getOrDefault(ae.getKey(), "未知"));
                as.setQuantity(BigDecimal.valueOf(ae.getValue()));
                areaList.add(as);
            }
            vo.setAreaStocks(areaList);
            result.add(vo);
        }

        return result;
    }
}
