package edu.scau.vms.module.registration;

import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.Result;
import edu.scau.vms.common.security.UserPrincipal;
import edu.scau.vms.module.registration.dto.AuditRegRequest;
import edu.scau.vms.module.registration.dto.RegisterRequest;
import edu.scau.vms.module.registration.dto.RegistrationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Registration", description = "活动报名 FR-02")
@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @Operation(summary = "志愿者报名")
    @PostMapping
    public Result<Map<String, Long>> apply(@AuthenticationPrincipal UserPrincipal me,
                                           @Valid @RequestBody RegisterRequest req) {
        Long regId = registrationService.apply(me.userId(), req.getActivityId());
        return Result.ok(Map.of("regId", regId));
    }

    @Operation(summary = "志愿者：我的报名记录")
    @GetMapping("/mine")
    public Result<PageResult<RegistrationVO>> mine(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false, defaultValue = "1") Long page,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String auditStatus) {
        return Result.ok(registrationService.mine(me.userId(), page, pageSize, auditStatus));
    }

    @Operation(summary = "志愿者取消报名")
    @DeleteMapping("/{regId}")
    public Result<Void> cancel(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long regId) {
        registrationService.cancel(regId, me.userId());
        return Result.ok();
    }

    @Operation(summary = "组织者：查看活动的报名列表")
    @GetMapping
    @PreAuthorize("hasAuthority('ORG') or hasAuthority('ADM')")
    public Result<PageResult<RegistrationVO>> byActivity(
            @RequestParam Long activityId,
            @RequestParam(required = false, defaultValue = "1") Long page,
            @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        return Result.ok(registrationService.byActivity(activityId, page, pageSize));
    }

    @Operation(summary = "组织者审核报名")
    @PutMapping("/{regId}/audit")
    @PreAuthorize("hasAuthority('ORG') or hasAuthority('ADM')")
    public Result<Void> audit(@AuthenticationPrincipal UserPrincipal me,
                              @PathVariable Long regId,
                              @Valid @RequestBody AuditRegRequest req) {
        registrationService.audit(regId, me.userId(), me.admin(), req.getApprove());
        return Result.ok();
    }
}
