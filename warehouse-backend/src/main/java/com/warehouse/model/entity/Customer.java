package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {
    private Long id;
    private String code;
    private String name;
    private String contact;
    private String phone;
    private String address;
    private Boolean enabled;
    private LocalDateTime createTime;
}
