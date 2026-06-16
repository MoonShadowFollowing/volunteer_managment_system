package edu.scau.vms.module.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("attendance")
public class Attendance {

    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    private Long activityId;

    private Long volunteerId;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    private Integer serviceHours;

    private Integer serviceMinutes;

    private String status;
}
