package edu.scau.vms.module.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.vms.common.PageResult;
import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.constant.MsgType;
import edu.scau.vms.common.constant.Role;
import edu.scau.vms.common.exception.BizException;
import edu.scau.vms.module.activity.entity.Activity;
import edu.scau.vms.module.activity.mapper.ActivityMapper;
import edu.scau.vms.module.message.MessageService;
import edu.scau.vms.module.user.dto.UserSummaryVO;
import edu.scau.vms.module.user.entity.User;
import edu.scau.vms.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;
    private final MessageService messageService;

    public User findByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    public User findById(Long userId) {
        return userMapper.selectById(userId);
    }

    public PageResult<UserSummaryVO> promotable(Long page, Long size, String name, String userNo) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getIsAdmin, false)
          .ne(User::getRole, Role.SUPERADMIN)
          // 仅允许 username 长度为 8 位的用户被提升为管理员（学号/工号格式）
          .apply("LENGTH(username) = 8");
        if (name != null && !name.isBlank()) qw.like(User::getName, name);
        if (userNo != null && !userNo.isBlank()) {
            String numeric = userNo.replaceAll("[^0-9]", "");
            if (!numeric.isBlank()) qw.like(User::getUserId, numeric);
        }
        qw.orderByAsc(User::getUserId);
        Page<User> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<User> result = userMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), result.getRecords().stream().map(this::toVO).toList());
    }

    public PageResult<UserSummaryVO> admins(Long page, Long size, String name, String userNo) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getIsAdmin, true)
          .ne(User::getRole, Role.SUPERADMIN);
        if (name != null && !name.isBlank()) qw.like(User::getName, name);
        if (userNo != null && !userNo.isBlank()) {
            String numeric = userNo.replaceAll("[^0-9]", "");
            if (!numeric.isBlank()) qw.like(User::getUserId, numeric);
        }
        qw.orderByAsc(User::getUserId);
        Page<User> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<User> result = userMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), result.getRecords().stream().map(this::toVO).toList());
    }

    /** 已通过组织者列表（is_organizer=true 且 role != superadmin），含已发布活动数 */
    public PageResult<UserSummaryVO> organizers(Long page, Long size, String name, String userNo) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getIsOrganizer, true)
          .ne(User::getRole, Role.SUPERADMIN);
        if (name != null && !name.isBlank()) qw.like(User::getName, name);
        if (userNo != null && !userNo.isBlank()) {
            String numeric = userNo.replaceAll("[^0-9]", "");
            if (!numeric.isBlank()) qw.like(User::getUserId, numeric);
        }
        qw.orderByAsc(User::getUserId);
        Page<User> p = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        Page<User> result = userMapper.selectPage(p, qw);

        List<UserSummaryVO> vos = result.getRecords().stream().map(this::toVO).toList();
        List<Long> uids = vos.stream().map(UserSummaryVO::getUserId).toList();
        Map<Long, Long> actCountMap = countActivitiesByOrganizers(uids);
        vos.forEach(v -> v.setActCount(actCountMap.getOrDefault(v.getUserId(), 0L)));
        return PageResult.of(result.getTotal(), vos);
    }

    @Transactional
    public void promoteAdmin(Long userId, Long operatorId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        if (Role.SUPERADMIN.equals(u.getRole())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "不能修改超级管理员");
        }
        if (Boolean.TRUE.equals(u.getIsAdmin())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "该用户已经是管理员");
        }
        // 校验：仅允许 username 长度为 8 位的用户被提升为管理员
        if (u.getUsername() == null || u.getUsername().length() != 8) {
            throw new BizException(ErrorCode.PARAM_INVALID, "仅允许用户名为 8 位（学号/工号格式）的用户成为管理员");
        }
        u.setIsAdmin(true);
        u.setRole(Role.ADMIN);
        // 方案三：管理员自动获得组织者资质（管理员 ⊇ 组织者 ⊇ 志愿者）
        u.setIsOrganizer(true);
        userMapper.updateById(u);
        messageService.sendDirect(userId, MsgType.QUAL_AUDIT,
                "您已获得系统管理员权限",
                "超级管理员已将您提升为系统管理员，可使用管理员功能。",
                "volunteer");
    }

    @Transactional
    public void revokeAdmin(Long userId, Long operatorId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        if (Role.SUPERADMIN.equals(u.getRole())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "不能修改超级管理员");
        }
        if (!Boolean.TRUE.equals(u.getIsAdmin())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "该用户不是管理员");
        }
        u.setIsAdmin(false);
        // 同时把角色降回 volunteer（除非他还是 organizer，反正 role 字段在 V1.1 中已淡化）
        u.setRole(Role.VOLUNTEER);
        userMapper.updateById(u);
        messageService.sendDirect(userId, MsgType.QUAL_AUDIT,
                "管理员权限已被撤销",
                "超级管理员已撤销您的系统管理员权限。",
                "volunteer");
    }

    /** 撤销组织者资质 */
    @Transactional
    public void revokeOrganizer(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        if (!Boolean.TRUE.equals(u.getIsOrganizer())) {
            throw new BizException(ErrorCode.BIZ_CONFLICT, "该用户没有组织者资质");
        }
        // 方案三：管理员身份不可被撤销组织者资质
        if (Boolean.TRUE.equals(u.getIsAdmin())) {
            throw new BizException(ErrorCode.ADMIN_CANNOT_REVOKE_ORGANIZER,
                    "该用户是系统管理员，无法撤销其组织者身份！");
        }
        u.setIsOrganizer(false);
        userMapper.updateById(u);
        messageService.sendDirect(userId, MsgType.QUAL_AUDIT,
                "组织者资质已被撤销",
                "管理员已撤销您的组织者资质，您将无法继续发布活动。",
                "volunteer");
    }

    public UserSummaryVO toVO(User u) {
        return UserSummaryVO.builder()
                .userId(u.getUserId())
                .username(u.getUsername())
                .name(u.getName())
                .role(u.getRole())
                .phone(u.getPhone())
                .userNo(formatUserNo(u))
                .isOrganizer(Boolean.TRUE.equals(u.getIsOrganizer()))
                .isAdmin(Boolean.TRUE.equals(u.getIsAdmin()))
                .createdAt(u.getCreatedAt())
                .build();
    }

    public String formatUserNo(User u) {
        String prefix;
        if (Role.SUPERADMIN.equals(u.getRole())) prefix = "SUP";
        else if (Boolean.TRUE.equals(u.getIsAdmin())) prefix = "ADM";
        else if (Boolean.TRUE.equals(u.getIsOrganizer())) prefix = "ORG";
        else prefix = "VOL";
        // 使用 username（工号/学号）作为编号后缀
        return prefix + "-" + u.getUsername();
    }

    private Map<Long, Long> countActivitiesByOrganizers(List<Long> organizerIds) {
        if (organizerIds.isEmpty()) return Map.of();
        List<Activity> list = activityMapper.selectList(
                new LambdaQueryWrapper<Activity>().in(Activity::getOrganizerId, organizerIds));
        return list.stream().collect(Collectors.groupingBy(Activity::getOrganizerId, Collectors.counting()));
    }
}
