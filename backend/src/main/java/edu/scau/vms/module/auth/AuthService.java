package edu.scau.vms.module.auth;

import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.exception.BizException;
import edu.scau.vms.common.security.JwtService;
import edu.scau.vms.common.security.UserPrincipal;
import edu.scau.vms.module.auth.dto.LoginRequest;
import edu.scau.vms.module.auth.dto.LoginResponse;
import edu.scau.vms.module.auth.dto.UserInfo;
import edu.scau.vms.module.integration.IntegrationService;
import edu.scau.vms.module.integration.dto.EduValidateResult;
import edu.scau.vms.module.user.UserService;
import edu.scau.vms.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// 登录 + 拉用户信息 + 刷 token
// me() 和 refresh() 都会从 DB 重查，因为组织者/管理员资格可能被超管改过
// login() 密码校验链路：本地BCrypt → 失败且source=EDU时 → 调教务系统兜底
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final IntegrationService integrationService;

    public LoginResponse login(LoginRequest req) {
        User user = userService.findByUsername(req.getAccount());
        if (user == null) {
            throw new BizException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误",
                    "请检查账号和密码是否正确。学生用户请使用学号登录。如忘记密码，请联系系统管理员重置。");
        }

        // 第一步：本地 BCrypt 验
        if (passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            UserPrincipal p = toPrincipal(user);
            String token = jwtService.generate(p);
            return new LoginResponse(token, toUserInfo(p));
        }

        // 第二步：本地没对上，且是教务同步过来的用户 →
        //         调教务系统实时验证，兜底"学生刚在教务改完密码还没同步到VMS"的场景
        if ("EDU".equals(user.getSource())) {
            EduValidateResult eduResult = integrationService.validateAgainstEdu(
                    req.getAccount(), req.getPassword());
            if (eduResult.isValid()) {
                // 教务说密码对 → 更新本地密码哈希，下次直接本地过不用再调教务
                userService.updatePassword(user.getUserId(), passwordEncoder.encode(req.getPassword()));
                UserPrincipal p = toPrincipal(user);
                String token = jwtService.generate(p);
                return new LoginResponse(token, toUserInfo(p));
            }
        }

        throw new BizException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误",
                "请检查账号和密码是否正确。学生用户请使用学号登录。如忘记密码，请联系系统管理员重置。");
    }

    // 每次都重查，不信 JWT 里的 org/adm，防止资格被撤销后客户端还在用旧 token 越权
    public UserInfo me(UserPrincipal p) {
        User u = userService.findById(p.userId());
        if (u == null) throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在",
                "您的账号可能已被删除或禁用，请联系系统管理员确认。");
        return toUserInfo(toPrincipal(u));
    }

    // 刷 token 也走重查；前端 403 自动同步流程就靠这个
    public String refresh(UserPrincipal p) {
        User u = userService.findById(p.userId());
        if (u == null) throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在",
                "您的账号可能已被删除或禁用，请联系系统管理员确认。");
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
