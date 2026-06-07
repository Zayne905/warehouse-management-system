package com.warehouse.model.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InboundDetailDTO {
    private Long partId;
    private BigDecimal plannedQty;
    private String unit;
    private Long warehouseAreaId;
    private String batchNo;
    private Integer lineNo;
}
