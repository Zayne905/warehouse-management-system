package com.warehouse.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class RepackOrderSaveDTO {
    private Long id;
    private String repackType;
    private Long partId;
    private Long warehouseAreaId;
    private Integer targetBoxCapacity;  // 向下转包: 目标箱容量
    private String outboundOrderNo;
    private String remark;
    private Long operatorId;
    private List<RepackDetailDTO> details;
}
