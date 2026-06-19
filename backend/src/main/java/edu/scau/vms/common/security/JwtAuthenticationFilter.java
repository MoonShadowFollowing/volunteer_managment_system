package edu.scau.vms.common.security;

import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.module.user.entity.User;
import edu.scau.vms.module.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwt;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(req, res);
            return;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = jwt.parse(token);
            Long userId = Long.valueOf(claims.getSubject());
            User u = userMapper.selectById(userId);
            if (u == null) {
                SecurityContextHolder.clearContext();
                SecurityExceptionResponder.write(res, 401, ErrorCode.TOKEN_INVALID, "用户不存在");
                return;
            }

            // 以数据库最新状态覆盖 JWT 中的 org/adm 声明
            UserPrincipal p = new UserPrincipal(
                    u.getUserId(),
                    u.getUsername(),
                    u.getName(),
                    u.getRole(),
                    Boolean.TRUE.equals(u.getIsOrganizer()),
                    Boolean.TRUE.equals(u.getIsAdmin())
            );

            List<SimpleGrantedAuthority> auths = new ArrayList<>(3);
            auths.add(new SimpleGrantedAuthority("ROLE_" + p.role().toUpperCase()));
            if (p.organizer()) auths.add(new SimpleGrantedAuthority("ORG"));
            if (p.admin())     auths.add(new SimpleGrantedAuthority("ADM"));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(p, null, auths);
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(req, res);
        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            SecurityExceptionResponder.write(res, 401, ErrorCode.TOKEN_EXPIRED, "Token已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            SecurityExceptionResponder.write(res, 401, ErrorCode.TOKEN_INVALID, "Token无效");
        }
    }
}
