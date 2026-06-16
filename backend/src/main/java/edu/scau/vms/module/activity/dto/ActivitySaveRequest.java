package edu.scau.vms.module.activity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivitySaveRequest {

    @NotBlank(message = "活动名称不能为空")
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String desc;

    @NotBlank(message = "活动地点不能为空")
    @Size(max = 100)
    private String location;

    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @NotNull(message = "招募人数不能为空")
    private Integer limitNum;
}
