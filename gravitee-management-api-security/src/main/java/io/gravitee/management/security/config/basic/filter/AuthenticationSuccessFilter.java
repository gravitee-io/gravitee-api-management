/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.security.config.basic.filter;

import com.auth0.jwt.JWTSigner;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.security.JWTCookieGenerator;
import io.gravitee.management.security.config.JWTClaims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Azize Elamrani (azize at gravitee.io)
 * @author GraviteeSource Team
 */
public class AuthenticationSuccessFilter extends GenericFilterBean {

    private final JWTCookieGenerator jwtCookieGenerator;
    private final String jwtSecret;
    private final int jwtExpireAfter;
    private final String jwtIssuer;

    public AuthenticationSuccessFilter(final JWTCookieGenerator jwtCookieGenerator,
                                       final String jwtSecret, final String jwtIssuer, final int jwtExpireAfter) {
        this.jwtCookieGenerator = jwtCookieGenerator;
        this.jwtSecret = jwtSecret;
        this.jwtExpireAfter = jwtExpireAfter;
        this.jwtIssuer = jwtIssuer;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest req = (HttpServletRequest) servletRequest;

        final Optional<Cookie> optionalStringToken;

        if (req.getCookies() == null) {
            optionalStringToken = Optional.empty();
        } else {
            optionalStringToken = Arrays.stream(req.getCookies())
                    .filter(cookie -> HttpHeaders.AUTHORIZATION.equals(cookie.getName()))
                    .findAny();
        }

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !optionalStringToken.isPresent()) {
            // JWT signer
            final Map<String, Object> claims = new HashMap<>();
            claims.put(JWTClaims.ISSUER, jwtIssuer);

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            claims.put(JWTClaims.PERMISSIONS, userDetails.getAuthorities());
            claims.put(JWTClaims.SUBJECT, userDetails.getUsername());
            claims.put(JWTClaims.EMAIL, userDetails.getEmail());
            claims.put(JWTClaims.FIRSTNAME, userDetails.getFirstname());
            claims.put(JWTClaims.LASTNAME, userDetails.getLastname());

            final JWTSigner.Options options = new JWTSigner.Options();
            options.setExpirySeconds(jwtExpireAfter);
            options.setIssuedAt(true);
            options.setJwtId(true);

            final Cookie bearerCookie =
                    jwtCookieGenerator.generate("Bearer " + new JWTSigner(jwtSecret).sign(claims, options));
            ((HttpServletResponse) servletResponse).addCookie(bearerCookie);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
