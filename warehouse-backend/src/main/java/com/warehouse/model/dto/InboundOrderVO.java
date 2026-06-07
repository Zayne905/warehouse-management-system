package com.warehouse.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class InboundOrderVO {
    private Long id;
    private String orderNo;
    private Long supplierId;
    private String supplierName;
    private String orderNumber;
    private Integer status;
    private String statusText;
    private String remark;
    private String createTime;
    private String updateTime;
    private List<InboundDetailVO> details;
}
