package edu.scau.vms.module.attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AttendanceVO {

    private Long recordId;
    private Long activityId;
    private String actNo;
    private String activityName;

    private Long volunteerId;
    private String volId;
    private String volName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkInTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkOutTime;

    private Integer hours;
    private Integer minutes;

    /** 签到状态：未签到/已签到/已签退/漏签退/异常 */
    private String status;

    /** 签到状态的展示别名：'正常' / signStatus 原值（前端友好） */
    private String signStatus;
}
