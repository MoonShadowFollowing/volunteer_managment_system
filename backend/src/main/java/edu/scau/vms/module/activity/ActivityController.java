package edu.scau.vms.module.activity;

import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.Result;
import edu.scau.vms.common.security.UserPrincipal;
import edu.scau.vms.module.activity.dto.ActivitySaveRequest;
import edu.scau.vms.module.activity.dto.ActivityVO;
import edu.scau.vms.module.activity.dto.AuditRequest;
import edu.scau.vms.module.activity.dto.PublishRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "Activity", description = "志愿活动管理 FR-01")
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @Operation(summary = "分页查询活动")
    @GetMapping
    public Result<PageResult<ActivityVO>> list(
            @RequestParam(required = false, defaultValue = "1") Long page,
            @RequestParam(required = false, defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) String publishStatus,
            @RequestParam(required = false) Long organizerId,
            @RequestParam(required = false, defaultValue = "false") boolean volunteerView) {
        return Result.ok(activityService.list(page, pageSize, name, startDate, endDate,
                auditStatus, publishStatus, organizerId, volunteerView));
    }

    @Operation(summary = "活动详情")
    @GetMapping("/{id}")
    public Result<ActivityVO> detail(@PathVariable Long id) {
        return Result.ok(activityService.detail(id));
    }

    @Operation(summary = "组织者发布新活动（待审核）")
    @PostMapping
    @PreAuthorize("hasAuthority('ORG')")
    public Result<Map<String, Long>> create(@AuthenticationPrincipal UserPrincipal me,
                                            @Valid @RequestBody ActivitySaveRequest req) {
        Long id = activityService.create(me.userId(), req);
        return Result.ok(Map.of("activityId", id));
    }

    @Operation(summary = "组织者修改活动（修改后重新进入待审核）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG') or hasAuthority('ADM')")
    public Result<Void> update(@AuthenticationPrincipal UserPrincipal me,
                               @PathVariable Long id,
                               @Valid @RequestBody ActivitySaveRequest req) {
        activityService.update(id, me.userId(), me.admin(), req);
        return Result.ok();
    }

    @Operation(summary = "组织者删除活动")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG') or hasAuthority('ADM')")
    public Result<Void> delete(@AuthenticationPrincipal UserPrincipal me, @PathVariable Long id) {
        activityService.delete(id, me.userId(), me.admin());
        return Result.ok();
    }

    @Operation(summary = "管理员审核活动")
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('ADM')")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditRequest req) {
        activityService.audit(id, req.getApprove());
        return Result.ok();
    }

    @Operation(summary = "组织者切换前台发布状态")
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('ORG') or hasAuthority('ADM')")
    public Result<Void> publish(@AuthenticationPrincipal UserPrincipal me,
                                @PathVariable Long id,
                                @Valid @RequestBody PublishRequest req) {
        activityService.togglePublish(id, me.userId(), me.admin(), req.getPublish());
        return Result.ok();
    }
}
