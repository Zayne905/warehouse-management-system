package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("repack_order")
public class RepackOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Integer status;
    private String repackType;
    private Long partId;            // 限定零件ID
    private String partCode;        // 限定零件编码
    private String partName;        // 限定零件名称
    private Long warehouseAreaId;   // 限定库区ID
    private String warehouseAreaName; // 限定库区名称
    private Integer targetBoxCapacity; // 向下转包：目标箱容量
    private Long outboundOrderId;
    private String outboundOrderNo;
    private String remark;
    private Long operatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_CANCELLED = 2;

    public String getStatusText() {
        switch (this.status) { case 0: return "待转包"; case 1: return "已完成"; case 2: return "已取消"; default: return "未知"; }
    }
    public String getRepackTypeText() {
        if (this.repackType == null) return "未知";
        switch (this.repackType) { case "BREAKDOWN": return "向下转包（拆包）"; case "CONSOLIDATE": return "向上转包（合并）"; case "REMAINDER": return "带余量转包"; default: return this.repackType; }
    }
}
