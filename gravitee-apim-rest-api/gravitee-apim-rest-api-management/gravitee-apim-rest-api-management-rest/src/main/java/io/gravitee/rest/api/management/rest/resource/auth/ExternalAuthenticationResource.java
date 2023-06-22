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

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.AbstractKeyProcessor;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.HMACKeyProcessor;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.RSAKeyProcessor;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.alg.Signature;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks.MACJWKSourceResolver;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks.RSAJWKSourceResolver;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.resolver.SignatureKeyResolver;
import io.gravitee.rest.api.model.NewExternalUserEntity;
import io.gravitee.rest.api.model.UpdateUserEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Singleton
@Path("/auth/external")
public class ExternalAuthenticationResource extends AbstractAuthenticationResource {

    protected static final String ORG_CLAIM = "org";
    protected static final String ENVIRONMENT_CLAIM = "env";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalAuthenticationResource.class);

    @Autowired
    protected MembershipService membershipService;

    @Autowired
    protected CookieGenerator cookieGenerator;

    private boolean enabled;

    private AbstractKeyProcessor<?> keyProcessor;

    private Signature algorithm;

    @Autowired
    private UserService userService;

    @Autowired
    private Environment environment;

    @Autowired
    private AuthoritiesProvider authoritiesProvider;

    private String uiURL;

    @PostConstruct
    public void afterPropertiesSet() {
        enabled = environment.getProperty("auth.external.enabled", Boolean.class, false);

        if (enabled) {
            uiURL = environment.getProperty("console.ui.url", "http://localhost:3000");
            try {
                algorithm = environment.getProperty("auth.external.algorithm", Signature.class);
                SignatureKeyResolver keyResolver = () -> environment.getProperty("auth.external.signature");

                String issuer = environment.getProperty("auth.external.issuer", String.class);

                JWTClaimsSet claimsSet = null;
                if (issuer != null) {
                    claimsSet = new JWTClaimsSet.Builder().issuer(issuer).build();
                }

                final DefaultJWTClaimsVerifier claimsVerifier = new DefaultJWTClaimsVerifier(
                        claimsSet, new HashSet<>(Arrays.asList("sub", "iss", "iat")));

                switch (algorithm) {
                        case RSA_RS256:
                        case RSA_RS384:
                        case RSA_RS512:
                            keyProcessor = new RSAKeyProcessor<>(claimsVerifier);
                            keyProcessor.setJwkSourceResolver(new RSAJWKSourceResolver<>(keyResolver));
                            break;
                        case HMAC_HS256:
                        case HMAC_HS384:
                        case HMAC_HS512:
                            keyProcessor = new HMACKeyProcessor<>(claimsVerifier);
                            keyProcessor.setJwkSourceResolver(new MACJWKSourceResolver<>(keyResolver));
                            break;
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
            JWTClaimsSet jwtClaimsSet = keyProcessor
                    .process(algorithm, token)
                    .get();

            // Current organization must be set to those coming from cockpit token.
            String organizationId = jwtClaimsSet.getStringClaim(ORG_CLAIM);
            if (organizationId == null) {
                organizationId = "DEFAULT";
            }

            // Set user to Authentication Context
            String environmentId = jwtClaimsSet.getStringClaim(ENVIRONMENT_CLAIM);
            if (environmentId == null) {
                environmentId = "DEFAULT";
            }

            GraviteeContext.setCurrentEnvironment(environmentId);

            GraviteeContext.setCurrentOrganization(organizationId);

            // Retrieve the user or try to create unknown user
            UserEntity user;
            try {
                user = userService.findBySource(
                    GraviteeContext.getExecutionContext(),
                        jwtClaimsSet.getIssuer(),
                    jwtClaimsSet.getSubject(),
                    true
                );

                UpdateUserEntity userEntity = new UpdateUserEntity();
                userEntity.setEmail(jwtClaimsSet.getStringClaim("email"));
                userEntity.setLastname(jwtClaimsSet.getStringClaim("family_name"));
                userEntity.setFirstname(jwtClaimsSet.getStringClaim("given_name"));
                userEntity.setPicture(jwtClaimsSet.getStringClaim("picture"));
                user = userService.update(GraviteeContext.getExecutionContext(), user.getId(), userEntity);
            } catch (UserNotFoundException unfe) {
                NewExternalUserEntity userEntity = new NewExternalUserEntity();
                userEntity.setEmail(jwtClaimsSet.getStringClaim("email"));
                userEntity.setLastname(jwtClaimsSet.getStringClaim("family_name"));
                userEntity.setFirstname(jwtClaimsSet.getStringClaim("given_name"));
                userEntity.setSource(jwtClaimsSet.getIssuer());
                userEntity.setSourceId(jwtClaimsSet.getSubject());
                userEntity.setPicture(jwtClaimsSet.getStringClaim("picture"));
                user = userService.create(GraviteeContext.getExecutionContext(), userEntity, true);
            }

            final Set<GrantedAuthority> authorities = authoritiesProvider.retrieveAuthorities(user.getId(), organizationId, environmentId);

            UserDetails userDetails = new UserDetails(user.getId(), "", authorities);
            userDetails.setEmail(user.getEmail());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, authorities));

            // External user is authenticated, connect user (ie: generate cookie).
            super.connectUser(user, httpResponse);

            String url = String.format(
                    uiURL + "?organization=%s/#!/environments/%s",
                organizationId,
                environmentId
            );

            // Redirect the user.
            return Response
                    .temporaryRedirect(new URI(url))
                    .build();
        } catch (Exception e) {
            LOGGER.error("Error occurred when trying to log user using external authentication provider.", e);
            return Response.serverError().build();
        } finally {
            GraviteeContext.cleanContext();
        }
    }
}
