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

import com.auth0.jwt.JWTExpiredException;
import com.auth0.jwt.JWTVerifier;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.security.cookies.JWTCookieGenerator;
import io.gravitee.management.service.common.JWTHelper.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @author Azize Elamrani (azize at gravitee.io)
 * @author GraviteeSource Team
 */
public class JWTAuthenticationFilter extends GenericFilterBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthenticationFilter.class);

    private final JWTVerifier jwtVerifier;
    private JWTCookieGenerator jwtCookieGenerator;

    public JWTAuthenticationFilter(final String jwtSecret, final JWTCookieGenerator jwtCookieGenerator) {
        this.jwtVerifier = new JWTVerifier(jwtSecret);
        this.jwtCookieGenerator = jwtCookieGenerator;
    }

    @Override
    @SuppressWarnings(value = "unchecked")
    public void doFilter(final ServletRequest request, final ServletResponse response,
                         final FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String stringToken = req.getHeader(HttpHeaders.AUTHORIZATION);

        if (isEmpty(stringToken) && req.getCookies() != null) {
            final String authCookieName = "Auth-Graviteeio-APIM";
            final Optional<Cookie> optionalStringToken = Arrays.stream(req.getCookies())
                    .filter(cookie -> authCookieName.equals(cookie.getName()))
                    .findAny();
            if (optionalStringToken.isPresent()) {
                stringToken = decode(optionalStringToken.get().getValue(), defaultCharset().name());
            }
        }

        if (isEmpty(stringToken)) {
            LOGGER.debug("Authorization header/cookie not found");
        } else {
            final String authorizationSchema = "Bearer";
            if (stringToken.contains(authorizationSchema)) {
                final String jwtToken = stringToken.substring(authorizationSchema.length()).trim();
                try {
                    final Map<String, Object> verify = jwtVerifier.verify(jwtToken);

                    List<Map> permissions = (List<Map>) verify.get(Claims.PERMISSIONS);
                    List<SimpleGrantedAuthority> authorities;

                    if (permissions != null) {
                        authorities = ((List<Map>) verify.get(Claims.PERMISSIONS)).stream()
                                .map(map -> new SimpleGrantedAuthority(map.get("authority").toString()))
                                .collect(Collectors.toList());
                    } else {
                        authorities = Collections.emptyList();
                    }

                    final UserDetails userDetails = new UserDetails(getStringValue(verify.get(Claims.SUBJECT)), "",
                            authorities);
                    userDetails.setEmail((String) verify.get(Claims.EMAIL));
                    userDetails.setFirstname((String) verify.get(Claims.FIRSTNAME));
                    userDetails.setLastname((String) verify.get(Claims.LASTNAME));

                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
                } catch (final Exception e) {
                    final String errorMessage = "Invalid token";
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.error(errorMessage, e);
                    } else {
                        if (e instanceof JWTExpiredException) {
                            LOGGER.warn(errorMessage);
                        } else {
                            LOGGER.error(errorMessage);
                        }
                    }
                    res.addCookie(jwtCookieGenerator.generate(null));
                    res.sendError(HttpStatusCode.UNAUTHORIZED_401);
                    return;
                }
            } else {
                LOGGER.debug("Authorization schema not found");
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
