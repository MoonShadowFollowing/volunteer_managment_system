package edu.scau.vms.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "JWT 令牌（请放进 Authorization: Bearer 头）")
    private String token;

    @Schema(description = "用户基本信息")
    private UserInfo user;
}
