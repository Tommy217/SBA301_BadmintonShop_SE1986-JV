package fu.se.badmintonshop.modules.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProperties {

    @Value("${app.jwt.secret-base64:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretBase64;

    @Value("${app.jwt.access-token-ttl-seconds:86400}")
    private long accessTokenTtlSeconds;

    public String secretBase64() {
        return secretBase64;
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }
}

