package fu.se.badmintonshop.modules.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProperties {

    @Value("${app.jwt.secret-base64:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretBase64;

    @Value("${app.jwt.access-token-ttl-seconds:900}")
    private long accessTokenTtlSeconds;

    @Value("${app.jwt.cookie-name:ACCESS_TOKEN}")
    private String cookieName;

    @Value("${app.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.jwt.cookie-same-site:Lax}")
    private String cookieSameSite;

    public String secretBase64() {
        return secretBase64;
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public String cookieName() {
        return cookieName;
    }

    public boolean cookieSecure() {
        return cookieSecure;
    }

    public String cookieSameSite() {
        return cookieSameSite;
    }
}

