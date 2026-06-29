package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("warehouse")
public class Warehouse {
    private Long id;
    private String code;
    private String name;
    private String address;
    private String adminName;
    private Boolean enabled;
    private LocalDateTime createTime;
}
