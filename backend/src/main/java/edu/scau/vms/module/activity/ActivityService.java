package edu.scau.vms.module.activity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.constant.AuditStatus;
import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.constant.PublishStatus;
import edu.scau.vms.common.constant.RegStatus;
import edu.scau.vms.common.exception.BizException;
import edu.scau.vms.module.activity.dto.ActivitySaveRequest;
import edu.scau.vms.module.activity.dto.ActivityVO;
import edu.scau.vms.module.activity.entity.Activity;
import edu.scau.vms.module.activity.mapper.ActivityMapper;
import edu.scau.vms.module.registration.entity.Registration;
import edu.scau.vms.module.registration.mapper.RegistrationMapper;
import edu.scau.vms.module.user.UserService;
import edu.scau.vms.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 活动的 CRUD + 两层状态（审核状态 × 发布状态），改逻辑时记得：
//   - 组织者改了活动 → 重新走审核，发布开关回 STOPPED
//   - 只有 APPROVED 的活动志愿者才看得见
@Service
@RequiredArgsConstructor
public class ActivityService {

    private static final DateTimeFormatter ACT_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final UserService userService;

    public PageResult<ActivityVO> list(Long page, Long size, String name, LocalDate startDate, LocalDate endDate,
                                       String auditStatus, String publishStatus, Long organizerId, boolean volunteerView) {
        LambdaQueryWrapper<Activity> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) qw.like(Activity::getTitle, name);
        if (startDate != null) qw.ge(Activity::getStartTime, startDate.atStartOfDay());
        if (endDate != null) qw.le(Activity::getStartTime, endDate.atTime(23, 59, 59));
        if (auditStatus != null && !auditStatus.isBlank()) qw.eq(Activity::getAuditStatus, auditStatus);
        if (publishStatus != null && !publishStatus.isBlank()) qw.eq(Activity::getPublishStatus, publishStatus);
        if (organizerId != null) qw.eq(Activity::getOrganizerId, organizerId);
        // volunteerView=true 是给志愿者列表用的，强制只看"审核通过+发布中"
        if (volunteerView) {
            qw.eq(Activity::getAuditStatus, AuditStatus.APPROVED)
              .eq(Activity::getPublishStatus, PublishStatus.PUBLISHED);
        }
        // 管理员/组织者列表里待审核的置顶，剩下的按开始时间倒序
        // 直接拼 CASE 是因为 LambdaQueryWrapper 不太好表达这种条件排序
        qw.last("ORDER BY CASE WHEN audit_status = '待审核' THEN 0 ELSE 1 END, start_time DESC");

