package edu.scau.vms.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;
    private volatile SecretKey signingKey;

    private SecretKey key() {
        SecretKey k = signingKey;
        if (k == null) {
            byte[] decoded = Base64.getDecoder().decode(props.getSecret());
            k = Keys.hmacShaKeyFor(decoded);
            signingKey = k;
        }
        return k;
    }

    public String generate(UserPrincipal p) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofHours(props.getExpirationHours()));
        return Jwts.builder()
                .issuer(props.getIssuer())
                .subject(String.valueOf(p.userId()))
                .claim("usr", p.username())
                .claim("nam", p.name())
                .claim("role", p.role())
                .claim("org", p.organizer())
                .claim("adm", p.admin())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key())
                .compact();
    }

    /** 解析并校验 token。非法/过期时抛 {@link JwtException}。 */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .requireIssuer(props.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UserPrincipal toPrincipal(Claims c) {
        return new UserPrincipal(
                Long.valueOf(c.getSubject()),
                c.get("usr", String.class),
                c.get("nam", String.class),
                c.get("role", String.class),
                Boolean.TRUE.equals(c.get("org", Boolean.class)),
                Boolean.TRUE.equals(c.get("adm", Boolean.class))
        );
    }
}
