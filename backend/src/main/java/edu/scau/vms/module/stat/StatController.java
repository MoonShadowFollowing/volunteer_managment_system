package edu.scau.vms.module.stat;

import edu.scau.vms.common.Result;
import edu.scau.vms.common.security.UserPrincipal;
import edu.scau.vms.module.stat.dto.DashboardStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Stat", description = "工作台聚合统计 FR-06")
@RestController
@RequestMapping("/api/stat")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @Operation(summary = "工作台数据（按当前角色返回不同字段）")
    @GetMapping("/dashboard")
    public Result<DashboardStats> dashboard(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false) String view) {
        return Result.ok(statService.forUser(me, view));
    }
}
