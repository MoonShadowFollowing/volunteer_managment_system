package edu.scau.vms.module.auth;

import edu.scau.vms.common.Result;
import edu.scau.vms.common.security.UserPrincipal;
import edu.scau.vms.module.auth.dto.LoginRequest;
import edu.scau.vms.module.auth.dto.LoginResponse;
import edu.scau.vms.module.auth.dto.RefreshResponse;
import edu.scau.vms.module.auth.dto.UserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "鉴权与会话")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录", description = "账号密码登录，成功返回 JWT 与用户信息")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @Operation(summary = "获取当前用户", description = "凭 Bearer Token 拉取当前登录用户")
    @GetMapping("/me")
    public Result<UserInfo> me(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(authService.me(principal));
    }

    @Operation(summary = "刷新 Token", description = "凭旧 token 换取一个新 token，避免长会话强制下线")
    @PostMapping("/refresh")
    public Result<RefreshResponse> refresh(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(new RefreshResponse(authService.refresh(principal)));
    }

    // JWT 是无状态的，登出其实就是前端把 token 扔了，这接口纯粹是个仪式感
    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
