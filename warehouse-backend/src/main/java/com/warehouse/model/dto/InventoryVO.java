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
    private BigDecimal totalStock;           // 库存总量
    private List<AreaStock> areaStocks;      // 各库区分布

    @Data
    public static class AreaStock {
        private Long areaId;
        private String areaName;
        private BigDecimal quantity;
    }
}
