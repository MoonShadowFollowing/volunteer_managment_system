package edu.scau.vms.module.registration.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RegistrationVO {

    private Long regId;

    private Long activityId;
    private String actNo;
    private String activityName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String location;
    private String desc;
    private Integer limitNum;
    private Integer enrolledNum;

    private Long volunteerId;
    private String volId;       // 展示编号：VOL-{id zero pad 5}
    private String volName;

    /** 报名审核状态：待审核/审核通过/审核拒绝/已取消 */
    private String auditStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appliedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditedAt;

    /** 签到时间（可选 join，志愿者已报名页用） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkInTime;

    /** 签退时间（可选 join，志愿者已报名页用） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkOutTime;

    /** 认证小时（可选 join） */
    private Integer hours;
    /** 认证分钟（可选 join） */
    private Integer minutes;
}
