package edu.scau.vms.module.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "刷新令牌响应")
public class RefreshResponse {

    @Schema(description = "新的 JWT 令牌")
    private String token;
}
