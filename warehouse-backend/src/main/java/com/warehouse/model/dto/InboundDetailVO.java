package com.warehouse.model.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InboundDetailVO {
    private Long id;
    private Long partId;
    private String partCode;
    private String partName;
    private String unit;
    private BigDecimal plannedQty;
    private BigDecimal actualQty;
    private Long warehouseAreaId;
    private String warehouseAreaName;
    private String batchNo;
    private Integer boxCount;
    private Integer packageCapacity;
    private Integer lineNo;
}
