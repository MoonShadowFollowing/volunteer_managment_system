package edu.scau.vms.module.organizer;

import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.Result;
import edu.scau.vms.module.user.UserService;
import edu.scau.vms.module.user.dto.UserSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Organizer", description = "已通过组织者资质管理 FR-09")
@RestController
@RequestMapping("/api/organizers")
@RequiredArgsConstructor
public class OrganizerController {

    private final UserService userService;

    @Operation(summary = "已通过组织者列表（含已发活动数）")
    @GetMapping
    @PreAuthorize("hasAuthority('ADM')")
    public Result<PageResult<UserSummaryVO>> list(
            @RequestParam(required = false, defaultValue = "1") Long page,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String userNo) {
        return Result.ok(userService.organizers(page, pageSize, name, userNo));
    }

    @Operation(summary = "撤销组织者资质")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('ADM')")
    public Result<Void> revoke(@PathVariable Long userId) {
        userService.revokeOrganizer(userId);
        return Result.ok();
    }
}
