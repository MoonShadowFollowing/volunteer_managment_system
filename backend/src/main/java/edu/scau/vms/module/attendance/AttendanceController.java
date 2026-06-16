package edu.scau.vms.module.attendance;

import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.Result;
import edu.scau.vms.common.security.UserPrincipal;
import edu.scau.vms.module.attendance.dto.AttendanceVO;
import edu.scau.vms.module.attendance.dto.HoursRequest;
import edu.scau.vms.module.attendance.dto.ManualSignRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Attendance", description = "签到与志愿时 FR-03/04")
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "组织者查看活动签到记录")
    @GetMapping
    @PreAuthorize("hasAuthority('ORG') or hasAuthority('ADM')")
    public Result<PageResult<AttendanceVO>> list(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam Long activityId,
            @RequestParam(required = false, defaultValue = "1") Long page,
            @RequestParam(required = false, defaultValue = "10") Long pageSize) {
        return Result.ok(attendanceService.byActivity(activityId, page, pageSize, me.userId(), me.admin()));
    }

    @Operation(summary = "修改志愿时（工时=0 自动失效证书）")
    @PutMapping("/{recordId}/hours")
    @PreAuthorize("hasAuthority('ORG') or hasAuthority('ADM')")
    public Result<Void> updateHours(@AuthenticationPrincipal UserPrincipal me,
                                    @PathVariable Long recordId,
                                    @Valid @RequestBody HoursRequest req) {
        attendanceService.updateHours(recordId, me.userId(), me.admin(), req);
        return Result.ok();
    }

    @Operation(summary = "手动补签")
    @PutMapping("/{recordId}/manual")
    @PreAuthorize("hasAuthority('ORG') or hasAuthority('ADM')")
    public Result<Void> manualSign(@AuthenticationPrincipal UserPrincipal me,
                                   @PathVariable Long recordId,
                                   @Valid @RequestBody ManualSignRequest req) {
        attendanceService.manualSign(recordId, me.userId(), me.admin(), req);
        return Result.ok();
    }
}
