package edu.scau.vms.module.integration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// 教务系统密码验证入参
@Data
public class EduValidateRequest {
    @NotBlank
    private String studentId;
    @NotBlank
    private String password;   // 前端 SHA256 后的哈希
}
