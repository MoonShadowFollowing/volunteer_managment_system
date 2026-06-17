package edu.scau.vms.module.stat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.scau.vms.common.constant.AuditStatus;
import edu.scau.vms.common.constant.CertStatus;
import edu.scau.vms.common.constant.PublishStatus;
import edu.scau.vms.common.constant.RegStatus;
import edu.scau.vms.common.constant.Role;
import edu.scau.vms.common.security.UserPrincipal;
import edu.scau.vms.module.activity.entity.Activity;
import edu.scau.vms.module.activity.mapper.ActivityMapper;
import edu.scau.vms.module.attendance.entity.Attendance;
import edu.scau.vms.module.attendance.mapper.AttendanceMapper;
import edu.scau.vms.module.certificate.entity.Certificate;
import edu.scau.vms.module.certificate.mapper.CertificateMapper;
import edu.scau.vms.module.registration.entity.Registration;
import edu.scau.vms.module.registration.mapper.RegistrationMapper;
import edu.scau.vms.module.stat.dto.DashboardStats;
import edu.scau.vms.module.user.entity.User;
import edu.scau.vms.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatService {

    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final AttendanceMapper attendanceMapper;
    private final CertificateMapper certificateMapper;

    public DashboardStats forUser(UserPrincipal me) {
        String role = me.role();
        Map<String, Object> m = new LinkedHashMap<>();
        // 管理员 ⊇ 组织者 ⊇ 志愿者（递进关系），必须按高权限优先判断
        if (Role.SUPERADMIN.equals(role)) {
            superMetrics(m);
        } else if (Role.ADMIN.equals(role) || me.admin()) {
            adminMetrics(m);
        } else if (me.organizer()) {
            organizerMetrics(me.userId(), m);
        } else {
            volunteerMetrics(me.userId(), m);
        }
        return DashboardStats.builder().role(role).metrics(m).build();
    }

    private void volunteerMetrics(Long userId, Map<String, Object> m) {
        List<Attendance> myAtts = attendanceMapper.selectList(
                new LambdaQueryWrapper<Attendance>().eq(Attendance::getVolunteerId, userId));
        int totalMinutes = myAtts.stream()
                .mapToInt(a -> a.getServiceHours() * 60 + a.getServiceMinutes())
                .sum();
        m.put("totalHours", totalMinutes / 60);
        m.put("totalMinutes", totalMinutes % 60);

        long certCount = certificateMapper.selectCount(
                new LambdaQueryWrapper<Certificate>()
                        .eq(Certificate::getVolunteerId, userId)
                        .eq(Certificate::getStatus, CertStatus.VALID));
        m.put("certificateCount", certCount);

        long appliedCount = registrationMapper.selectCount(
                new LambdaQueryWrapper<Registration>()
                        .eq(Registration::getVolunteerId, userId)
                        .eq(Registration::getAuditStatus, RegStatus.APPROVED));
        m.put("approvedRegCount", appliedCount);
    }

    private void organizerMetrics(Long userId, Map<String, Object> m) {
        long mine = activityMapper.selectCount(
                new LambdaQueryWrapper<Activity>().eq(Activity::getOrganizerId, userId));
        m.put("myActivityCount", mine);

        List<Activity> mineList = activityMapper.selectList(
                new LambdaQueryWrapper<Activity>().eq(Activity::getOrganizerId, userId));
        List<Long> ids = mineList.stream().map(Activity::getActivityId).toList();
        long pendingRegs = ids.isEmpty() ? 0 :
                registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                        .in(Registration::getActivityId, ids)
                        .eq(Registration::getAuditStatus, RegStatus.PENDING));
        m.put("pendingRegCount", pendingRegs);

        long published = mineList.stream().filter(a -> PublishStatus.PUBLISHED.equals(a.getPublishStatus())).count();
        m.put("publishedActivityCount", published);
    }

    private void adminMetrics(Map<String, Object> m) {
        m.put("totalActivities", activityMapper.selectCount(null));
        m.put("totalVolunteers", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, Role.VOLUNTEER)));
        m.put("totalCertificates", certificateMapper.selectCount(
                new LambdaQueryWrapper<Certificate>().eq(Certificate::getStatus, CertStatus.VALID)));
        m.put("pendingActivityAudits", activityMapper.selectCount(
                new LambdaQueryWrapper<Activity>().eq(Activity::getAuditStatus, AuditStatus.PENDING)));

        List<Attendance> all = attendanceMapper.selectList(null);
        int totalMinutes = all.stream()
                .mapToInt(a -> a.getServiceHours() * 60 + a.getServiceMinutes())
                .sum();
        m.put("totalServiceHours", totalMinutes / 60);
    }

    private void superMetrics(Map<String, Object> m) {
        m.put("totalAdmins", userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getIsAdmin, true)));
        m.put("totalUsers", userMapper.selectCount(null));
    }
}
