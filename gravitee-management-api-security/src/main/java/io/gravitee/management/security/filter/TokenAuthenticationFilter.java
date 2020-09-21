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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.TokenEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.security.cookies.CookieGenerator;
import io.gravitee.management.security.utils.AuthoritiesProvider;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.TokenService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.common.JWTHelper;
import io.gravitee.management.service.common.JWTHelper.Claims;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.repository.management.model.Token;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
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
import java.util.stream.Collectors;

import static java.net.URLDecoder.decode;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.springframework.security.core.authority.AuthorityUtils.commaSeparatedStringToAuthorityList;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

/**
 * @author Azize Elamrani (azize at gravitee.io)
 * @author GraviteeSource Team
 */
public class TokenAuthenticationFilter extends GenericFilterBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenAuthenticationFilter.class);

    public static final String AUTH_COOKIE_NAME = "Auth-Graviteeio-APIM";
    public static final String TOKEN_AUTH_SCHEMA = "bearer";

    private final JWTVerifier jwtVerifier;
    private CookieGenerator cookieGenerator;
    private UserService userService;
    private TokenService tokenService;
    private AuthoritiesProvider authoritiesProvider;

    public TokenAuthenticationFilter(final String jwtSecret, final CookieGenerator cookieGenerator,
                                     final UserService userService, final TokenService tokenService, final MembershipService membershipService) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        jwtVerifier = JWT.require(algorithm).build();
        this.cookieGenerator = cookieGenerator;
        this.userService = userService;
        this.tokenService = tokenService;
        this.authoritiesProvider = new AuthoritiesProvider(membershipService);
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public void doFilter(final ServletRequest request, final ServletResponse response,
                         final FilterChain chain) throws IOException, ServletException {
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
            LOGGER.debug("Authorization header/cookie not found");
        } else {
            try {
                if (stringToken.toLowerCase().contains(TOKEN_AUTH_SCHEMA)) {
                    final String tokenValue = stringToken.substring(TOKEN_AUTH_SCHEMA.length()).trim();
                    if (tokenValue.contains(".")) {
                        final DecodedJWT jwt = jwtVerifier.verify(tokenValue);

                        final Set<GrantedAuthority> authorities =
                                this.authoritiesProvider.retrieveAuthorities(jwt.getClaim(Claims.SUBJECT).asString());

                        final UserDetails userDetails = new UserDetails(getStringValue(jwt.getSubject()), "",
                                authorities);
                        userDetails.setEmail(jwt.getClaim(Claims.EMAIL).asString());
                        userDetails.setFirstname(jwt.getClaim(Claims.FIRSTNAME).asString());
                        userDetails.setLastname(jwt.getClaim(Claims.LASTNAME).asString());

                        getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, authorities));
                    } else {
                        final Token token = tokenService.findByToken(tokenValue);
                        final UserEntity user = userService.findById(token.getReferenceId());

                        final Set<GrantedAuthority> authorities = this.authoritiesProvider.retrieveAuthorities(user.getId());

                        final UserDetails userDetails = new UserDetails(user.getId(), "", authorities);
                        userDetails.setFirstname(user.getFirstname());
                        userDetails.setLastname(user.getLastname());
                        userDetails.setEmail(user.getEmail());
                        userDetails.setSource("token");
                        userDetails.setSourceId(token.getName());

                        getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, authorities));
                    }
                } else {
                    LOGGER.debug("Authorization schema not found");
                }
            } catch (final Exception e) {
                final String errorMessage = "Invalid token";
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.error(errorMessage, e);
                } else {
                    if (e instanceof JWTVerificationException) {
                        LOGGER.warn(errorMessage);
                    } else {
                        LOGGER.error(errorMessage);
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
