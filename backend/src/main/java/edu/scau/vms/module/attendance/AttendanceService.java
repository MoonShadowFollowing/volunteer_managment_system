package edu.scau.vms.module.attendance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.constant.AttendStatus;
import edu.scau.vms.common.constant.CertStatus;
import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.constant.MsgType;
import edu.scau.vms.common.constant.PublishStatus;
import edu.scau.vms.common.exception.BizException;
import edu.scau.vms.module.activity.entity.Activity;
import edu.scau.vms.module.activity.mapper.ActivityMapper;
import edu.scau.vms.module.attendance.dto.AttendanceVO;
import edu.scau.vms.module.attendance.dto.HoursRequest;
import edu.scau.vms.module.attendance.dto.ManualSignRequest;
import edu.scau.vms.module.attendance.entity.Attendance;
import edu.scau.vms.module.attendance.mapper.AttendanceMapper;
import edu.scau.vms.module.certificate.entity.Certificate;
import edu.scau.vms.module.certificate.mapper.CertificateMapper;
import edu.scau.vms.module.message.MessageService;
import edu.scau.vms.module.user.UserService;
import edu.scau.vms.module.user.entity.User;
import edu.scau.vms.module.registration.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 签到/志愿时这块是核心副作用集中地：
//   - 改工时 → 联动 certificates（>0 复活/发证，=0 失效）
//   - 补签 → 顺便发个证
// 改这俩方法之前先想清楚证书的状态机别打乱了
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final DateTimeFormatter ACT_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AttendanceMapper attendanceMapper;
    private final ActivityMapper activityMapper;
    private final CertificateMapper certificateMapper;
    private final UserService userService;
    private final MessageService messageService;

    public PageResult<AttendanceVO> byActivity(Long activityId, Long page, Long size, Long currentUserId, boolean isAdmin) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        if (!isAdmin && !a.getOrganizerId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看他人活动签到");
        }
        LambdaQueryWrapper<Attendance> qw = new LambdaQueryWrapper<>();
        qw.eq(Attendance::getActivityId, activityId).orderByAsc(Attendance::getRecordId);
        Page<Attendance> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<Attendance> result = attendanceMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), toVOs(result.getRecords()));
    }

    @Transactional
    public void updateHours(Long recordId, Long currentUserId, boolean isAdmin, HoursRequest req) {
        Attendance att = attendanceMapper.selectById(recordId);
        if (att == null) throw new BizException(ErrorCode.NOT_FOUND, "签到记录不存在");
        Activity a = activityMapper.selectById(att.getActivityId());
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        if (!isAdmin && !a.getOrganizerId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权修改他人活动志愿时");
        }
        // 校验工时不能为负数
        if (req.getHours() < 0 || req.getMinutes() < 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "工时不能为负数");
        }
        if (req.getMinutes() > 59) {
            throw new BizException(ErrorCode.PARAM_INVALID, "分钟数不能超过 59");
        }
        // 校验工时不能超过活动总时长
        long totalMinutes = req.getHours() * 60L + req.getMinutes();
        long activityMinutes = java.time.Duration.between(a.getStartTime(), a.getEndTime()).toMinutes();
        if (totalMinutes > activityMinutes) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    String.format("志愿时不能超过活动总时长（%d小时%d分钟）",
                            activityMinutes / 60, activityMinutes % 60));
        }
        att.setServiceHours(req.getHours());
        att.setServiceMinutes(req.getMinutes());
        attendanceMapper.updateById(att);

        // 联动证书：>0 → 自动发证/复活；=0 → 失效已发证书。
        syncCertificate(att.getActivityId(), att.getVolunteerId(), a, req.getHours(), req.getMinutes());

        boolean hasHours = req.getHours() > 0 || req.getMinutes() > 0;
        messageService.sendDirect(att.getVolunteerId(), MsgType.ACTIVITY_NOTICE,
                "志愿时已更新 - " + a.getTitle(),
                "您在活动【" + a.getTitle() + "】中的志愿时已被管理员更新为 "
                        + req.getHours() + " 小时 " + req.getMinutes() + " 分钟。"
                        + (hasHours ? "您的志愿服务证书已同步更新。" : "注意：由于工时归零，相关证书已失效。"),
                "volunteer");
    }

    // 漏签退/异常时组织者手动补一刀，时间和工时都自己填
    @Transactional
    public void manualSign(Long recordId, Long currentUserId, boolean isAdmin, ManualSignRequest req) {
        if (req.getMinutes() < 0 || req.getMinutes() > 59 || req.getHours() < 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "工时格式不合法");
        }
        if (!req.getCheckOutTime().isAfter(req.getCheckInTime())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "签退时间须晚于签到时间");
        }
        Attendance att = attendanceMapper.selectById(recordId);
        if (att == null) throw new BizException(ErrorCode.NOT_FOUND, "签到记录不存在");
        Activity a = activityMapper.selectById(att.getActivityId());
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        if (!isAdmin && !a.getOrganizerId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作他人活动签到");
        }
        // 校验工时不能超过活动总时长
        long totalMinutes = req.getHours() * 60L + req.getMinutes();
        long activityMinutes = java.time.Duration.between(a.getStartTime(), a.getEndTime()).toMinutes();
        if (totalMinutes > activityMinutes) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    String.format("志愿时不能超过活动总时长（%d小时%d分钟）",
                            activityMinutes / 60, activityMinutes % 60));
        }
        att.setCheckInTime(req.getCheckInTime());
        att.setCheckOutTime(req.getCheckOutTime());
        att.setServiceHours(req.getHours());
        att.setServiceMinutes(req.getMinutes());
        att.setStatus(AttendStatus.CHECKED_OUT);
        attendanceMapper.updateById(att);

        // 补签如果有工时就顺便发/续证书
        syncCertificate(att.getActivityId(), att.getVolunteerId(), a, req.getHours(), req.getMinutes());

        User vol = userService.findById(att.getVolunteerId());
        messageService.sendDirect(att.getVolunteerId(), MsgType.ACTIVITY_NOTICE,
                "补签成功 - " + a.getTitle(),
                "您在活动【" + a.getTitle() + "】中已由管理员手动补签，志愿时登记为 "
                        + req.getHours() + " 小时 " + req.getMinutes() + " 分钟。",
                "volunteer");
    }

    // 志愿者自助签到
    @Transactional
    public void checkIn(Long activityId, Long volunteerId) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        if (!PublishStatus.PUBLISHED.equals(a.getPublishStatus())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "活动未发布或已停止，无法签到");
        }

        LambdaQueryWrapper<Attendance> qw = new LambdaQueryWrapper<>();
        qw.eq(Attendance::getActivityId, activityId).eq(Attendance::getVolunteerId, volunteerId);
        Attendance att = attendanceMapper.selectOne(qw);
        if (att == null) throw new BizException(ErrorCode.BIZ_CONFLICT, "没有已通过的报名记录，无法签到");
        if (!AttendStatus.NOT_CHECKED_IN.equals(att.getStatus())) {
            throw new BizException(ErrorCode.ALREADY_CHECKED_IN,
                    AttendStatus.CHECKED_IN.equals(att.getStatus()) ? "已签到，无需重复签到" : "当前状态不允许签到");
        }

        att.setCheckInTime(LocalDateTime.now());
        att.setStatus(AttendStatus.CHECKED_IN);
        attendanceMapper.updateById(att);
    }

    // 志愿者自助签退，自动算工时并发证
    @Transactional
    public void checkOut(Long activityId, Long volunteerId) {
        LambdaQueryWrapper<Attendance> qw = new LambdaQueryWrapper<>();
        qw.eq(Attendance::getActivityId, activityId).eq(Attendance::getVolunteerId, volunteerId);
        Attendance att = attendanceMapper.selectOne(qw);
        if (att == null) throw new BizException(ErrorCode.BIZ_CONFLICT, "没有签到记录");
        if (!AttendStatus.CHECKED_IN.equals(att.getStatus())) {
            throw new BizException(ErrorCode.NOT_CHECKED_IN,
                    AttendStatus.NOT_CHECKED_IN.equals(att.getStatus()) ? "请先签到" : "当前状态不允许签退");
        }

        Activity a = activityMapper.selectById(att.getActivityId());
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");

        LocalDateTime now = LocalDateTime.now();
        att.setCheckOutTime(now);
        long totalMinutes = java.time.Duration.between(att.getCheckInTime(), now).toMinutes();
        if (totalMinutes < 0) totalMinutes = 0;
        int hours = (int) (totalMinutes / 60);
        int minutes = (int) (totalMinutes % 60);
        att.setServiceHours(hours);
        att.setServiceMinutes(minutes);
        att.setStatus(AttendStatus.CHECKED_OUT);
        attendanceMapper.updateById(att);

        syncCertificate(att.getActivityId(), att.getVolunteerId(), a, hours, minutes);

        messageService.sendDirect(att.getVolunteerId(), MsgType.ACTIVITY_NOTICE,
                "签退成功 - " + a.getTitle(),
                "您在活动【" + a.getTitle() + "】中已成功签退，志愿时登记为 "
                        + hours + " 小时 " + minutes + " 分钟。",
                "volunteer");
    }

    // >0 发证/复活，=0 失效已发证书
    private void syncCertificate(Long activityId, Long volunteerId, Activity a, int hours, int minutes) {
        boolean hasHours = hours > 0 || minutes > 0;
        Certificate cert = findCert(activityId, volunteerId);
        if (hasHours) {
            if (cert == null) {
                cert = new Certificate();
                cert.setTitle(a.getTitle() + "志愿服务证明");
                cert.setActivityId(activityId);
                cert.setVolunteerId(volunteerId);
                cert.setStartTime(a.getStartTime());
                cert.setEndTime(a.getEndTime());
                cert.setCertHours(hours);
                cert.setCertMinutes(minutes);
                cert.setIssuedDate(LocalDate.now());
                cert.setStatus(CertStatus.VALID);
                certificateMapper.insert(cert);
            } else {
                cert.setCertHours(hours);
                cert.setCertMinutes(minutes);
                cert.setStatus(CertStatus.VALID);
                certificateMapper.updateById(cert);
            }
        } else if (cert != null) {
            cert.setCertHours(0);
            cert.setCertMinutes(0);
            cert.setStatus(CertStatus.INVALID);
            certificateMapper.updateById(cert);
        }
    }

    private Certificate findCert(Long activityId, Long volunteerId) {
        LambdaQueryWrapper<Certificate> qw = new LambdaQueryWrapper<>();
        qw.eq(Certificate::getActivityId, activityId).eq(Certificate::getVolunteerId, volunteerId);
        return certificateMapper.selectOne(qw);
    }

    // entity → VO，顺便把活动名和志愿者编号都填上
    // TODO 这俩 for 是 N 次 selectById，要是分页页数大可以改 selectBatchIds，目前 pageSize≤500 凑合够用
    private List<AttendanceVO> toVOs(List<Attendance> list) {
        if (list.isEmpty()) return List.of();
        List<Long> activityIds = list.stream().map(Attendance::getActivityId).distinct().toList();
        List<Long> userIds = list.stream().map(Attendance::getVolunteerId).distinct().toList();
        Map<Long, Activity> actMap = new HashMap<>();
        for (Long aid : activityIds) {
            Activity a = activityMapper.selectById(aid);
            if (a != null) actMap.put(aid, a);
        }
        Map<Long, User> userMap = new HashMap<>();
        for (Long uid : userIds) {
            User u = userService.findById(uid);
            if (u != null) userMap.put(uid, u);
        }
        return list.stream().map(att -> {
            Activity a = actMap.get(att.getActivityId());
            User u = userMap.get(att.getVolunteerId());
            return AttendanceVO.builder()
                    .recordId(att.getRecordId())
                    .activityId(att.getActivityId())
                    .actNo(a == null ? null : a.getStartTime().format(ACT_NO_FMT) + String.format("%04d", a.getActivityId()))
                    .activityName(a == null ? null : a.getTitle())
                    .volunteerId(att.getVolunteerId())
                    .volId(u == null ? null : "VOL-" + u.getUsername())
                    .volName(u == null ? null : u.getName())
                    .checkInTime(att.getCheckInTime())
                    .checkOutTime(att.getCheckOutTime())
                    .hours(att.getServiceHours())
                    .minutes(att.getServiceMinutes())
                    .status(att.getStatus())
                    .signStatus(toSignStatus(att.getStatus()))
                    .build();
        }).collect(Collectors.toList());
    }

    // DB 里写"已签退"，前端列表里更喜欢"正常"两个字，其它状态原样透传
    private String toSignStatus(String dbStatus) {
        return AttendStatus.CHECKED_OUT.equals(dbStatus) ? "正常" : dbStatus;
    }
}
