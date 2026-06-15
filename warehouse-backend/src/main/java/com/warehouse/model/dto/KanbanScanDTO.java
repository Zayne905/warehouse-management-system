package com.warehouse.model.dto;

import lombok.Data;
import java.math.BigDecimal;

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
    private BigDecimal quantity;
    private String warehouseArea;
    private String inboundOrderNo;
    private Integer boxSeq;
    private Long operatorId;
}
