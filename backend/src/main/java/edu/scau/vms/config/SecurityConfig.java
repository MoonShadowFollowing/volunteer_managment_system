package edu.scau.vms.config;

import edu.scau.vms.common.constant.ErrorCode;
import edu.scau.vms.common.security.JwtAuthenticationFilter;
import edu.scau.vms.common.security.SecurityExceptionResponder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Spring Security 的总入口：白名单 + JWT 过滤器 + 401/403 处理
// @EnableMethodSecurity 开了之后 @PreAuthorize 才生效
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 这步坑过：JwtAuthenticationFilter 被 @Component 之后 Spring Boot 默认会再注册一次到全局 Servlet
    // 链，导致每个请求过两遍。这里显式 disable 让它只在 SecurityFilterChain 里跑
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterAutoRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        // 白名单：登录、健康检查、对外综测、静态文件、Swagger
                        // 其它一律要 JWT
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/health/**",
                                "/api/public/**",
                                "/api/integration/edu/validate",
                                "/api/files/static/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                SecurityExceptionResponder.write(res, 401,
                                        ErrorCode.UNAUTHORIZED, "未登录或Token已过期"))
                        .accessDeniedHandler((req, res, ex) ->
                                SecurityExceptionResponder.write(res, 403,
                                        ErrorCode.FORBIDDEN, "权限不足")));
        return http.build();
    }
}
