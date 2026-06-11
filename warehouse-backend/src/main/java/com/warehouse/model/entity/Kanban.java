package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("kanban")
public class Kanban {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String kanbanNo;
    private Long inboundOrderId;
    private String inboundOrderNo;
    private Long partId;
    private String partCode;
    private String partName;
    private String supplierName;
    private Integer quantity;
    private Integer boxSeq;
    private Long warehouseAreaId;
    private String warehouseAreaName;
    private Integer status;
    private LocalDateTime createTime;
}
