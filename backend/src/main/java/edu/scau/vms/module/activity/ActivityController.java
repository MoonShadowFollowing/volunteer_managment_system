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

@Tag(name = "Activity", description = "志愿活动")
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    // volunteerView=true 给志愿者列表用，会自动过滤"审核通过+发布中"
    @Operation(summary = "分页查活动")
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

    // 刚发布的活动默认进"待审核 + 已停止"，等管理员点过才能开
    @Operation(summary = "组织者发布新活动")
    @PostMapping
    @PreAuthorize("hasAuthority('ORG')")
    public Result<Map<String, Long>> create(@AuthenticationPrincipal UserPrincipal me,
                                            @Valid @RequestBody ActivitySaveRequest req) {
        Long id = activityService.create(me.userId(), req);
        return Result.ok(Map.of("activityId", id));
    }

    // 注意：组织者改完活动会回到"待审核"，得重新走一遍审核
    @Operation(summary = "改活动")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG') or hasAuthority('ADM')")
    public Result<Void> update(@AuthenticationPrincipal UserPrincipal me,
                               @PathVariable Long id,
                               @Valid @RequestBody ActivitySaveRequest req) {
        activityService.update(id, me.userId(), me.admin(), req);
        return Result.ok();
    }

    @Operation(summary = "删活动")
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

    @Operation(summary = "前台发布开关（仅审核通过后可用）")
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('ORG') or hasAuthority('ADM')")
    public Result<Void> publish(@AuthenticationPrincipal UserPrincipal me,
                                @PathVariable Long id,
                                @Valid @RequestBody PublishRequest req) {
        activityService.togglePublish(id, me.userId(), me.admin(), req.getPublish());
        return Result.ok();
    }
}
