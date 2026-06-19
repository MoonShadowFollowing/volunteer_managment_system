package edu.scau.vms.module.organizer;

import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.Result;
import edu.scau.vms.common.security.UserPrincipal;
import edu.scau.vms.module.organizer.dto.ApplicationVO;
import edu.scau.vms.module.organizer.dto.AuditApplicationRequest;
import edu.scau.vms.module.organizer.dto.SubmitApplicationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "OrganizerApplication", description = "组织者资质申请与审核 FR-09")
@RestController
@RequestMapping("/api/organizer-applications")
@RequiredArgsConstructor
public class OrganizerApplicationController {

    private final OrganizerApplicationService service;

    @Operation(summary = "志愿者提交组织者资质申请")
    @PostMapping
    public Result<Map<String, Long>> submit(@AuthenticationPrincipal UserPrincipal me,
                                            @Valid @RequestBody SubmitApplicationRequest req) {
        Long id = service.submit(me.userId(), req);
        return Result.ok(Map.of("appId", id));
    }

    @Operation(summary = "我的申请历史")
    @GetMapping("/mine")
    public Result<PageResult<ApplicationVO>> mine(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false, defaultValue = "1") Long page,
            @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        return Result.ok(service.mine(me.userId(), page, pageSize));
    }

    @Operation(summary = "管理员查看申请列表")
    @GetMapping
    @PreAuthorize("hasAuthority('ADM')")
    public Result<PageResult<ApplicationVO>> list(
            @RequestParam(required = false, defaultValue = "1") Long page,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String userNo) {
        return Result.ok(service.list(page, pageSize, status, name, userNo));
    }

    @Operation(summary = "审核组织者申请（通过/拒绝）")
    @PutMapping("/{appId}/audit")
    @PreAuthorize("hasAuthority('ADM')")
    public Result<Void> audit(@AuthenticationPrincipal UserPrincipal me,
                              @PathVariable Long appId,
                              @Valid @RequestBody AuditApplicationRequest req) {
        service.audit(appId, me.userId(), req.getApprove());
        return Result.ok();
    }
}
