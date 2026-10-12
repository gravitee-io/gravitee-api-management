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
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.gravitee.apim.rest.api.mcp.security.oauth.IdentityCache;
import io.gravitee.apim.rest.api.mcp.security.oauth.IdentityProviderTokenIntrospector;
import io.gravitee.apim.rest.api.mcp.security.oauth.McpProtectedResource;
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
import io.gravitee.rest.api.service.exceptions.TokenNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a bearer token on the MCP mount: an APIM token (JWT or personal access token), or, where OAuth protects
 * the URL, an access token of the organization's identity provider.
 *
 * The shared {@code TokenAuthenticationFilter} resolves a personal access token through the organization of the
 * request context, which the bare mount does not have: here the user is looked up first, and the organization is his.
 * When the URL names an organization, the token must belong to it.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class McpTokenAuthenticationFilter extends OncePerRequestFilter {

    static final String TOKEN_SOURCE = "token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTVerifier jwtVerifier;
    private final String jwtIssuer;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthoritiesProvider authoritiesProvider;
    private final IdentityProviderTokenIntrospector introspector;
    private final IdentityCache<Identity> identities = new IdentityCache<>();

    public McpTokenAuthenticationFilter(
        String jwtSecret,
        String jwtIssuer,
        TokenService tokenService,
        UserRepository userRepository,
        UserService userService,
        AuthoritiesProvider authoritiesProvider,
        IdentityProviderTokenIntrospector introspector
    ) {
        this.jwtVerifier = JWT.require(Algorithm.HMAC256(jwtSecret)).build();
        this.jwtIssuer = jwtIssuer;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.authoritiesProvider = authoritiesProvider;
        this.introspector = introspector;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        var value = request.getHeader(HttpHeaders.AUTHORIZATION).substring(BEARER_PREFIX.length()).trim();
        var resource = McpProtectedResource.of(request).orElseThrow(() ->
            new IllegalStateException("The MCP protected resource filter must run before the token filter")
        );
        Identity identity;
        try {
            identity = authenticate(value, resource);
            var mount = resource.mount();
            if (mount.explicit() && !mount.organizationId().equals(identity.organizationId())) {
                throw new IllegalStateException(
                    "the token belongs to organization " + identity.organizationId() + ", not to " + mount.organizationId()
                );
            }
        } catch (Exception e) {
            log.warn("Rejected an MCP request with an invalid bearer token: {}", reason(e));
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, BearerOnlyFilter.bearerChallenge(request, "error=\"invalid_token\""));
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

    /** The messages of these exceptions embed what the client sent, which may be a valid credential elsewhere. */
    private static String reason(Exception e) {
        return e instanceof TokenNotFoundException || e instanceof JWTDecodeException ? "unknown or malformed token" : e.getMessage();
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

    /** APIM tokens never reach the identity provider: only a token APIM does not know is introspected there. */
    private Identity authenticate(String value, McpProtectedResource resource) throws TechnicalException {
        var mountResource = resource.mount().resource();
        var cached = mountResource == null ? Optional.<Identity>empty() : identities.get(mountResource, value);
        if (cached.isPresent()) {
            return cached.get();
        }
        if (value.contains(".")) {
            return isApimJwt(value, resource) ? fromJwt(value) : fromIdentityProvider(value, resource);
        }
        try {
            return fromPersonalAccessToken(value);
        } catch (TokenNotFoundException e) {
            if (resource.authorizationServer().isEmpty()) {
                throw e;
            }
            return fromIdentityProvider(value, resource);
        }
    }

    private boolean isApimJwt(String value, McpProtectedResource resource) {
        try {
            var issuer = JWT.decode(value).getIssuer();
            return issuer == null || issuer.equals(jwtIssuer);
        } catch (JWTDecodeException e) {
            return resource.authorizationServer().isEmpty();
        }
    }

    private Identity fromIdentityProvider(String value, McpProtectedResource resource) {
        var mount = resource.mount();
        var authorizationServer = resource
            .authorizationServer()
            .orElseThrow(() ->
                new IllegalStateException("it is no APIM token, and OAuth is off for organization " + mount.organizationId())
            );
        var identityProvider = authorizationServer.identityProvider();
        var token = introspector.introspect(identityProvider, authorizationServer.credentials(), value, mount.resource());
        var executionContext = new ExecutionContext(mount.organizationId());
        GraviteeContext.fromExecutionContext(executionContext);
        // The introspection response is the profile: unlike userinfo, it does not need the token to carry the openid
        // scope, and its sub is what login stores as the user's sourceId under the default mapping.
        var user = userService.createOrUpdateUserFromSocialIdentityProvider(
            executionContext,
            identityProvider,
            token.claims().toString(),
            value,
            null
        );
        var identity = new Identity(
            user.getId(),
            user.getOrganizationId(),
            user.getEmail(),
            user.getFirstname(),
            user.getLastname(),
            identityProvider.getId(),
            user.getSourceId()
        );
        identities.put(mount.resource(), value, identity, token.expiresAt());
        return identity;
    }

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
