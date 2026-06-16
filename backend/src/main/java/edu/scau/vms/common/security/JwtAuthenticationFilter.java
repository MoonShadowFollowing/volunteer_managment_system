package edu.scau.vms.common.security;

import edu.scau.vms.common.constant.ErrorCode;
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
            UserPrincipal p = jwt.toPrincipal(claims);

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
