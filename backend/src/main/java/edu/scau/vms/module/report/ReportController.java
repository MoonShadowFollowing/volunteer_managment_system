package edu.scau.vms.module.report;

import edu.scau.vms.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// 三个导出接口，返回都是 xlsx 字节流，前端 axios 用 blob 接
@Tag(name = "Report", description = "报表导出（Excel）")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    // xlsx 的 MIME，写一次免得每个方法都重复
    private static final MediaType XLSX_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService reportService;

    // 不传 volunteerId 就是导自己，admin 可以传别人的 id
    @Operation(summary = "个人志愿工时表")
    @GetMapping("/personal-hours.xlsx")
    public ResponseEntity<ByteArrayResource> personalHoursXlsx(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false) Long volunteerId) {
        Long target = volunteerId == null ? me.userId() : volunteerId;
        byte[] data = reportService.personalHoursXlsx(target, me.userId(), me.admin());
        return xlsxResponse(data, "VMS-PersonalHours-" + target + ".xlsx");
    }

    @Operation(summary = "活动签到汇总表")
    @GetMapping("/activity-summary.xlsx")
    @PreAuthorize("hasAuthority('ORG') or hasAuthority('ADM')")
    public ResponseEntity<ByteArrayResource> activitySummaryXlsx(
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam Long activityId) {
        byte[] data = reportService.activitySummaryXlsx(activityId, me.userId(), me.admin());
        return xlsxResponse(data, "VMS-Activity-" + activityId + ".xlsx");
    }

    @Operation(summary = "月度全院汇总表")
    @GetMapping("/monthly.xlsx")
    @PreAuthorize("hasAuthority('ADM')")
    public ResponseEntity<ByteArrayResource> monthlyXlsx(
            @RequestParam int year,
            @RequestParam int month) {
        byte[] data = reportService.monthlyXlsx(year, month);
        return xlsxResponse(data, "VMS-Monthly-" + year + "-" + String.format("%02d", month) + ".xlsx");
    }

    private static ResponseEntity<ByteArrayResource> xlsxResponse(byte[] bytes, String fileName) {
        ByteArrayResource resource = new ByteArrayResource(bytes);
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(XLSX_TYPE)
                .contentLength(bytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
