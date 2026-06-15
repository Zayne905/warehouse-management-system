package com.warehouse.model.dto;

import lombok.Data;

@Data
public class InboundOrderQuery {
    private long current = 1;
    private long size = 10;
    private String orderNo;
    private Long supplierId;
    private String orderNumber;
    private Integer status;
}
