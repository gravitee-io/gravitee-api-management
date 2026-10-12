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
package io.gravitee.apim.rest.api.mcp.security.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.Token;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.TokenService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.JWTHelper.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import lombok.CustomLog;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a bearer token on the MCP mount, whose path carries no organization.
 *
 * The shared {@code TokenAuthenticationFilter} resolves a personal access token through the organization of the
 * request context, which this mount does not have: here the user is looked up first, and the organization is his.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class McpTokenAuthenticationFilter extends OncePerRequestFilter {

    static final String TOKEN_SOURCE = "token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTVerifier jwtVerifier;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthoritiesProvider authoritiesProvider;

    public McpTokenAuthenticationFilter(
        String jwtSecret,
        TokenService tokenService,
        UserRepository userRepository,
        UserService userService,
        AuthoritiesProvider authoritiesProvider
    ) {
        this.jwtVerifier = JWT.require(Algorithm.HMAC256(jwtSecret)).build();
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.authoritiesProvider = authoritiesProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        var value = request.getHeader(HttpHeaders.AUTHORIZATION).substring(BEARER_PREFIX.length()).trim();
        Identity identity;
        try {
            identity = value.contains(".") ? fromJwt(value) : fromPersonalAccessToken(value);
        } catch (Exception e) {
            log.warn("Rejected an MCP request with an invalid bearer token: {}", e.getMessage());
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        // The authorities provider reads the organization from the request context; the environment falls back to the default one.
        GraviteeContext.fromExecutionContext(new ExecutionContext(identity.organizationId()));
        var authorities = authoritiesProvider.retrieveAuthorities(identity.userId());
        var user = new UserDetails(identity.userId(), "", authorities);
        user.setEmail(identity.email());
        user.setFirstname(identity.firstname());
        user.setLastname(identity.lastname());
        user.setSource(identity.source());
        user.setSourceId(identity.sourceId());
        user.setOrganizationId(identity.organizationId());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
        chain.doFilter(request, response);
    }

    private record Identity(
        String userId,
        String organizationId,
        String email,
        String firstname,
        String lastname,
        String source,
        String sourceId
    ) {}

    private Identity fromJwt(String value) {
        DecodedJWT jwt = jwtVerifier.verify(value);
        return new Identity(
            jwt.getSubject(),
            jwt.getClaim(Claims.ORG).asString(),
            jwt.getClaim(Claims.EMAIL).asString(),
            jwt.getClaim(Claims.FIRSTNAME).asString(),
            jwt.getClaim(Claims.LASTNAME).asString(),
            null,
            null
        );
    }

    private Identity fromPersonalAccessToken(String value) throws TechnicalException {
        Token token = tokenService.findByToken(value);
        if (token.getExpiresAt() != null && token.getExpiresAt().before(new Date())) {
            throw new IllegalStateException("token " + token.getId() + " expired on " + token.getExpiresAt());
        }
        var organizationId = userRepository
            .findById(token.getReferenceId())
            .orElseThrow(() -> new IllegalStateException("no user " + token.getReferenceId() + " for token " + token.getId()))
            .getOrganizationId();
        var entity = userService.findById(new ExecutionContext(organizationId), token.getReferenceId());
        return new Identity(
            entity.getId(),
            organizationId,
            entity.getEmail(),
            entity.getFirstname(),
            entity.getLastname(),
            TOKEN_SOURCE,
            token.getName()
        );
    }
}
