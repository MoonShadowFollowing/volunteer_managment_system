package edu.scau.vms.module.external.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "对外综测系统：志愿者累计认证工时")
public class PublicHoursVO {

    @Schema(description = "学号（users.username）", example = "20240001")
    private String studentId;

    @Schema(description = "姓名", example = "张三")
    private String studentName;

    @Schema(description = "累计认证工时-小时部分", example = "12")
    private Integer totalHours;

    @Schema(description = "累计认证工时-分钟部分 0~59", example = "30")
    private Integer totalMinutes;

    @Schema(description = "格式化工时", example = "12小时30分钟")
    private String formattedDuration;

    @Schema(description = "已认证活动数（已签退且工时>0）", example = "5")
    private Integer certifiedActivityCount;

    @Schema(description = "本次查询生成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime generatedAt;
}