        Page<Activity> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<Activity> result = activityMapper.selectPage(p, qw);
        List<ActivityVO> vos = toVOs(result.getRecords());
        return PageResult.of(result.getTotal(), vos);
    }

    public ActivityVO detail(Long activityId) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        return toVOs(List.of(a)).get(0);
    }

    @Transactional
    public Long create(Long organizerId, ActivitySaveRequest req) {
        validateTime(req.getStartTime(), req.getEndTime());
        Activity a = new Activity();
        a.setTitle(req.getName());
        a.setDescription(req.getDesc());
        a.setLocation(req.getLocation());
        a.setStartTime(req.getStartTime());
        a.setEndTime(req.getEndTime());
        a.setCapacity(req.getLimitNum());
        a.setOrganizerId(organizerId);
        a.setAuditStatus(AuditStatus.PENDING);
        a.setPublishStatus(PublishStatus.STOPPED);
        activityMapper.insert(a);
        return a.getActivityId();
    }

    @Transactional
    public void update(Long activityId, Long currentUserId, boolean isAdmin, ActivitySaveRequest req) {
        Activity a = mustOwn(activityId, currentUserId, isAdmin);
        validateTime(req.getStartTime(), req.getEndTime());
        a.setTitle(req.getName());
        a.setDescription(req.getDesc());
        a.setLocation(req.getLocation());
        a.setStartTime(req.getStartTime());
        a.setEndTime(req.getEndTime());
        a.setCapacity(req.getLimitNum());
        // 修改后重新审核：审核状态回到待审核，发布状态停止
        a.setAuditStatus(AuditStatus.PENDING);
        a.setPublishStatus(PublishStatus.STOPPED);
        activityMapper.updateById(a);
    }

    @Transactional
    public void delete(Long activityId, Long currentUserId, boolean isAdmin) {
        mustOwn(activityId, currentUserId, isAdmin);
        activityMapper.deleteById(activityId);
    }

    @Transactional
    public void audit(Long activityId, boolean approve) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        a.setAuditStatus(approve ? AuditStatus.APPROVED : AuditStatus.REJECTED);
        if (!approve) a.setPublishStatus(PublishStatus.STOPPED);
        activityMapper.updateById(a);
    }

    // 发布开关：必须先审核通过才能"发布中"，停了能再开
    @Transactional
    public void togglePublish(Long activityId, Long currentUserId, boolean isAdmin, boolean publish) {
        Activity a = mustOwn(activityId, currentUserId, isAdmin);
        if (publish && !AuditStatus.APPROVED.equals(a.getAuditStatus())) {
            throw new BizException(ErrorCode.ACTIVITY_NOT_AUDITED, "活动尚未通过审核，无法发布");
        }
        a.setPublishStatus(publish ? PublishStatus.PUBLISHED : PublishStatus.STOPPED);
        activityMapper.updateById(a);
    }

    // 老朋友越权防护：组织者只能动自己的活动，admin 不限
    private Activity mustOwn(Long activityId, Long currentUserId, boolean isAdmin) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) throw new BizException(ErrorCode.NOT_FOUND, "活动不存在");
        if (!isAdmin && !a.getOrganizerId().equals(currentUserId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作他人活动");
        }
        return a;
    }

    private void validateTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "活动结束时间必须晚于开始时间");
        }
    }

    private List<ActivityVO> toVOs(List<Activity> list) {
        if (list.isEmpty()) return List.of();
        List<Long> ids = list.stream().map(Activity::getActivityId).toList();
        List<Long> organizerIds = list.stream().map(Activity::getOrganizerId).distinct().toList();

        Map<Long, Long> enrolledCount = countApprovedRegistrations(ids);

        // organizer 通常就一个或几个，循环查就行，不值得 batch
        Map<Long, String> organizerNames = new HashMap<>();
        for (Long uid : organizerIds) {
            User u = userService.findById(uid);
            organizerNames.put(uid, u == null ? "未知" : u.getName());
        }

        return list.stream().map(a -> ActivityVO.builder()
                .activityId(a.getActivityId())
                .actNo(formatActNo(a))
                .name(a.getTitle())
                .desc(a.getDescription())
                .location(a.getLocation())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .limitNum(a.getCapacity())
                .enrolledNum(enrolledCount.getOrDefault(a.getActivityId(), 0L).intValue())
                .organizerId(a.getOrganizerId())
                .organizerName(organizerNames.get(a.getOrganizerId()))
                .auditStatus(a.getAuditStatus())
                .publishStatus(a.getPublishStatus())
                .isPublished(PublishStatus.PUBLISHED.equals(a.getPublishStatus()))
                .build()).collect(Collectors.toList());
    }

    private Map<Long, Long> countApprovedRegistrations(List<Long> activityIds) {
        if (activityIds.isEmpty()) return Map.of();
        LambdaQueryWrapper<Registration> qw = new LambdaQueryWrapper<>();
        qw.in(Registration::getActivityId, activityIds)
          .eq(Registration::getAuditStatus, RegStatus.APPROVED);
        List<Registration> regs = registrationMapper.selectList(qw);
        return regs.stream().collect(Collectors.groupingBy(Registration::getActivityId, Collectors.counting()));
    }

    private String formatActNo(Activity a) {
        return a.getStartTime().format(ACT_NO_FMT) + String.format("%04d", a.getActivityId());
    }
}
