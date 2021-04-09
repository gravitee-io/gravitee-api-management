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
package io.gravitee.rest.api.portal.rest.resource.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.util.Maps;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.portal.rest.model.Token;
import io.gravitee.rest.api.portal.rest.model.Token.TokenTypeEnum;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EXPIRE_AFTER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class AbstractAuthenticationResource {

    @Autowired
    protected Environment environment;
    @Autowired
    protected UserService userService;
    @Autowired
    protected MembershipService membershipService;
    @Autowired
    protected CookieGenerator cookieGenerator;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String CLIENT_ID_KEY = "client_id";
    public static final String REDIRECT_URI_KEY = "redirect_uri";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String CODE_KEY = "code";
    public static final String CODE_VERIFIER_KEY = "code_verifier";
    public static final String GRANT_TYPE_KEY = "grant_type";
    public static final String TOKEN = "token";
    public static final String STATE = "state";

    protected Map<String, Object> getResponseEntity(final Response response) throws IOException {
        return getEntity((getResponseEntityAsString(response)));
    }

    protected String getResponseEntityAsString(final Response response) {
        return response.readEntity(String.class);
    }

    protected Map<String, Object> getEntity(final String response) throws IOException {
        return MAPPER.readValue(response, new TypeReference<Map<String, Object>>() {});
    }

    protected Response connectUser(String userId,final String state, final HttpServletResponse servletResponse, final String accessToken, final String idToken) {
        UserEntity user = userService.connect(userId);

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Manage authorities, initialize it with dynamic permissions from the IDP
        List<Map<String, String>> authorities = userDetails.getAuthorities().stream().map(authority -> Maps.<String, String>builder().put("authority", authority.getAuthority()).build()).collect(Collectors.toList());

        // We must also load permissions from repository for configured environment role
        Set<RoleEntity> userRoles = membershipService.getRoles(
                MembershipReferenceType.ENVIRONMENT,
                GraviteeContext.getCurrentEnvironment(),
                MembershipMemberType.USER,
                userDetails.getId());
        if (!userRoles.isEmpty()) {
            userRoles.forEach(role -> authorities.add(Maps.<String, String>builder().put("authority", role.getScope().toString() + ':' + role.getName()).build()));
        }

        // JWT signer
        Algorithm algorithm = Algorithm.HMAC256(environment.getProperty("jwt.secret"));

        Date issueAt = new Date();
        Instant expireAt = issueAt.toInstant().plus(Duration.ofSeconds(environment.getProperty("jwt.expire-after", Integer.class, DEFAULT_JWT_EXPIRE_AFTER)));

        final String sign = JWT.create()
                .withIssuer(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER))
                .withIssuedAt(issueAt)
                .withExpiresAt(Date.from(expireAt))
                .withSubject(user.getId())
                .withClaim(JWTHelper.Claims.PERMISSIONS, authorities)
                .withClaim(JWTHelper.Claims.EMAIL, user.getEmail())
                .withClaim(JWTHelper.Claims.FIRSTNAME, user.getFirstname())
                .withClaim(JWTHelper.Claims.LASTNAME, user.getLastname())
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithm);

        final Token tokenEntity = new Token();
        tokenEntity.setTokenType(TokenTypeEnum.BEARER);
        tokenEntity.setToken(sign);
        if ( idToken != null) {
            tokenEntity.setAccessToken(accessToken);
            tokenEntity.setIdToken(idToken);
        }

        if (state != null && !state.isEmpty()) {
            tokenEntity.setState(state);
        }

        final Cookie bearerCookie = cookieGenerator.generate("Bearer%20" + sign);
        servletResponse.addCookie(bearerCookie);

        return Response
                .ok(tokenEntity)
                .build();
    }
}
