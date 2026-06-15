package com.warehouse.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private Boolean enabled;
    private String role;
    private LocalDateTime createTime;
}
