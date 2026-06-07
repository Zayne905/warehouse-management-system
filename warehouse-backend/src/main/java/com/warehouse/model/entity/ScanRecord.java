package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("scan_record")
public class ScanRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inboundOrderId;
    private String inboundOrderNo;
    private Long partId;
    private String partCode;
    private String partName;
    private String batchNo;
    private BigDecimal scanQty;
    private LocalDateTime scanTime;
    private Long operatorId;
    private LocalDateTime createTime;
}
