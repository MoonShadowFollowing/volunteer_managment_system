package edu.scau.vms.module.user;

import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.Result;
import edu.scau.vms.common.security.UserPrincipal;
import edu.scau.vms.module.user.dto.UserSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "用户与角色管理 FR-08")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "可提升为管理员的用户（超管用）")
    @GetMapping("/promotable")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Result<PageResult<UserSummaryVO>> promotable(
            @RequestParam(required = false, defaultValue = "1") Long page,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String userNo) {
        return Result.ok(userService.promotable(page, pageSize, name, userNo));
    }

    @Operation(summary = "当前管理员列表（超管用）")
    @GetMapping("/admins")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Result<PageResult<UserSummaryVO>> admins(
            @RequestParam(required = false, defaultValue = "1") Long page,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String userNo) {
        return Result.ok(userService.admins(page, pageSize, name, userNo));
    }

    @Operation(summary = "提升为管理员")
    @PutMapping("/{userId}/promote-admin")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Result<Void> promote(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long userId) {
        userService.promoteAdmin(userId, me.userId());
        return Result.ok();
    }

    @Operation(summary = "撤销管理员")
    @PutMapping("/{userId}/revoke-admin")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public Result<Void> revoke(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long userId) {
        userService.revokeAdmin(userId, me.userId());
        return Result.ok();
    }
}
