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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.rest.api.service.common.JWTHelper.ACTION.USER_REGISTRATION;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_EXPIRE_AFTER;
import static io.gravitee.rest.api.service.common.JWTHelper.DefaultValues.DEFAULT_JWT_ISSUER;
import static javax.ws.rs.core.Response.ok;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.gravitee.common.util.Maps;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;


import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.portal.rest.model.Token;
import io.gravitee.rest.api.portal.rest.model.Token.TokenTypeEnum;
import io.gravitee.rest.api.portal.rest.resource.auth.OAuth2AuthenticationResource;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper.Claims;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AuthResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;
    @Context
    private HttpServletResponse response;
    
    @Autowired
    private ConfigurableEnvironment environment;
    @Autowired
    private CookieGenerator cookieGenerator;

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(final @Context javax.ws.rs.core.HttpHeaders headers, final @Context HttpServletResponse servletResponse) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            // JWT signer
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

            Algorithm algorithm = Algorithm.HMAC256(environment.getProperty("jwt.secret"));

            Date issueAt = new Date();
            Instant expireAt = issueAt.toInstant().plus(Duration.ofSeconds(environment.getProperty("jwt.expire-after", Integer.class, DEFAULT_JWT_EXPIRE_AFTER)));

            final String sign = JWT.create()
                    .withIssuer(environment.getProperty("jwt.issuer", DEFAULT_JWT_ISSUER))
                    .withIssuedAt(issueAt)
                    .withExpiresAt(Date.from(expireAt))
                    .withSubject(userDetails.getUsername())
                    .withClaim(Claims.PERMISSIONS, authorities)
                    .withClaim(Claims.EMAIL, userDetails.getEmail())
                    .withClaim(Claims.FIRSTNAME, userDetails.getFirstname())
                    .withClaim(Claims.LASTNAME, userDetails.getLastname())
                    .withJWTId(UUID.randomUUID().toString())
                    .sign(algorithm);

            final Token tokenEntity = new Token();
            tokenEntity.setTokenType(TokenTypeEnum.BEARER);
            tokenEntity.setToken(sign);

            final Cookie bearerCookie = cookieGenerator.generate("Bearer%20" + sign);
            servletResponse.addCookie(bearerCookie);

            return ok(tokenEntity).build();
        }
        return ok().build();
    }

    @POST
    @Path("/logout")
    public Response logout() {
        response.addCookie(cookieGenerator.generate(null));
        return Response.ok().build();
    }
    
    
    @Path("/oauth2/{identity}")
    public OAuth2AuthenticationResource getOAuth2AuthenticationResource() {
        return resourceContext.getResource(OAuth2AuthenticationResource.class);
    }
    
}
