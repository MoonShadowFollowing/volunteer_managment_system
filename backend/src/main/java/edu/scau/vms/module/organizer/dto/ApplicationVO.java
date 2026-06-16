package edu.scau.vms.module.organizer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApplicationVO {

    private Long appId;

    private Long applicantId;
    private String applicantNo;   // 展示编号
    private String applicantName;
    private String applicantPhone;

    private String reason;
    private String materialUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submittedAt;

    private String auditStatus;

    private Long auditorId;
    private String auditorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditedAt;
}
