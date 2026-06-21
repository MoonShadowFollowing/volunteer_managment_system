package edu.scau.vms.module.review.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewVO {

    private Long reviewId;

    private Long activityId;

    private String activityName;

    private Long reviewerId;

    private String reviewerName;

    private String reviewerRole;

    private Long targetId;

    private String targetName;

    private Integer rating;

    private String comment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
