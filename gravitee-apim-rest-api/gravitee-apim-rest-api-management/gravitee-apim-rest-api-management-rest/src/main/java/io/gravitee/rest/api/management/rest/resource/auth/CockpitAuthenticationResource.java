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

import static io.gravitee.rest.api.management.rest.resource.PortalRedirectResource.PROPERTY_HTTP_API_PORTAL_ENTRYPOINT;
import static io.gravitee.rest.api.management.rest.resource.PortalRedirectResource.PROPERTY_HTTP_API_PORTAL_PROXY_PATH;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.management.rest.model.TokenEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.security.cookies.CookieGenerator;
import io.gravitee.rest.api.security.utils.AuthoritiesProvider;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;
import java.util.Set;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Singleton
@Path("/auth/cockpit")
public class CockpitAuthenticationResource extends AbstractAuthenticationResource {

    protected static final String ORG_CLAIM = "org";
    protected static final String KID = "cockpit";
    protected static final String REDIRECT_URI_CLAIM = "redirect_uri";
    protected static final String ENVIRONMENT_CLAIM = "env";
    protected static final String API_CLAIM = "api";
    protected static final String APPLICATION_CLAIM = "app";
    private static final Logger LOGGER = LoggerFactory.getLogger(CockpitAuthenticationResource.class);
    private static final String COCKPIT_SOURCE = "cockpit";

    @Autowired
    protected MembershipService membershipService;

    @Autowired
    protected CookieGenerator cookieGenerator;

    private boolean enabled;
    private JWTProcessor<SecurityContext> jwtProcessor;

    @Autowired
    private UserService userService;

    @Autowired
    private Environment environment;

    @Autowired
    private AuthoritiesProvider authoritiesProvider;

    @Autowired
    private ApiSearchService apiSearchService;

    @Autowired
    private InstallationAccessQueryService installationAccessQueryService;

    @PostConstruct
    public void afterPropertiesSet() {
        enabled = getProperty("cockpit.enabled", "cloud.enabled", Boolean.class, false);

        if (enabled) {
            try {
                // Initialize the JWT processor which will handle JWT signature verification and assertions such as pollInterval time.
                final RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) getPublicKey()).keyID(KID).build();
                final JWSAlgorithmFamilyJWSKeySelector<SecurityContext> jwsKeySelector = new JWSAlgorithmFamilyJWSKeySelector<>(
                    JWSAlgorithm.Family.RSA,
                    new ImmutableJWKSet<>(new JWKSet(rsaKey))
                );

                // Note that the JWT processor can be configured with custom verifiers if we want more assertions (ex: validate iss, check org claims is defined, ...)
                final ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
                jwtProcessor.setJWSKeySelector(jwsKeySelector);
                this.jwtProcessor = jwtProcessor;
            } catch (Exception e) {
                throw new RuntimeException("Unable to initialize cockpit filter", e);
            }
        }
    }

    @GET
    public Response tokenExchange(
        @Context final HttpServletRequest httpServletRequest,
        @QueryParam(value = "token") final String token,
        @Context final HttpServletResponse httpResponse
    ) {
        if (!enabled) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            // Verify and get claims from token.
            final JWTClaimsSet jwtClaimsSet = jwtProcessor.process(token, null);

            // Initialize execution context from cockpit token.
            final String organizationId = jwtClaimsSet.getStringClaim(ORG_CLAIM);
            final String environmentId = jwtClaimsSet.getStringClaim(ENVIRONMENT_CLAIM);
            GraviteeContext.fromExecutionContext(new ExecutionContext(organizationId, environmentId));

            // Retrieve the user.
            final UserEntity user = userService.findBySource(organizationId, COCKPIT_SOURCE, jwtClaimsSet.getSubject(), true);

            //set user to Authentication Context
            final Set<GrantedAuthority> authorities = authoritiesProvider.retrieveAuthorities(user.getId(), organizationId, environmentId);

            UserDetails userDetails = new UserDetails(user.getId(), "", authorities);
            userDetails.setOrganizationId(user.getOrganizationId());
            userDetails.setEmail(user.getEmail());
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, authorities));

            // Cockpit user is authenticated, connect user (ie: generate cookie).
            super.connectUser(user, httpResponse);

            if ("PORTAL".equalsIgnoreCase(jwtClaimsSet.getStringClaim(APPLICATION_CLAIM))) {
                TokenEntity tokenEntity = generateToken(user, 30);
                String url = installationAccessQueryService.getPortalAPIUrl(environmentId);
                if (url == null) {
                    ServerHttpRequest request = new ServletServerHttpRequest(httpServletRequest);
                    UriComponents uriComponents = UriComponentsBuilder
                        .fromHttpRequest(request)
                        .replacePath(getProperty(PROPERTY_HTTP_API_PORTAL_ENTRYPOINT, PROPERTY_HTTP_API_PORTAL_PROXY_PATH, "/portal"))
                        .replaceQuery(null)
                        .build();
                    url = uriComponents.toUriString();
                }

                return Response
                    .temporaryRedirect(
                        new URI("%s/environments/%s/auth/console?token=%s".formatted(url, environmentId, tokenEntity.getToken()))
                    )
                    .build();
            } else {
                final String apiCrossId = jwtClaimsSet.getStringClaim(API_CLAIM);
                final String apiId = Optional
                    .ofNullable(apiCrossId)
                    .flatMap(crossId -> this.apiSearchService.findIdByEnvironmentIdAndCrossId(environmentId, crossId))
                    .orElse(null);

                String url = String.format(
                    "%s/#!/%s/%s",
                    installationAccessQueryService.getConsoleUrl(organizationId),
                    environmentId,
                    apiId == null ? "" : String.format("apis/%s", apiId)
                );

                // Redirect the user.
                return Response.temporaryRedirect(new URI(url)).build();
            }
        } catch (UserNotFoundException e) {
            LOGGER.error("Authentication failed", e);
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (Exception e) {
            LOGGER.error("Error occurred when trying to log user using cockpit.", e);
            return Response.serverError().build();
        } finally {
            GraviteeContext.cleanContext();
        }
    }

    private Key getPublicKey() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        final KeyStore trustStore = loadTrustStore();
        final Certificate cert = trustStore.getCertificate(
            getProperty("cockpit.keystore.key.alias", "cloud.connector.ws.ssl.keystore.key.alias", "cockpit-client")
        );

        return cert.getPublicKey();
    }

    private KeyStore loadTrustStore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final KeyStore keystore = KeyStore.getInstance(getProperty("cockpit.keystore.type", "cloud.connector.ws.ssl.keystore.type", null));

        try (
            InputStream is = new File(getProperty("cockpit.keystore.path", "cloud.connector.ws.ssl.keystore.path", null))
                .toURI()
                .toURL()
                .openStream()
        ) {
            final String password = getProperty("cockpit.keystore.password", "cloud.connector.ws.ssl.keystore.password", null);
            keystore.load(is, null == password ? null : password.toCharArray());
        }
        return keystore;
    }

    private String getProperty(final String property, final String fallback, final String defaultValue) {
        return getProperty(property, fallback, String.class, defaultValue);
    }

    <T> T getProperty(final String property, final String fallback, Class<T> targetType, final T defaultValue) {
        T value = environment.getProperty(property, targetType);
        if (value == null) {
            value = environment.getProperty(fallback, targetType);
        }
        return value != null ? value : defaultValue;
    }
}
