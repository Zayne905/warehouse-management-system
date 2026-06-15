package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("outbound_order_detail")
public class OutboundOrderDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long outboundOrderId;
    private Long partId;
    private String partCode;
    private String partName;
    private String unit;
    private BigDecimal plannedQty;
    private BigDecimal actualQty;
    private Integer boxCount;
    private Long warehouseAreaId;
    private Integer lineNo;
    private LocalDateTime createTime;
}
