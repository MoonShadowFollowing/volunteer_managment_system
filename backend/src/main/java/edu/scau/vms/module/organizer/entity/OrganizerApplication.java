package edu.scau.vms.module.organizer.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("organizer_applications")
public class OrganizerApplication {

    @TableId(value = "app_id", type = IdType.AUTO)
    private Long appId;

    private Long applicantId;

    private String reason;

    private String materialUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime submittedAt;

    private String auditStatus;

    private Long auditorId;

    private LocalDateTime auditedAt;
}
