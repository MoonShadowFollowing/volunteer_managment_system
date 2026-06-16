package edu.scau.vms.module.registration.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("registrations")
public class Registration {

    @TableId(value = "reg_id", type = IdType.AUTO)
    private Long regId;

    private Long activityId;

    private Long volunteerId;

    private String auditStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime appliedAt;

    private LocalDateTime auditedAt;
}
