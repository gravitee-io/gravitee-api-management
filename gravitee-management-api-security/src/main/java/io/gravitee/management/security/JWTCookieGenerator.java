package io.gravitee.management.security;

import io.gravitee.common.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.servlet.http.Cookie;

/**
 * @author Azize Elamrani (azize at gravitee.io)
 * @author GraviteeSource Team
 */
public class JWTCookieGenerator {

    private static final boolean DEFAULT_JWT_COOKIE_SECURE = false;
    private static final String DEFAULT_JWT_COOKIE_PATH = "/";

    @Autowired
    private Environment environment;

    public Cookie generate(final String value) {
        final Cookie cookie = new Cookie(HttpHeaders.AUTHORIZATION, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(environment.getProperty("jwt.cookie-secure", Boolean.class, DEFAULT_JWT_COOKIE_SECURE));
        cookie.setPath(environment.getProperty("jwt.cookie-path", DEFAULT_JWT_COOKIE_PATH));
        return cookie;
    }
}
