package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("repack_order_detail")
public class RepackOrderDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long repackOrderId;
    private Long sourceKanbanId;
    private String sourceKanbanNo;
    private String targetKanbanNo;
    private Long partId;
    private String partCode;
    private String partName;
    private Integer transferQty;
    private Integer remainderQty;
    private Integer sourceRemainingQty;
    private Integer lineNo;
    private LocalDateTime createTime;
}
