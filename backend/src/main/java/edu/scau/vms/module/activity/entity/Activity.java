package edu.scau.vms.module.activity.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activities")
public class Activity {

    @TableId(value = "activity_id", type = IdType.AUTO)
    private Long activityId;

    private String title;

    private String description;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer capacity;

    private Long organizerId;

    private String auditStatus;

    private String publishStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
