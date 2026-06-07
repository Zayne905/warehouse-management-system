package com.warehouse.model.dto;

import lombok.Data;

@Data
public class ScanDuplicateCheckDTO {
    private Long inboundOrderId;
    private String partCode;
    private String batchNo;
}
