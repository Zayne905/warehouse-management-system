package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("outbound_scan")
public class OutboundScan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long outboundOrderId;
    private String outboundOrderNo;
    private String kanbanNo;
    private Long partId;
    private String partCode;
    private String partName;
    private BigDecimal quantity;
    private LocalDateTime scanTime;
    private Long operatorId;
    private LocalDateTime createTime;
}
