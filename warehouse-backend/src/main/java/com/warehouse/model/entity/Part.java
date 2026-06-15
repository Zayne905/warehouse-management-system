package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("part")
public class Part {
    private Long id;
    private String code;
    private String name;
    private String unit;
    private String spec;
    private Integer packageCapacity;
    private Long warehouseAreaId;
    @TableField(exist = false)
    private String warehouseAreaName;
    @TableField(exist = false)
    private Long supplierId;
    @TableField(exist = false)
    private String supplierName;
    private Boolean enabled;
    private LocalDateTime createTime;
}
