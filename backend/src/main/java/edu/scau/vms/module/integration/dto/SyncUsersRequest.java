package edu.scau.vms.module.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

// 教务系统批量同步用户的请求体
@Data
public class SyncUsersRequest {

    @NotNull
    private List<EduUser> users;

    @Data
    public static class EduUser {
        @NotBlank
        private String studentId;   // 学号，对应 users.username

        @NotBlank
        private String name;        // 姓名

        // 教务系统传过来的密码已经是 BCrypt(SHA256(明文))，VMS 直接存不碰明文
        @NotBlank
        private String passwordHash;
    }
}
