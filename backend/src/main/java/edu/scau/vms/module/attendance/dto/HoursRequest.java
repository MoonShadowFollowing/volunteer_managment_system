package edu.scau.vms.module.attendance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HoursRequest {

    @NotNull
    @Min(value = 0, message = "小时数不能为负")
    private Integer hours;

    @NotNull
    @Min(value = 0, message = "分钟数不能为负")
    @Max(value = 59, message = "分钟数 0~59")
    private Integer minutes;
}
