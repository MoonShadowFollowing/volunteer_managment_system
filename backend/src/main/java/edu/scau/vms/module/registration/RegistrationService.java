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
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        if (!AuditStatus.APPROVED.equals(a.getAuditStatus()) || !PublishStatus.PUBLISHED.equals(a.getPublishStatus())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "该活动当前不开放报名");
        }
        if (a.getOrganizerId().equals(volunteerId)) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "不能报名自己发布的活动");
        }
        LambdaQueryWrapper<Registration> qw = new LambdaQueryWrapper<>();
        qw.eq(Registration::getActivityId, activityId).eq(Registration::getVolunteerId, volunteerId);
        Registration exist = registrationMapper.selectOne(qw);
        if (exist != null && !RegStatus.CANCELLED.equals(exist.getAuditStatus())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "您已报名该活动");
        }
        long approved = countApproved(activityId);
        if (approved >= a.getCapacity()) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "活动报名人数已满");
        }
        if (exist != null) {
            // 之前取消过，复用记录改回待审核
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
        if (r == null) throw new BizException(ErrorCode.NOT_FOUND, "报名记录不存在");
        if (!r.getVolunteerId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作他人报名");
        }
        if (!RegStatus.PENDING.equals(r.getAuditStatus())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "仅待审核报名可取消");
        }
        r.setAuditStatus(RegStatus.CANCELLED);
        registrationMapper.updateById(r);
    }

    @Transactional
    public void audit(Long regId, Long currentUserId, boolean isAdmin, boolean approve) {
        Registration r = registrationMapper.selectById(regId);
        if (r == null) throw new BizException(ErrorCode.NOT_FOUND, "报名记录不存在");
        Activity a = activityMapper.selectById(r.getActivityId());
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        if (!isAdmin && !a.getOrganizerId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权审核他人活动报名");
        }
        if (!RegStatus.PENDING.equals(r.getAuditStatus())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "仅待审核报名可处理");
        }
        if (approve) {
            long approved = countApproved(r.getActivityId());
            if (approved >= a.getCapacity()) {
                throw new BizException(ErrorCode.BIZ_CONFLICT, "活动报名人数已满");
            }
            r.setAuditStatus(RegStatus.APPROVED);
            ensureAttendance(r.getActivityId(), r.getVolunteerId());
        } else {
            r.setAuditStatus(RegStatus.REJECTED);
        }
        r.setAuditedAt(LocalDateTime.now());
        registrationMapper.updateById(r);

        messageService.sendDirect(r.getVolunteerId(), MsgType.REG_NOTICE,
                approve ? "报名审核通过" : "报名审核未通过",
                "您报名的【" + a.getTitle() + "】" + (approve ? "已审核通过，请按时签到。" : "未通过审核。"));
    }

    /** 志愿者：我的已报名列表 */
    public PageResult<RegistrationVO> mine(Long volunteerId, Long page, Long size, String auditStatus) {
        LambdaQueryWrapper<Registration> qw = new LambdaQueryWrapper<>();
        qw.eq(Registration::getVolunteerId, volunteerId).orderByDesc(Registration::getAppliedAt);
        if (auditStatus != null && !auditStatus.isBlank()) qw.eq(Registration::getAuditStatus, auditStatus);
        Page<Registration> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<Registration> result = registrationMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), toVOs(result.getRecords(), true));
    }

    /** 组织者：某活动的报名列表 */
    public PageResult<RegistrationVO> byActivity(Long activityId, Long page, Long size) {
        LambdaQueryWrapper<Registration> qw = new LambdaQueryWrapper<>();
        qw.eq(Registration::getActivityId, activityId)
          .orderByAsc(Registration::getAuditStatus)   // 待审核排前
          .orderByDesc(Registration::getAppliedAt);
        Page<Registration> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<Registration> result = registrationMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), toVOs(result.getRecords(), false));
    }

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
                    .volId(formatVolId(r.getVolunteerId()))
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

    public static String formatVolId(Long userId) {
        return "VOL-" + String.format("%05d", userId);
    }
}
