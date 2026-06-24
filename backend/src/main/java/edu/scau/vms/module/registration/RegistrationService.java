package edu.scau.vms.module.registration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.constant.AuditStatus;
import edu.scau.vms.common.constant.AttendStatus;
import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.constant.MsgType;
import edu.scau.vms.common.constant.PublishStatus;
import edu.scau.vms.common.constant.RegStatus;
import edu.scau.vms.common.exception.BizException;
import edu.scau.vms.module.activity.entity.Activity;
import edu.scau.vms.module.activity.mapper.ActivityMapper;
import edu.scau.vms.module.attendance.entity.Attendance;
import edu.scau.vms.module.attendance.mapper.AttendanceMapper;
import edu.scau.vms.module.message.MessageService;
import edu.scau.vms.module.registration.dto.RegistrationVO;
import edu.scau.vms.module.registration.entity.Registration;
import edu.scau.vms.module.registration.mapper.RegistrationMapper;
import edu.scau.vms.module.user.UserService;
import edu.scau.vms.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 报名 + 审核。审核通过会顺手在 attendance 里建一条"未签到"的占位记录
// 这样组织者后面手动补签时不用再担心"找不到 attendance 行"
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final DateTimeFormatter ACT_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;
    private final AttendanceMapper attendanceMapper;
    private final UserService userService;
    private final MessageService messageService;

    @Transactional
    public Long apply(Long volunteerId, Long activityId) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在",
                "请刷新活动列表获取最新数据，该活动可能已被删除或下架。");
        if (!AuditStatus.APPROVED.equals(a.getAuditStatus()) || !PublishStatus.PUBLISHED.equals(a.getPublishStatus())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "该活动当前不开放报名",
                    "该活动可能未通过审核或已被组织者停止发布。您可以浏览其他正在招募志愿者的活动。");
        }
        if (a.getOrganizerId().equals(volunteerId)) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "不能报名自己发布的活动",
                    "您是该活动的组织者，无需报名。请切换到组织者面板查看和管理报名情况。");
        }
        LambdaQueryWrapper<Registration> qw = new LambdaQueryWrapper<>();
        qw.eq(Registration::getActivityId, activityId).eq(Registration::getVolunteerId, volunteerId);
        Registration exist = registrationMapper.selectOne(qw);
        // 只有"已取消"和"审核拒绝"才允许重新报；待审核/已通过都算占着位
        if (exist != null && !RegStatus.CANCELLED.equals(exist.getAuditStatus())
                && !RegStatus.REJECTED.equals(exist.getAuditStatus())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "您已报名该活动",
                    "您可以在「我的报名」中查看报名状态和审核进度。报名被拒或取消后可重新报名。");
        }
        long approved = countApproved(activityId);
        if (approved >= a.getCapacity()) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "活动报名人数已满",
                    "该活动名额已满。您可以关注其他类似活动，或联系活动组织者确认是否可能增加名额。");
        }
        if (exist != null) {
            // 之前取消/被拒过，复用同一行 reg_id 改回待审核
            // 不新建是因为 (activity_id, volunteer_id) 有 UNIQUE 约束，再 insert 会爆
            exist.setAuditStatus(RegStatus.PENDING);
            exist.setAuditedAt(null);
            exist.setAppliedAt(LocalDateTime.now());
            registrationMapper.updateById(exist);
            return exist.getRegId();
        }
        Registration r = new Registration();
        r.setActivityId(activityId);
        r.setVolunteerId(volunteerId);
        r.setAuditStatus(RegStatus.PENDING);
        r.setAppliedAt(LocalDateTime.now());
        registrationMapper.insert(r);
        return r.getRegId();
    }

    @Transactional
    public void cancel(Long regId, Long currentUserId) {
        Registration r = registrationMapper.selectById(regId);
        if (r == null) throw new BizException(ErrorCode.NOT_FOUND, "报名记录不存在",
                "请刷新报名列表获取最新数据，该记录可能已被删除。");
        if (!r.getVolunteerId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作他人报名",
                    "您只能取消自己的报名记录。");
        }
        if (!RegStatus.PENDING.equals(r.getAuditStatus())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "仅待审核报名可取消",
                    "报名已审核通过或已拒绝，无法取消。如已通过审核需要退出，请联系活动组织者。");
        }
        r.setAuditStatus(RegStatus.CANCELLED);
        registrationMapper.updateById(r);
    }

    @Transactional
    public void audit(Long regId, Long currentUserId, boolean isAdmin, boolean approve) {
        Registration r = registrationMapper.selectById(regId);
        if (r == null) throw new BizException(ErrorCode.NOT_FOUND, "报名记录不存在",
                "请刷新报名列表获取最新数据，该记录可能已被志愿者取消。");
        Activity a = activityMapper.selectById(r.getActivityId());
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在",
                "该活动可能已被删除，请联系系统管理员确认。");
        if (!isAdmin && !a.getOrganizerId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权审核他人活动报名",
                    "您只能审核自己发布活动的报名。请切换到对应活动的管理页面操作。");
        }
        if (!RegStatus.PENDING.equals(r.getAuditStatus())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "仅待审核报名可处理",
                    "该报名已审核过，请刷新列表获取最新状态。如需修改审核结果，请联系系统管理员。");
        }
        if (approve) {
            // 真审到通过那一刻再查一次容量，避免并发场景下越审越多
            long approved = countApproved(r.getActivityId());
            if (approved >= a.getCapacity()) {
                throw new BizException(ErrorCode.BIZ_CONFLICT, "活动报名人数已满",
                        "审核时名额已被其他人占满。您可以拒绝此报名，或联系管理员扩大活动名额后再操作。");
            }
            r.setAuditStatus(RegStatus.APPROVED);
            // 顺便占住 attendance 那行，后续签到/补签直接 update 就行
            ensureAttendance(r.getActivityId(), r.getVolunteerId());
        } else {
            r.setAuditStatus(RegStatus.REJECTED);
        }
        r.setAuditedAt(LocalDateTime.now());
        registrationMapper.updateById(r);

        messageService.sendDirect(r.getVolunteerId(), MsgType.REG_NOTICE,
                approve ? "报名审核通过" : "报名审核未通过",
                "您报名的【" + a.getTitle() + "】" + (approve ? "已审核通过，请按时签到。" : "未通过审核。"),
                "volunteer");

        // 通知组织者审核结果
        User applicant = userService.findById(r.getVolunteerId());
        messageService.sendDirect(a.getOrganizerId(), MsgType.REG_NOTICE,
                "报名审核已处理",
                "志愿者 " + (applicant != null ? applicant.getName() : "未知") + "（" + (applicant != null ? userService.formatUserNo(applicant) : "VOL-未知") + "）"
                        + "对【" + a.getTitle() + "】的报名已"
                        + (approve ? "通过" : "拒绝") + "。",
                "organizer");
    }

    // 志愿者看自己的报名记录，会带上 attendance 那行（签到时间/工时一起返）
    public PageResult<RegistrationVO> mine(Long volunteerId, Long page, Long size, String auditStatus) {
        LambdaQueryWrapper<Registration> qw = new LambdaQueryWrapper<>();
        qw.eq(Registration::getVolunteerId, volunteerId).orderByDesc(Registration::getAppliedAt);
        if (auditStatus != null && !auditStatus.isBlank()) qw.eq(Registration::getAuditStatus, auditStatus);
        Page<Registration> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<Registration> result = registrationMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), toVOs(result.getRecords(), true));
    }

    // 组织者看自己活动的报名，待审核排前面，方便一眼看到要处理的
    public PageResult<RegistrationVO> byActivity(Long activityId, Long page, Long size) {
        LambdaQueryWrapper<Registration> qw = new LambdaQueryWrapper<>();
        qw.eq(Registration::getActivityId, activityId)
          .orderByAsc(Registration::getAuditStatus)   // 待审核排前
          .orderByDesc(Registration::getAppliedAt);
        Page<Registration> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<Registration> result = registrationMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), toVOs(result.getRecords(), false));
    }

    // 报名审核通过时占住 attendance 一行；要是被反复审核就 noop
    private void ensureAttendance(Long activityId, Long volunteerId) {
        LambdaQueryWrapper<Attendance> qw = new LambdaQueryWrapper<>();
        qw.eq(Attendance::getActivityId, activityId).eq(Attendance::getVolunteerId, volunteerId);
        Attendance exist = attendanceMapper.selectOne(qw);
        if (exist != null) return;
        Attendance att = new Attendance();
        att.setActivityId(activityId);
        att.setVolunteerId(volunteerId);
        att.setServiceHours(0);
        att.setServiceMinutes(0);
        att.setStatus(AttendStatus.NOT_CHECKED_IN);
        attendanceMapper.insert(att);
    }

    private long countApproved(Long activityId) {
        LambdaQueryWrapper<Registration> qw = new LambdaQueryWrapper<>();
        qw.eq(Registration::getActivityId, activityId).eq(Registration::getAuditStatus, RegStatus.APPROVED);
        return registrationMapper.selectCount(qw);
    }

    // includeAttendance=true 时多 join 一次 attendance，给志愿者自己的页面用
    private List<RegistrationVO> toVOs(List<Registration> list, boolean includeAttendance) {
        if (list.isEmpty()) return List.of();
        List<Long> activityIds = list.stream().map(Registration::getActivityId).distinct().toList();
        List<Long> volunteerIds = list.stream().map(Registration::getVolunteerId).distinct().toList();

        Map<Long, Activity> actMap = new HashMap<>();
        for (Long aid : activityIds) {
            Activity a = activityMapper.selectById(aid);
            if (a != null) actMap.put(aid, a);
        }
        Map<Long, Long> enrolled = new HashMap<>();
        for (Long aid : activityIds) enrolled.put(aid, countApproved(aid));
        Map<Long, User> userMap = new HashMap<>();
        for (Long uid : volunteerIds) {
            User u = userService.findById(uid);
            if (u != null) userMap.put(uid, u);
        }
        Map<String, Attendance> attMap = new HashMap<>();
        if (includeAttendance) {
            LambdaQueryWrapper<Attendance> qw = new LambdaQueryWrapper<>();
            qw.in(Attendance::getActivityId, activityIds).in(Attendance::getVolunteerId, volunteerIds);
            for (Attendance att : attendanceMapper.selectList(qw)) {
                attMap.put(att.getActivityId() + ":" + att.getVolunteerId(), att);
            }
        }

        return list.stream().map(r -> {
            Activity a = actMap.get(r.getActivityId());
            User u = userMap.get(r.getVolunteerId());
            RegistrationVO.RegistrationVOBuilder b = RegistrationVO.builder()
                    .regId(r.getRegId())
                    .activityId(r.getActivityId())
                    .actNo(a == null ? null : a.getStartTime().format(ACT_NO_FMT) + String.format("%04d", a.getActivityId()))
                    .activityName(a == null ? null : a.getTitle())
                    .startTime(a == null ? null : a.getStartTime())
                    .endTime(a == null ? null : a.getEndTime())
                    .location(a == null ? null : a.getLocation())
                    .desc(a == null ? null : a.getDescription())
                    .limitNum(a == null ? null : a.getCapacity())
                    .enrolledNum(enrolled.getOrDefault(r.getActivityId(), 0L).intValue())
                    .volunteerId(r.getVolunteerId())
                    .volId(u == null ? null : "VOL-" + u.getUsername())
                    .volName(u == null ? null : u.getName())
                    .auditStatus(r.getAuditStatus())
                    .appliedAt(r.getAppliedAt())
                    .auditedAt(r.getAuditedAt());
            if (includeAttendance) {
                Attendance att = attMap.get(r.getActivityId() + ":" + r.getVolunteerId());
                if (att != null) {
                    b.checkInTime(att.getCheckInTime())
                     .checkOutTime(att.getCheckOutTime())
                     .hours(att.getServiceHours())
                     .minutes(att.getServiceMinutes());
                }
            }
            return b.build();
        }).collect(Collectors.toList());
    }

}
