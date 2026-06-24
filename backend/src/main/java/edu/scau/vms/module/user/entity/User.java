package edu.scau.vms.module.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    private String username;

    private String password;

    private String name;

    private String role;

    private String phone;

    private Boolean isOrganizer;

    private Boolean isAdmin;

    private String source;      // 账号来源 LOCAL/EDU

    private LocalDateTime syncedAt;  // 最近同步时间

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
