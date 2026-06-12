package com.warehouse.model.dto;

import lombok.Data;

/**
 * 看板扫码入库请求
 * 客户端扫描看板QR码，将JSON原样发送到后端
 */
@Data
public class KanbanScanDTO {
    private String kanbanNo;
    private String partCode;
    private String partName;
    private String supplierName;
    private Integer quantity;
    private String warehouseArea;
    private String inboundOrderNo;
    private Integer boxSeq;
    private Long operatorId;
}
