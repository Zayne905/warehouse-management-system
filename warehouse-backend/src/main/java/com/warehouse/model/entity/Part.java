package com.warehouse.model.entity;

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
    private Boolean enabled;
    private LocalDateTime createTime;
}
