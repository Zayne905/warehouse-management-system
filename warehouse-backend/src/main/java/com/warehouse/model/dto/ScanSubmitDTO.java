package com.warehouse.model.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ScanSubmitDTO {
    private Long inboundOrderId;
    private String inboundOrderNo;
    private String partCode;
    private String batchNo;
    private BigDecimal scanQty;
    private Long operatorId;
}
