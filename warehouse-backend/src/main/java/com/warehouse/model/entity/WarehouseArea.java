package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("warehouse_area")
public class WarehouseArea {
    private Long id;
    private String code;
    private String name;
    private Boolean enabled;
    private LocalDateTime createTime;
}
