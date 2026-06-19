package edu.scau.vms.module.certificate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CertificateVO {

    private Long certId;
    private String certNo;
    private String certName;

    private Long activityId;
    private String actNo;
    private String activityName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Integer hours;
    private Integer minutes;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issuedDate;

    private String status;
}
