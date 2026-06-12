package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("outbound_order")
public class OutboundOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Integer status;       // 0=待出库 1=部分出库 2=已完成 3=作废
    private String remark;
    private Long createUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
