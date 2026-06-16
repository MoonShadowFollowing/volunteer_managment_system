package edu.scau.vms.module.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录用户信息")
public class UserInfo {

    @Schema(description = "用户编号")
    private Long userId;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "角色：volunteer/organizer/admin/superadmin")
    private String role;

    @Schema(description = "是否具备组织者资质")
    @JsonProperty("isOrganizerQualified")
    private boolean isOrganizerQualified;

    @Schema(description = "是否被超管赋权管理员")
    @JsonProperty("isAdmin")
    private boolean isAdmin;
}
