package edu.scau.vms.module.activity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityVO {

    private Long activityId;
    private String actNo;
    private String name;
    private String desc;
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Integer limitNum;
    private Integer enrolledNum;

    private Long organizerId;
    private String organizerName;

    private String auditStatus;
    private String publishStatus;
    @JsonProperty("isPublished")
    private boolean isPublished;
}
