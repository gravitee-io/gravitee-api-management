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
package io.gravitee.rest.api.management.rest.resource.auth;

import static io.gravitee.rest.api.management.rest.model.TokenType.BEARER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EXPIRE_AFTER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.util.Maps;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.management.rest.model.TokenEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.filter.TokenAuthenticationFilter;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class AbstractAuthenticationResource {

    public static final String CLIENT_ID_KEY = "client_id", REDIRECT_URI_KEY = "redirect_uri", CLIENT_SECRET = "client_secret", CODE_KEY =
        "code", GRANT_TYPE_KEY = "grant_type", AUTH_CODE = "authorization_code", TOKEN = "token", STATE = "state";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    protected Environment environment;

    @Autowired
    protected UserService userService;

    @Autowired
    protected MembershipService membershipService;

    @Autowired
    protected CookieGenerator cookieGenerator;

    protected Map<String, Object> getResponseEntity(final Response response) throws IOException {
        return getEntity((getResponseEntityAsString(response)));
    }

    protected String getResponseEntityAsString(final Response response) throws IOException {
        return response.readEntity(String.class);
    }

    protected Map<String, Object> getEntity(final String response) throws IOException {
        return MAPPER.readValue(response, new TypeReference<Map<String, Object>>() {});
    }

    protected Response connectUser(
        String userId,
        final String state,
        final HttpServletResponse servletResponse,
        final String accessToken,
        final String idToken
    ) {
        UserEntity user = userService.connect(userId);
        return this.connectUserInternal(user, state, servletResponse, accessToken, idToken);
    }

    protected void connectUser(UserEntity user, final HttpServletResponse servletResponse) {
        this.connectUserInternal(user, null, servletResponse, null, null);
    }

    protected Response connectUserInternal(
        UserEntity user,
        final String state,
        final HttpServletResponse servletResponse,
        final String accessToken,
        final String idToken
    ) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Manage authorities, initialize it with dynamic permissions from the IDP
        List<Map<String, String>> authorities = userDetails
            .getAuthorities()
            .stream()
            .map(authority -> Maps.<String, String>builder().put("authority", authority.getAuthority()).build())
            .collect(Collectors.toList());

        // We must also load permissions from repository for configured management or portal role
        Set<RoleEntity> userRoles = membershipService.getRoles(
            MembershipReferenceType.ORGANIZATION,
            GraviteeContext.getCurrentOrganization(),
            MembershipMemberType.USER,
            userDetails.getId()
        );
        if (!userRoles.isEmpty()) {
            userRoles.forEach(
                role ->
                    authorities.add(
                        Maps.<String, String>builder().put("authority", role.getScope().toString() + ':' + role.getName()).build()
                    )
            );
        }

        // JWT signer
        Algorithm algorithm = Algorithm.HMAC256(environment.getProperty("jwt.secret"));

        Date issueAt = new Date();
        Instant expireAt = issueAt
            .toInstant()
            .plus(Duration.ofSeconds(environment.getProperty("jwt.expire-after", Integer.class, DEFAULT_JWT_EXPIRE_AFTER)));

        final String token = JWT
            .create()
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

        final TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setType(BEARER);
        tokenEntity.setToken(token);
        if (idToken != null) {
            tokenEntity.setAccessToken(accessToken);
            tokenEntity.setIdToken(idToken);
        }

        if (state != null && !state.isEmpty()) {
            tokenEntity.setState(state);
        }

        final Cookie bearerCookie = cookieGenerator.generate(TokenAuthenticationFilter.AUTH_COOKIE_NAME, "Bearer%20" + token);
        servletResponse.addCookie(bearerCookie);

        return Response.ok(tokenEntity).build();
    }

    public static class Payload {

        @NotBlank
        String clientId;

        @NotBlank
        String redirectUri;

        @NotBlank
        String code;

        String state;

        public String getClientId() {
            return clientId;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public String getCode() {
            return code;
        }

        public String getState() {
            return state;
        }
    }
}
