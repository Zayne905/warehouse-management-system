package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("repack_relation")
public class RepackRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String parentKanbanNo;
    private String childKanbanNo;
    private Long repackOrderId;
    private String repackOrderNo;
    private Integer transferQty;
    private Long partId;
    private String partCode;
    private String partName;
    private Long operatorId;
    private LocalDateTime repackTime;
    private LocalDateTime createTime;
}
