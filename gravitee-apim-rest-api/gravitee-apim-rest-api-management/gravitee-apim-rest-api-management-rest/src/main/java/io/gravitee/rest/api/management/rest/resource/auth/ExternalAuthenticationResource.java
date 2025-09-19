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
package io.gravitee.rest.api.management.rest.resource.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.*;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.exceptions.InvalidAlgorithmException;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.exceptions.InvalidTokenException;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks.ECJWKSourceResolver;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks.MACJWKSourceResolver;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks.RSAJWKSourceResolver;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks.RemoteJWKSourceResolver;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.resolver.PublicKeyResolver;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@Singleton
@Path("/auth/external")
public class ExternalAuthenticationResource extends AbstractAuthenticationResource {

    protected static final String ORG_CLAIM = "org";
    protected static final String ENVIRONMENT_CLAIM = "env";
    private boolean enabled;
    private Algorithm algorithm;
    private AbstractKeyProcessor<?> keyProcessor;

    @Autowired
    private AuthoritiesProvider authoritiesProvider;

    @Autowired
    private InstallationAccessQueryService installationAccessQueryService;

    @PostConstruct
    public void afterPropertiesSet() {
        enabled = environment.getProperty("auth.external.enabled", Boolean.class, false);

        if (enabled) {
            try {
                algorithm = environment.getProperty("auth.external.algorithm", Algorithm.class);
                if (algorithm == null) {
                    throw new InvalidAlgorithmException(
                        "Algorithm can not be null, it can be set using auth.external.algorithm in the gravitee config file"
                    );
                }

                PublicKeyResolver publicKeyResolver = () -> environment.getProperty("auth.external.verificationKey");
                if (publicKeyResolver.resolve() == null) {
                    throw new IllegalArgumentException(
                        "The Authentication key can not be null. You can either set a ULR to you existing JWKS or pass the public key directly"
                    );
                }

                String issuer = environment.getProperty("auth.external.issuer", String.class);

                JWTClaimsSet claimsSet = null;
                if (issuer != null) {
                    claimsSet = new JWTClaimsSet.Builder().issuer(issuer).build();
                }

                final DefaultJWTClaimsVerifier claimsVerifier = new DefaultJWTClaimsVerifier<>(
                    claimsSet,
                    new HashSet<>(Arrays.asList("sub", "iss", "iat"))
                );

                // init JWT key source (Remote URL or from configuration file)
                if (publicKeyResolver.resolve().toLowerCase().startsWith("http")) {
                    keyProcessor = new JWKSKeyProcessor(claimsVerifier);
                    keyProcessor.setJwkSourceResolver(new RemoteJWKSourceResolver<>(publicKeyResolver));
                } else {
                    switch (algorithm) {
                        case RS256, RS384, RS512: {
                            keyProcessor = new RSAECKeyProcessor<>(claimsVerifier);
                            keyProcessor.setJwkSourceResolver(new RSAJWKSourceResolver<>(publicKeyResolver));
                            break;
                        }
                        case HS256, HS384, HS512: {
                            keyProcessor = new HMACKeyProcessor<>(claimsVerifier);
                            keyProcessor.setJwkSourceResolver(new MACJWKSourceResolver<>(publicKeyResolver));
                            break;
                        }
                        case ES256, ES384, ES512: {
                            keyProcessor = new RSAECKeyProcessor<>(claimsVerifier);
                            keyProcessor.setJwkSourceResolver(new ECJWKSourceResolver<>(publicKeyResolver));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Unable to initialize external authentication resource", e);
            }
        }
    }

    @GET
    public Response tokenExchange(@QueryParam(value = "token") final String token, @Context final HttpServletResponse httpResponse) {
        if (!enabled) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            // Verify and get claims from token.
            JWTClaimsSet jwtClaimsSet = keyProcessor.process(algorithm, token);

            // Current organization must be set to those coming from cockpit token.
            String organizationId = jwtClaimsSet.getStringClaim(ORG_CLAIM);
            if (organizationId == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Set user to Authentication Context
            String environmentId = jwtClaimsSet.getStringClaim(ENVIRONMENT_CLAIM);
            if (environmentId == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            GraviteeContext.fromExecutionContext(new ExecutionContext(organizationId, environmentId));

            // Retrieve the user or try to create unknown user
            UserEntity user = userService.findBySource(
                GraviteeContext.getCurrentOrganization(),
                jwtClaimsSet.getIssuer(),
                jwtClaimsSet.getSubject(),
                true
            );

            final Set<GrantedAuthority> authorities = authoritiesProvider.retrieveAuthorities(user.getId(), organizationId, environmentId);

            UserDetails userDetails = new UserDetails(user.getId(), "", authorities);
            userDetails.setEmail(user.getEmail());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, authorities));

            // External user is authenticated, connect user (ie: generate cookie).
            super.connectUser(user, httpResponse);

            String url = String.format("%s/#!/%s", installationAccessQueryService.getConsoleUrl(organizationId), environmentId);

            // Redirect the user.
            return Response.temporaryRedirect(new URI(url)).build();
        } catch (InvalidTokenException | UserNotFoundException e) {
            log.error("Authentication failed", e);
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error occurred when trying to log user using external authentication provider.", e);
            return Response.serverError().build();
        } finally {
            GraviteeContext.cleanContext();
        }
    }
}
