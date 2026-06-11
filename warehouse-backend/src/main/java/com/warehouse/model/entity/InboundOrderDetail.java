package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inbound_order_detail")
public class InboundOrderDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inboundOrderId;
    private Long partId;
    private String partCode;
    private String partName;
    private String unit;
    private BigDecimal plannedQty;
    private BigDecimal actualQty;
    private Long warehouseAreaId;
    private String batchNo;
    private Integer boxCount;
    private Integer lineNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
