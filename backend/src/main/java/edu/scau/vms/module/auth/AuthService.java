package edu.scau.vms.module.auth;

import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.exception.BizException;
import edu.scau.vms.common.security.JwtService;
import edu.scau.vms.common.security.UserPrincipal;
import edu.scau.vms.module.auth.dto.LoginRequest;
import edu.scau.vms.module.auth.dto.LoginResponse;
import edu.scau.vms.module.auth.dto.UserInfo;
import edu.scau.vms.module.user.UserService;
import edu.scau.vms.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// 登录 + 拉用户信息 + 刷 token
// me() 和 refresh() 都会从 DB 重查一次，因为组织者/管理员资格可能被超管改过
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest req) {
        User user = userService.findByUsername(req.getAccount());
        // 密码是 BCrypt 散列过的，用 PasswordEncoder.matches 验
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误");
        }
        UserPrincipal p = toPrincipal(user);
        String token = jwtService.generate(p);
        return new LoginResponse(token, toUserInfo(p));
    }

    // 每次都重查，不信 JWT 里的 org/adm，防止资格被撤销后客户端还在用旧 token 越权
    public UserInfo me(UserPrincipal p) {
        User u = userService.findById(p.userId());
        if (u == null) throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        return toUserInfo(toPrincipal(u));
    }

    // 刷 token 也走重查；前端 403 自动同步流程就靠这个
    public String refresh(UserPrincipal p) {
        User u = userService.findById(p.userId());
        if (u == null) throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        return jwtService.generate(toPrincipal(u));
    }

    private static UserPrincipal toPrincipal(User u) {
        return new UserPrincipal(
                u.getUserId(),
                u.getUsername(),
                u.getName(),
                u.getRole(),
                Boolean.TRUE.equals(u.getIsOrganizer()),
                Boolean.TRUE.equals(u.getIsAdmin())
        );
    }

    private static UserInfo toUserInfo(UserPrincipal p) {
        return UserInfo.builder()
                .userId(p.userId())
                .username(p.username())
                .name(p.name())
                .role(p.role())
                .isOrganizerQualified(p.organizer())
                .isAdmin(p.admin())
                .build();
    }
}
