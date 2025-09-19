/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.security.filter;

import static java.net.URLDecoder.decode;
import static java.nio.charset.Charset.defaultCharset;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.management.model.Token;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

/**
 * @author Azize Elamrani (azize at gravitee.io)
 * @author GraviteeSource Team
 */
@Slf4j
public class TokenAuthenticationFilter extends GenericFilterBean {

    public static final String AUTH_COOKIE_NAME = "Auth-Graviteeio-APIM";
    public static final String TOKEN_AUTH_SCHEMA = "bearer";

    private final JWTVerifier jwtVerifier;
    private CookieGenerator cookieGenerator;
    private UserService userService;
    private TokenService tokenService;
    private AuthoritiesProvider authoritiesProvider;

    public TokenAuthenticationFilter(
        final String jwtSecret,
        final CookieGenerator cookieGenerator,
        final UserService userService,
        final TokenService tokenService,
        final AuthoritiesProvider authoritiesProvider
    ) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        jwtVerifier = JWT.require(algorithm).build();
        this.cookieGenerator = cookieGenerator;
        this.userService = userService;
        this.tokenService = tokenService;
        this.authoritiesProvider = authoritiesProvider;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String stringToken = req.getHeader(HttpHeaders.AUTHORIZATION);

        if (isEmpty(stringToken) && req.getCookies() != null) {
            final Optional<Cookie> optionalStringToken = Arrays.stream(req.getCookies())
                .filter(cookie -> AUTH_COOKIE_NAME.equals(cookie.getName()))
                .findAny();
            if (optionalStringToken.isPresent()) {
                stringToken = decode(optionalStringToken.get().getValue(), defaultCharset().name());
            }
        }

        if (isEmpty(stringToken)) {
            log.debug("Authorization header/cookie not found");
        } else {
            try {
                if (stringToken.toLowerCase().contains(TOKEN_AUTH_SCHEMA)) {
                    final String tokenValue = stringToken.substring(TOKEN_AUTH_SCHEMA.length()).trim();
                    if (tokenValue.contains(".")) {
                        final DecodedJWT jwt = jwtVerifier.verify(tokenValue);

                        final Set<GrantedAuthority> authorities = this.authoritiesProvider.retrieveAuthorities(
                            jwt.getClaim(Claims.SUBJECT).asString()
                        );

                        final UserDetails userDetails = new UserDetails(getStringValue(jwt.getSubject()), "", authorities);
                        userDetails.setEmail(jwt.getClaim(Claims.EMAIL).asString());
                        userDetails.setFirstname(jwt.getClaim(Claims.FIRSTNAME).asString());
                        userDetails.setLastname(jwt.getClaim(Claims.LASTNAME).asString());
                        userDetails.setOrganizationId(jwt.getClaim(Claims.ORG).asString());

                        SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities)
                        );
                    } else if (tokenService != null && userService != null) {
                        final Token token = tokenService.findByToken(tokenValue);
                        final UserEntity user = userService.findById(GraviteeContext.getExecutionContext(), token.getReferenceId());

                        final Set<GrantedAuthority> authorities = this.authoritiesProvider.retrieveAuthorities(user.getId());

                        final UserDetails userDetails = new UserDetails(user.getId(), "", authorities);
                        userDetails.setFirstname(user.getFirstname());
                        userDetails.setLastname(user.getLastname());
                        userDetails.setEmail(user.getEmail());
                        userDetails.setSource("token");
                        userDetails.setSourceId(token.getName());
                        userDetails.setOrganizationId(user.getOrganizationId());

                        SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities)
                        );
                    }
                } else {
                    log.debug("Authorization schema not found");
                }
            } catch (final Exception e) {
                final String errorMessage = "Invalid token";
                if (log.isDebugEnabled()) {
                    log.error(errorMessage, e);
                } else {
                    if (e instanceof JWTVerificationException) {
                        log.warn(errorMessage);
                    } else {
                        log.error(errorMessage);
                    }
                }
                res.addCookie(cookieGenerator.generate(TokenAuthenticationFilter.AUTH_COOKIE_NAME, null));
                res.sendError(HttpStatusCode.UNAUTHORIZED_401);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private String getStringValue(final Object object) {
        if (object == null) {
            return "";
        }
        return object.toString();
    }
}
