package com.warehouse.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class InventoryVO {
    private Long partId;
    private String partCode;
    private String partName;
    private String spec;
    private String unit;
    private Integer packageCapacity;
    private BigDecimal totalStock;           // 库存总量（从看板汇总）
    private Integer kanbanCount;             // 在库箱数
    private Integer avgQtyPerBox;            // 箱均数量（每箱实际件数）
    private Integer minStock;                // 最低储备阈值
    private Integer maxStock;                // 最高储备阈值
    private List<AreaStock> areaStocks;      // 各库区分布

    @Data
    public static class AreaStock {
        private Long areaId;
        private String areaName;
        private BigDecimal quantity;
    }
}
