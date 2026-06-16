package edu.scau.vms.common.security;

/**
 * 放进 Spring Security SecurityContext 的认证主体。
 * Controller 用 {@code @AuthenticationPrincipal UserPrincipal me} 直接拿。
 */
public record UserPrincipal(
        Long userId,
        String username,
        String name,
        String role,
        boolean organizer,
        boolean admin
) {}
