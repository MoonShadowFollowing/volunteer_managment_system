package edu.scau.vms.module.health;

import edu.scau.vms.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "健康检查")
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "Liveness 探活", description = "应用是否启动")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("pong");
    }

    @Operation(summary = "DB 探活", description = "查询 users 表行数验证数据库连接与建表/种子是否就绪")
    @GetMapping("/db")
    public Result<Long> db() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return Result.ok(count == null ? 0L : count);
    }
}
