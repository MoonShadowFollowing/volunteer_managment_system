package edu.scau.vms.module.external;

import edu.scau.vms.common.Result;
import edu.scau.vms.common.constant.AttendStatus;
import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.exception.BizException;
import edu.scau.vms.module.external.dto.PublicHoursVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

// 对外 API：综合测评系统按学号查累计工时
// SecurityConfig 把 /api/public/** 放白名单了，所以这里完全没鉴权
// 真上线前建议至少加个 API Key 头检查，不然谁都能扫
@Tag(name = "PublicApi", description = "对外开放接口")
@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicApiController {

    private final JdbcTemplate jdbc;

    @Operation(summary = "按学号查询累计认证服务工时（综测系统集成）")
    @GetMapping("/hours")
    public Result<PublicHoursVO> hours(
            @Parameter(description = "学号（对应 users.username）", required = true, example = "20240001")
            @RequestParam("studentId") String studentId) {

        if (studentId == null || studentId.trim().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "studentId 不能为空");
        }
        String sid = studentId.trim();

        Map<String, Object> userRow;
        try {
            userRow = jdbc.queryForMap(
                    "SELECT user_id, name FROM users WHERE username = ?", sid);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new BizException(ErrorCode.NOT_FOUND, "未找到学号对应的志愿者：" + sid);
        }
        Long userId = ((Number) userRow.get("user_id")).longValue();
        String name = (String) userRow.get("name");

        // 只算已签退 + 工时>0 的，未完成或归零的统统不算
        Map<String, Object> agg = jdbc.queryForMap(
                "SELECT COALESCE(SUM(service_hours),0) AS h, " +
                "       COALESCE(SUM(service_minutes),0) AS m, " +
                "       COUNT(*) AS cnt " +
                "FROM attendance " +
                "WHERE volunteer_id = ? AND status = ? " +
                "  AND (service_hours > 0 OR service_minutes > 0)",
                userId, AttendStatus.CHECKED_OUT);

        long totalH = ((Number) agg.get("h")).longValue();
        long totalM = ((Number) agg.get("m")).longValue();
        int cnt = ((Number) agg.get("cnt")).intValue();

        // 分钟可能加爆 60，进位到小时一下
        totalH += totalM / 60;
        totalM = totalM % 60;

        PublicHoursVO vo = PublicHoursVO.builder()
                .studentId(sid)
                .studentName(name)
                .totalHours((int) totalH)
                .totalMinutes((int) totalM)
                .formattedDuration(totalH + "小时" + totalM + "分钟")
                .certifiedActivityCount(cnt)
                .generatedAt(LocalDateTime.now())
                .build();

        log.info("[PublicApi] hours studentId={} → {}小时{}分钟（{}条认证）", sid, totalH, totalM, cnt);
        return Result.ok(vo);
    }
}
