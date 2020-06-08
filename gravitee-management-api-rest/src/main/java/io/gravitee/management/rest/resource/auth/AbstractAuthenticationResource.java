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
package io.gravitee.management.rest.resource.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.util.Maps;
import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.rest.model.TokenEntity;
import io.gravitee.management.security.cookies.CookieGenerator;
import io.gravitee.management.security.filter.TokenAuthenticationFilter;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.common.JWTHelper;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.gravitee.management.rest.model.TokenType.BEARER;
import static io.gravitee.management.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EXPIRE_AFTER;
import static io.gravitee.management.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;

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

    public static final String CLIENT_ID_KEY = "client_id", REDIRECT_URI_KEY = "redirect_uri",
            CLIENT_SECRET = "client_secret", CODE_KEY = "code", GRANT_TYPE_KEY = "grant_type",
            AUTH_CODE = "authorization_code", TOKEN = "token", STATE = "state";

    protected Map<String, Object> getResponseEntity(final Response response) throws IOException {
        return getEntity((getResponseEntityAsString(response)));
    }

    protected String getResponseEntityAsString(final Response response) throws IOException {
        return response.readEntity(String.class);
    }

    protected Map<String, Object> getEntity(final String response) throws IOException {
        return MAPPER.readValue(response, new TypeReference<Map<String, Object>>() {});
    }

    protected Response connectUser(String userId,final String state, final HttpServletResponse servletResponse) {
        UserEntity user = userService.connect(userId);

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Manage authorities, initialize it with dynamic permissions from the IDP
        List<Map<String, String>> authorities = userDetails.getAuthorities().stream().map(authority -> Maps.<String, String>builder().put("authority", authority.getAuthority()).build()).collect(Collectors.toList());

        // We must also load permissions from repository for configured management or portal role
        RoleEntity role = membershipService.getRole(
                MembershipReferenceType.MANAGEMENT,
                MembershipDefaultReferenceId.DEFAULT.toString(),
                userDetails.getId(),
                RoleScope.MANAGEMENT);
        if (role != null) {
            authorities.add(Maps.<String, String>builder().put("authority", role.getScope().toString() + ':' + role.getName()).build());
        }

        role = membershipService.getRole(
                MembershipReferenceType.PORTAL,
                MembershipDefaultReferenceId.DEFAULT.toString(),
                userDetails.getId(),
                RoleScope.PORTAL);
        if (role != null) {
            authorities.add(Maps.<String, String>builder().put("authority", role.getScope().toString() + ':' + role.getName()).build());
        }

        // JWT signer
        Algorithm algorithm = Algorithm.HMAC256(environment.getProperty("jwt.secret"));

        Date issueAt = new Date();
        Instant expireAt = issueAt.toInstant().plus(Duration.ofSeconds(environment.getProperty("jwt.expire-after",
                Integer.class, DEFAULT_JWT_EXPIRE_AFTER)));

        final String token = JWT.create()
                .withIssuer(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER))
                .withIssuedAt(issueAt)
                .withExpiresAt(Date.from(expireAt))
                .withSubject(user.getId())
                .withClaim(JWTHelper.Claims.PERMISSIONS, authorities)
                .withClaim(JWTHelper.Claims.EMAIL, user.getEmail())
                .withClaim(JWTHelper.Claims.FIRSTNAME, user.getFirstname())
                .withClaim(JWTHelper.Claims.LASTNAME, user.getLastname())
                .sign(algorithm);

        final TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.setType(BEARER);
        tokenEntity.setToken(token);

        if (state != null && !state.isEmpty()) {
            tokenEntity.setState(state);
        }

        final Cookie bearerCookie = cookieGenerator.generate(TokenAuthenticationFilter.AUTH_COOKIE_NAME, "Bearer%20" + token);
        servletResponse.addCookie(bearerCookie);

        return Response
                .ok(tokenEntity)
                .build();
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
