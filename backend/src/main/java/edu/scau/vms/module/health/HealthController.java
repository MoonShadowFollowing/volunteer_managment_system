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

    @Operation(summary = "ping 一下")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("pong");
    }

    // 数 users 表行数顺便验 DB 连得通、schema/seed 跑过没
    @Operation(summary = "DB 探活 + 用户数")
    @GetMapping("/db")
    public Result<Long> db() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return Result.ok(count == null ? 0L : count);
    }
}
