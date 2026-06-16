package edu.scau.vms.module.certificate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("certificates")
public class Certificate {

    @TableId(value = "cert_id", type = IdType.AUTO)
    private Long certId;

    private String title;

    private Long activityId;

    private Long volunteerId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer certHours;

    private Integer certMinutes;

    private LocalDate issuedDate;

    private String status;
}
