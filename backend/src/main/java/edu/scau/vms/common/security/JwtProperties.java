package edu.scau.vms.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vms.jwt")
public class JwtProperties {

    /** Base64-encoded HMAC secret. HS256 要求解码后 >= 32 bytes (256 bit)。 */
    private String secret;

    /** Token 有效期（小时）。 */
    private long expirationHours = 24L;

    /** 签发者。 */
    private String issuer = "vms-backend";
}
