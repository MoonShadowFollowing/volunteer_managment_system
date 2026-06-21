package edu.scau.vms.module.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.scau.vms.common.constant.AttendStatus;
import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.exception.BizException;
import edu.scau.vms.module.activity.entity.Activity;
import edu.scau.vms.module.activity.mapper.ActivityMapper;
import edu.scau.vms.module.attendance.entity.Attendance;
import edu.scau.vms.module.attendance.mapper.AttendanceMapper;
import edu.scau.vms.module.user.entity.User;
import edu.scau.vms.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 三张报表：个人工时、活动签到汇总、月度汇总
// 一律只算 status=已签退 的记录，没签退的不进表
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AttendanceMapper attendanceMapper;
    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;

    // ---- 个人工时 ----
    // 默认导自己，admin 可以导别人
    public byte[] personalHoursXlsx(Long targetUserId, Long currentUserId, boolean isAdmin) {
        if (!isAdmin && !targetUserId.equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权导出他人工时");
        }
        User u = userMapper.selectById(targetUserId);
        if (u == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");

        // 按签退时间排，xlsx 里就是时间顺序
        List<Attendance> records = attendanceMapper.selectList(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getVolunteerId, targetUserId)
                        .eq(Attendance::getStatus, AttendStatus.CHECKED_OUT)
                        .orderByAsc(Attendance::getCheckOutTime));

        // 一次性把活动信息捞回来，省得每行都查一次 DB
        Set<Long> aids = new HashSet<>();
        records.forEach(r -> aids.add(r.getActivityId()));
        Map<Long, Activity> actMap = batchActivities(aids);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("个人工时");
            CellStyle title = boldStyle(wb, 14);
            CellStyle head = headStyle(wb);
            CellStyle cell = borderStyle(wb);
            CellStyle total = totalStyle(wb);

            // 标题
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("个人志愿工时统计表 —— " + u.getName() + "（" + safe(u.getUsername()) + "）");
            c0.setCellStyle(title);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            // 表头
            String[] heads = {"序号", "活动编号", "活动名称", "服务日期", "认证小时", "认证分钟", "累计分钟"};
            Row hr = sheet.createRow(1);
            for (int i = 0; i < heads.length; i++) {
                Cell hc = hr.createCell(i);
                hc.setCellValue(heads[i]);
                hc.setCellStyle(head);
            }

            int rowIdx = 2;
            long totalMin = 0;
            for (int i = 0; i < records.size(); i++) {
                Attendance r = records.get(i);
                Activity a = actMap.get(r.getActivityId());
                long mins = (r.getServiceHours() == null ? 0 : r.getServiceHours()) * 60L
                        + (r.getServiceMinutes() == null ? 0 : r.getServiceMinutes());
                totalMin += mins;
                Row row = sheet.createRow(rowIdx++);
                setCell(row, 0, i + 1, cell);
                setCell(row, 1, a == null ? "-" : actNo(a), cell);
                setCell(row, 2, a == null ? "-" : safe(a.getTitle()), cell);
                setCell(row, 3, r.getCheckOutTime() == null ? "-" : r.getCheckOutTime().format(DATE_FMT), cell);
                setCell(row, 4, r.getServiceHours() == null ? 0 : r.getServiceHours(), cell);
                setCell(row, 5, r.getServiceMinutes() == null ? 0 : r.getServiceMinutes(), cell);
                setCell(row, 6, totalMin, cell);
            }

            // 合计行
            Row tr = sheet.createRow(rowIdx);
            Cell tc = tr.createCell(0);
            tc.setCellValue("合计");
            tc.setCellStyle(total);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 3));
            setCell(tr, 4, totalMin / 60, total);
            setCell(tr, 5, totalMin % 60, total);
            setCell(tr, 6, totalMin, total);

            autoSize(sheet, heads.length);
            wb.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("[ReportService] 个人工时 xlsx 生成失败 userId={}", targetUserId, e);
            throw new BizException(ErrorCode.SERVER_ERROR, "Excel 生成失败");
        }
    }

    // ---- 活动签到汇总 ----
    // 只有这个活动的组织者本人能导，admin 当然也能
    public byte[] activitySummaryXlsx(Long activityId, Long currentUserId, boolean isAdmin) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        if (!isAdmin && !a.getOrganizerId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权导出他人活动签到");
        }

        List<Attendance> records = attendanceMapper.selectList(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getActivityId, activityId)
                        .orderByAsc(Attendance::getRecordId));

        Set<Long> uids = new HashSet<>();
        records.forEach(r -> uids.add(r.getVolunteerId()));
        Map<Long, User> userMap = batchUsers(uids);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("活动签到汇总");
            CellStyle title = boldStyle(wb, 14);
            CellStyle head = headStyle(wb);
            CellStyle cell = borderStyle(wb);

            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("活动签到汇总 —— " + safe(a.getTitle()) + "（" + actNo(a) + "）");
            c0.setCellStyle(title);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            String[] heads = {"序号", "学号", "姓名", "签到时间", "签退时间", "状态", "认证小时", "认证分钟"};
            Row hr = sheet.createRow(1);
            for (int i = 0; i < heads.length; i++) {
                Cell hc = hr.createCell(i);
                hc.setCellValue(heads[i]);
                hc.setCellStyle(head);
            }

            int rowIdx = 2;
            for (int i = 0; i < records.size(); i++) {
                Attendance r = records.get(i);
                User vu = userMap.get(r.getVolunteerId());
                Row row = sheet.createRow(rowIdx++);
                setCell(row, 0, i + 1, cell);
                setCell(row, 1, vu == null ? "-" : safe(vu.getUsername()), cell);
                setCell(row, 2, vu == null ? "-" : safe(vu.getName()), cell);
                setCell(row, 3, r.getCheckInTime() == null ? "-" : r.getCheckInTime().format(DATETIME_FMT), cell);
                setCell(row, 4, r.getCheckOutTime() == null ? "-" : r.getCheckOutTime().format(DATETIME_FMT), cell);
                setCell(row, 5, safe(r.getStatus()), cell);
                setCell(row, 6, r.getServiceHours() == null ? 0 : r.getServiceHours(), cell);
                setCell(row, 7, r.getServiceMinutes() == null ? 0 : r.getServiceMinutes(), cell);
            }
            autoSize(sheet, heads.length);
            wb.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("[ReportService] 活动签到 xlsx 生成失败 activityId={}", activityId, e);
            throw new BizException(ErrorCode.SERVER_ERROR, "Excel 生成失败");
        }
    }

    // ---- 月度汇总 ----
    // admin 专属（Controller 那边卡了 @PreAuthorize），按签退月份归集
    public byte[] monthlyXlsx(int year, int month) {
        List<Row3> rows = aggregateMonthly(year, month);
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet(year + "年" + month + "月");
            CellStyle title = boldStyle(wb, 14);
            CellStyle head = headStyle(wb);
            CellStyle cell = borderStyle(wb);
            CellStyle total = totalStyle(wb);

            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue(year + " 年 " + month + " 月 志愿工时月度汇总");
            c0.setCellStyle(title);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            String[] heads = {"序号", "学号", "姓名", "参与活动数", "总小时", "总分钟"};
            Row hr = sheet.createRow(1);
            for (int i = 0; i < heads.length; i++) {
                Cell hc = hr.createCell(i);
                hc.setCellValue(heads[i]);
                hc.setCellStyle(head);
            }

            int rowIdx = 2;
            long sumMin = 0;
            int sumCnt = 0;
            for (int i = 0; i < rows.size(); i++) {
                Row3 r = rows.get(i);
                Row row = sheet.createRow(rowIdx++);
                setCell(row, 0, i + 1, cell);
                setCell(row, 1, safe(r.username), cell);
                setCell(row, 2, safe(r.name), cell);
                setCell(row, 3, r.actCount, cell);
                setCell(row, 4, r.totalMinutes / 60, cell);
                setCell(row, 5, r.totalMinutes % 60, cell);
                sumMin += r.totalMinutes;
                sumCnt += r.actCount;
            }

            Row tr = sheet.createRow(rowIdx);
            Cell tc = tr.createCell(0);
            tc.setCellValue("合计");
            tc.setCellStyle(total);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 2));
            setCell(tr, 3, sumCnt, total);
            setCell(tr, 4, sumMin / 60, total);
            setCell(tr, 5, sumMin % 60, total);

            autoSize(sheet, heads.length);
            wb.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("[ReportService] 月度 xlsx 生成失败 {}-{}", year, month, e);
            throw new BizException(ErrorCode.SERVER_ERROR, "Excel 生成失败");
        }
    }

    // 月度聚合：每个志愿者的活动数 + 总分钟，按总分钟降序
    public List<Row3> aggregateMonthly(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BizException(ErrorCode.PARAM_INVALID, "月份必须在 1~12 之间");
        }
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        // 用 [start, end) 半开区间，不用 between 是怕跨月边界出岔子
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<Attendance> records = attendanceMapper.selectList(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getStatus, AttendStatus.CHECKED_OUT)
                        .ge(Attendance::getCheckOutTime, start)
                        .lt(Attendance::getCheckOutTime, end));

        // 用 long[2] 当 tuple 凑合一下，[0]=活动数 [1]=总分钟
        Map<Long, long[]> agg = new HashMap<>();
        for (Attendance r : records) {
            long mins = (r.getServiceHours() == null ? 0 : r.getServiceHours()) * 60L
                    + (r.getServiceMinutes() == null ? 0 : r.getServiceMinutes());
            agg.computeIfAbsent(r.getVolunteerId(), k -> new long[2]);
            long[] cur = agg.get(r.getVolunteerId());
            cur[0] += 1;
            cur[1] += mins;
        }
        Map<Long, User> users = batchUsers(agg.keySet());
        List<Row3> rows = new ArrayList<>(agg.size());
        agg.forEach((uid, arr) -> {
            User u = users.get(uid);
            rows.add(new Row3(
                    u == null ? null : u.getUsername(),
                    u == null ? null : u.getName(),
                    (int) arr[0],
                    arr[1]));
        });
        // 按总分钟排，xlsx 出来天然是工时排行榜
        rows.sort((a, b) -> Long.compare(b.totalMinutes, a.totalMinutes));
        return rows;
    }

    // ---- 下面都是工具方法，POI 那一堆样式构造没啥好说的 ----

    private Map<Long, Activity> batchActivities(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        Map<Long, Activity> m = new HashMap<>();
        activityMapper.selectBatchIds(ids).forEach(a -> m.put(a.getActivityId(), a));
        return m;
    }

    private Map<Long, User> batchUsers(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        Map<Long, User> m = new HashMap<>();
        userMapper.selectBatchIds(ids).forEach(u -> m.put(u.getUserId(), u));
        return m;
    }

    private static String actNo(Activity a) {
        if (a == null || a.getStartTime() == null) return "-";
        return a.getStartTime().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", a.getActivityId());
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static void setCell(Row row, int idx, Object value, CellStyle style) {
        Cell c = row.createCell(idx);
        if (value instanceof Number n) {
            c.setCellValue(n.doubleValue());
        } else {
            c.setCellValue(value == null ? "" : value.toString());
        }
        if (style != null) c.setCellStyle(style);
    }

    private static void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) sheet.autoSizeColumn(i);
    }

    private static CellStyle boldStyle(Workbook wb, int size) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) size);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private static CellStyle headStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        addBorder(s);
        return s;
    }

    private static CellStyle borderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER);
        addBorder(s);
        return s;
    }

    private static CellStyle totalStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        addBorder(s);
        return s;
    }

    private static void addBorder(CellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    // 月度聚合单行；起这个名是因为只用在内部
    public static final class Row3 {
        public final String username;
        public final String name;
        public final int actCount;
        public final long totalMinutes;
        public Row3(String username, String name, int actCount, long totalMinutes) {
            this.username = username;
            this.name = name;
            this.actCount = actCount;
            this.totalMinutes = totalMinutes;
        }
    }
}
