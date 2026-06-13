package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    private Long outboundOrderId;
    private String outboundOrderNo;
    private Long partId;
    private String partCode;
    private String partName;
    private String supplierName;
    private Integer quantity;
    private Integer originalQty;   // 箱容量（原始装箱数量，不受转包影响）
    private Integer boxSeq;
    private Long warehouseAreaId;
    private String warehouseAreaName;
    private Integer status;
    private LocalDateTime createTime;

    // 状态常量
    public static final int STATUS_PENDING_INBOUND = 0;
    public static final int STATUS_AVAILABLE = 1;
    public static final int STATUS_LOCKED = 2;
    public static final int STATUS_OUTBOUND = 3;
    public static final int STATUS_BLOCKED = 4;
    public static final int STATUS_PARTIAL_REPACK = 5; // 部分转出（剩余>0，仍可被出库匹配）
    public static final int STATUS_CLEARED = 6;        // 已清空（剩余=0，完全转出或合并后作废）

    // 出库扫码时间（非表字段，查询时动态填充）
    @TableField(exist = false)
    private LocalDateTime outboundScanTime;

    public String getStatusText() {
        switch (this.status) {
            case 0: return "待入库";
            case 1: return "在库可用";
            case 2: return "待出库";
            case 3: return "已出库";
            case 4: return "封存";
            case 5: return "部分转出";
            case 6: return "已清空";
            default: return "未知";
        }
    }
}
