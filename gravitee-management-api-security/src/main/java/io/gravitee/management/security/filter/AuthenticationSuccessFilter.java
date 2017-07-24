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
package io.gravitee.management.security.filter;

import com.auth0.jwt.JWTSigner;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.security.cookies.JWTCookieGenerator;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.common.JWTHelper.Claims;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import java.util.*;

/**
 * @author Azize Elamrani (azize at gravitee.io)
 * @author GraviteeSource Team
 */
public class AuthenticationSuccessFilter extends GenericFilterBean {

    private final MembershipService membershipService;

    private final JWTCookieGenerator jwtCookieGenerator;
    private final String jwtSecret;
    private final int jwtExpireAfter;
    private final String jwtIssuer;

    public AuthenticationSuccessFilter(final JWTCookieGenerator jwtCookieGenerator,
                                       final String jwtSecret, final String jwtIssuer, final int jwtExpireAfter,
                                       final MembershipService membershipService) {
        this.jwtCookieGenerator = jwtCookieGenerator;
        this.jwtSecret = jwtSecret;
        this.jwtExpireAfter = jwtExpireAfter;
        this.jwtIssuer = jwtIssuer;
        this.membershipService = membershipService;
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
                    .filter(cookie -> cookie.getValue() != null && ! cookie.getValue().isEmpty())
                    .findAny();
        }

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !optionalStringToken.isPresent()) {
            // JWT signer
            final Map<String, Object> claims = new HashMap<>();
            claims.put(Claims.ISSUER, jwtIssuer);

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Manage authorities, initialize it with dynamic permissions from the IDP
            Set<GrantedAuthority> authorities = new HashSet<>(userDetails.getAuthorities());

            // We must also load permissions from repository for configured management or portal role
            RoleEntity role =  membershipService.getRole(MembershipReferenceType.MANAGEMENT, MembershipDefaultReferenceId.DEFAULT.toString(), userDetails.getUsername());
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority(role.getScope().toString() + ':' + role.getName()));
            }

            role =  membershipService.getRole(MembershipReferenceType.PORTAL, MembershipDefaultReferenceId.DEFAULT.toString(), userDetails.getUsername());
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority(role.getScope().toString() + ':' + role.getName()));
            }

            claims.put(Claims.PERMISSIONS, authorities);
            claims.put(Claims.SUBJECT, userDetails.getUsername());
            claims.put(Claims.EMAIL, userDetails.getEmail());
            claims.put(Claims.FIRSTNAME, userDetails.getFirstname());
            claims.put(Claims.LASTNAME, userDetails.getLastname());

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
