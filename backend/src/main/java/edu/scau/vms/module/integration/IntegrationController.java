package edu.scau.vms.module.integration;

import edu.scau.vms.common.Result;
import edu.scau.vms.module.integration.dto.EduValidateRequest;
import edu.scau.vms.module.integration.dto.EduValidateResult;
import edu.scau.vms.module.integration.dto.SyncResult;
import edu.scau.vms.module.integration.dto.SyncUsersRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 教务系统对接接口
// sync-users 需要管理员权限；edu/validate 白名单免 JWT
@Tag(name = "Integration", description = "外部系统对接（教务同步）")
@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;

    @Operation(summary = "从教务系统批量同步学生账号")
    @PostMapping("/sync-users")
    @PreAuthorize("hasAuthority('ADM')")
    public Result<SyncResult> syncUsers(@Valid @RequestBody SyncUsersRequest req) {
        return Result.ok(integrationService.syncUsers(req));
    }

    @Operation(summary = "教务系统密码验证（白名单免JWT）")
    @PostMapping("/edu/validate")
    public Result<EduValidateResult> eduValidate(@Valid @RequestBody EduValidateRequest req) {
        return Result.ok(integrationService.validateAgainstEdu(req.getStudentId(), req.getPassword()));
    }

    @Operation(summary = "教务同步统计")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ADM')")
    public Result<Map<String, Long>> stats() {
        return Result.ok(integrationService.stats());
    }
}
