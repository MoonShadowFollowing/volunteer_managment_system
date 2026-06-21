package edu.scau.vms.module.review.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_reviews")
public class ActivityReview {

    @TableId(value = "review_id", type = IdType.AUTO)
    private Long reviewId;

    private Long activityId;

    private Long reviewerId;

    private String reviewerRole;

    private Long targetId;

    private Integer rating;

    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
