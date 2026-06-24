package edu.scau.vms.module.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.scau.vms.common.constant.Role;
import edu.scau.vms.module.integration.dto.EduValidateResult;
import edu.scau.vms.module.integration.dto.SyncResult;
import edu.scau.vms.module.integration.dto.SyncUsersRequest;
import edu.scau.vms.module.integration.dto.SyncUsersRequest.EduUser;
import edu.scau.vms.module.user.entity.User;
import edu.scau.vms.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

// 对接教务系统：批量同步学生账号 + 实时密码验证
// syncUsers：教务系统 → VMS 批量同步
// validateAgainstEdu：模拟教务系统验证密码，AuthService.login 本地验不过时兜底
// 真对接时 validateAgainstEdu 改为调教务 API：POST /edu/api/validate
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // 批量同步学生账号
    // 教务系统传过来的密码已经是 BCrypt(SHA256(明文))，VMS 直接存不碰明文
    @Transactional
    public SyncResult syncUsers(SyncUsersRequest req) {
        int created = 0;
        int updated = 0;
        for (EduUser eu : req.getUsers()) {
            User exist = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, eu.getStudentId()));
            if (exist != null) {
                exist.setName(eu.getName());
                exist.setPassword(eu.getPasswordHash());
                exist.setSource("EDU");
                exist.setSyncedAt(LocalDateTime.now());
                userMapper.updateById(exist);
                updated++;
            } else {
                User u = new User();
                u.setUsername(eu.getStudentId());
                u.setPassword(eu.getPasswordHash());
                u.setName(eu.getName());
                u.setRole(Role.VOLUNTEER);
                u.setSource("EDU");
                u.setSyncedAt(LocalDateTime.now());
                u.setIsOrganizer(false);
                u.setIsAdmin(false);
                userMapper.insert(u);
                created++;
            }
        }
        log.info("[Integration] 同步完成：新建 {} 更新 {} 总计 {}", created, updated, req.getUsers().size());
        return SyncResult.builder()
                .created(created).updated(updated).total(req.getUsers().size())
                .build();
    }

    // 模拟教务系统验证密码
    // passwordHash 是前端 SHA256 后的值，用 BCrypt.matches 验
    // 真对接时改为调教务 API：POST /edu/api/validate {studentId, passwordHash} → {valid}
    public EduValidateResult validateAgainstEdu(String studentId, String passwordHash) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, studentId));
        if (user == null) {
            return EduValidateResult.builder().valid(false).studentId(studentId).build();
        }
        boolean valid = "EDU".equals(user.getSource())
                && passwordEncoder.matches(passwordHash, user.getPassword());
        log.info("[Integration] 教务验证 studentId={} → {}", studentId, valid ? "通过" : "失败");
        return EduValidateResult.builder()
                .valid(valid)
                .studentId(studentId)
                .name(user.getName())
                .build();
    }

    // 同步统计
    public Map<String, Long> stats() {
        Long eduCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getSource, "EDU"));
        Long localCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getSource, "LOCAL"));
        return Map.of("eduUsers", eduCount, "localUsers", localCount);
    }
}
