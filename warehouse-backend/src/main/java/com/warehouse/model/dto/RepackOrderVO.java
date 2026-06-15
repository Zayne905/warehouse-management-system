package com.warehouse.model.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RepackOrderVO {
    private Long id;
    private String orderNo;
    private Integer status;
    private String statusText;
    private String repackType;
    private String repackTypeText;
    private Long partId;
    private String partCode;
    private String partName;
    private Long warehouseAreaId;
    private String warehouseAreaName;
    private Integer targetBoxCapacity;
    private Long outboundOrderId;
    private String outboundOrderNo;
    private String remark;
    private Long operatorId;
    private Integer detailCount;
    private Integer totalTransferQty;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<RepackDetailVO> details;
}
