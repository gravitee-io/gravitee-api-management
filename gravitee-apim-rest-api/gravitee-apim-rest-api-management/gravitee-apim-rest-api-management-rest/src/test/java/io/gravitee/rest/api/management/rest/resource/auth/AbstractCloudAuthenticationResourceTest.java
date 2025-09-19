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

import static io.gravitee.rest.api.management.rest.resource.auth.CockpitAuthenticationResource.API_CLAIM;
import static io.gravitee.rest.api.management.rest.resource.auth.CockpitAuthenticationResource.APPLICATION_CLAIM;
import static io.gravitee.rest.api.management.rest.resource.auth.CockpitAuthenticationResource.ENVIRONMENT_CLAIM;
import static io.gravitee.rest.api.management.rest.resource.auth.CockpitAuthenticationResource.ORG_CLAIM;
import static io.gravitee.rest.api.management.rest.resource.auth.CockpitAuthenticationResource.REDIRECT_URI_CLAIM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.common.utils.UUID;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.common.JWTHelper;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author Guillaume Cusnieux (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractCloudAuthenticationResourceTest extends AbstractResourceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String PATH = "auth/cockpit";
    private static final String JWT_ISSUER = "test-issuer";
    private static final String JWT_SUBJECT = "test-user-id";
    private static final String ISSUER = "https://gravitee.cockpit";
    private static final String KID = "cockpit";
    private static final String ALGORITHM = "RS512";
    private static final int TTL_SECONDS = 10;

    @Autowired
    private InstallationAccessQueryService installationAccessQueryService;

    @Autowired
    private ApiSearchService apiSearchService;

    @Autowired
    protected Environment environment;

    @Override
    protected String contextPath() {
        return "";
    }

    @Override
    protected boolean followRedirect() {
        return false;
    }

    protected abstract String getPropertyPrefix();

    protected abstract String getKeystorePropertyPrefix();

    @Before
    public void init() {
        System.setProperty(getPropertyPrefix() + ".enabled", "true");
        System.setProperty(
            getKeystorePropertyPrefix() + ".keystore.path",
            new File(getClass().getClassLoader().getResource("auth/keystore.p12").getFile()).getAbsolutePath()
        );
        System.setProperty(getKeystorePropertyPrefix() + ".keystore.password", "keystore-secret");
        System.setProperty(getKeystorePropertyPrefix() + ".keystore.type", "PKCS12");
        System.setProperty(getKeystorePropertyPrefix() + ".keystore.key.alias", "cockpit-ca");
        System.setProperty(getPropertyPrefix() + ".connector.ws.ssl.keystore.type", "PKCS12");
        reset(userService);
    }

    @After
    public void teardown() {
        System.clearProperty(getPropertyPrefix() + ".enabled");
        System.clearProperty(getKeystorePropertyPrefix() + ".keystore.path");
        System.clearProperty(getKeystorePropertyPrefix() + ".keystore.password");
        System.clearProperty(getKeystorePropertyPrefix() + ".keystore.type");
        System.clearProperty(getKeystorePropertyPrefix() + ".keystore.key.alias");
        System.clearProperty(getPropertyPrefix() + ".connector.ws.ssl.keystore.type");
    }

    @Test
    public void shouldNotAuthenticateIfCockpitIsNotEnabled() {
        System.clearProperty(getPropertyPrefix() + ".enabled");
        final Response response = rootTarget(PATH).queryParam("token", "test").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateIfUserNotFound() {
        UserEntity userEntity = mockUserEntity();

        String jwt = this.generateJWT(userEntity.getId(), ORGANIZATION_ID, ENVIRONMENT_ID, "audience", null, null, null);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", userEntity.getId(), true)).thenThrow(
            new UserNotFoundException(JWT_SUBJECT)
        );
        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWithInvalidToken() throws JOSEException {
        UserEntity userEntity = mockUserEntity();
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", userEntity.getId(), true)).thenReturn(userEntity);
        when(authoritiesProvider.retrieveAuthorities(userEntity.getId(), ORGANIZATION_ID, ENVIRONMENT_ID)).thenReturn(Set.of());
        when(installationAccessQueryService.getConsoleUrl(ORGANIZATION_ID)).thenReturn(rootTarget("console").getUri().toString());
        final Response response = rootTarget(PATH).queryParam("token", "invalid").request().get();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void shouldAuthenticateToConsole() {
        UserEntity userEntity = mockUserEntity();

        String jwt = this.generateJWT(userEntity.getId(), ORGANIZATION_ID, ENVIRONMENT_ID, "audience", null, null, null);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", userEntity.getId(), true)).thenReturn(userEntity);
        when(authoritiesProvider.retrieveAuthorities(userEntity.getId(), ORGANIZATION_ID, ENVIRONMENT_ID)).thenReturn(Set.of());
        when(installationAccessQueryService.getConsoleUrl(ORGANIZATION_ID)).thenReturn(rootTarget("console").getUri().toString());
        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.TEMPORARY_REDIRECT_307, response.getStatus());
        assertTrue(response.getLocation().toString().contains("/console/#!/environment-id/"));
    }

    @Test
    public void shouldAuthenticateToConsoleAndRedirectToApi() {
        UserEntity userEntity = mockUserEntity();

        String jwt = this.generateJWT(userEntity.getId(), ORGANIZATION_ID, ENVIRONMENT_ID, "audience", "apiId", null, null);
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", userEntity.getId(), true)).thenReturn(userEntity);
        when(authoritiesProvider.retrieveAuthorities(userEntity.getId(), ORGANIZATION_ID, ENVIRONMENT_ID)).thenReturn(Set.of());
        when(installationAccessQueryService.getConsoleUrl(ORGANIZATION_ID)).thenReturn(rootTarget("console").getUri().toString());
        when(apiSearchService.findIdByEnvironmentIdAndCrossId(ENVIRONMENT_ID, "apiId")).thenReturn(Optional.of("apiId"));
        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.TEMPORARY_REDIRECT_307, response.getStatus());
        assertTrue(response.getLocation().toString().contains("/console/#!/environment-id/apis/apiId"));
    }

    @Test
    public void shouldAuthenticateToPortal() {
        UserEntity userEntity = mockUserEntity();

        String jwt = this.generateJWT(userEntity.getId(), ORGANIZATION_ID, ENVIRONMENT_ID, "audience", null, null, "portal");
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", userEntity.getId(), true)).thenReturn(userEntity);
        when(authoritiesProvider.retrieveAuthorities(userEntity.getId(), ORGANIZATION_ID, ENVIRONMENT_ID)).thenReturn(Set.of());
        when(installationAccessQueryService.getPortalAPIUrl(ENVIRONMENT_ID)).thenReturn(rootTarget("portal").getUri().toString());
        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.TEMPORARY_REDIRECT_307, response.getStatus());
        assertTrue(response.getLocation().toString().contains("/portal/environments/environment-id/auth/console?token="));
        assertToken(response, userEntity);
    }

    @Test
    public void shouldAuthenticateToPortalFromRequest() {
        UserEntity userEntity = mockUserEntity();

        String jwt = this.generateJWT(userEntity.getId(), ORGANIZATION_ID, ENVIRONMENT_ID, "audience", null, null, "portal");
        when(userService.findBySource(ORGANIZATION_ID, "cockpit", userEntity.getId(), true)).thenReturn(userEntity);
        when(authoritiesProvider.retrieveAuthorities(userEntity.getId(), ORGANIZATION_ID, ENVIRONMENT_ID)).thenReturn(Set.of());
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer(rootTarget(PATH).getUri().toString()));

        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.TEMPORARY_REDIRECT_307, response.getStatus());
        assertTrue(response.getLocation().toString().contains("/portal/environments/environment-id/auth/console?token="));
        assertToken(response, userEntity);
    }

    private void assertToken(Response response, UserEntity userEntity) {
        String token = URLEncodedUtils.parse(response.getLocation(), "UTF-8")
            .stream()
            .filter(pair -> "token".equalsIgnoreCase(pair.getName()))
            .findFirst()
            .get()
            .getValue();
        Algorithm algorithm = Algorithm.HMAC256(environment.getProperty("jwt.secret"));
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        final DecodedJWT decodedJwt = jwtVerifier.verify(token);
        assertNotNull(decodedJwt);
        assertNotNull(decodedJwt.getId());
        assertNotNull(decodedJwt.getExpiresAt());
        assertNotNull(decodedJwt.getSubject());
        assertNotNull(decodedJwt.getIssuer());
        assertNotNull(decodedJwt.getIssuedAt());
        assertNotNull(decodedJwt.getClaim(JWTHelper.Claims.PERMISSIONS));
        assertEquals(userEntity.getEmail(), decodedJwt.getClaim(JWTHelper.Claims.EMAIL).asString());
        assertEquals(userEntity.getFirstname(), decodedJwt.getClaim(JWTHelper.Claims.FIRSTNAME).asString());
        assertEquals(userEntity.getLastname(), decodedJwt.getClaim(JWTHelper.Claims.LASTNAME).asString());
        assertEquals(ORGANIZATION_ID, decodedJwt.getClaim(JWTHelper.Claims.ORG).asString());
    }

    private KeyStore loadKeyStore(String type, String path, String password) throws CertificateException {
        if (password == null) {
            throw new CertificateException("keystore password can't be null");
        }
        try (InputStream is = new File(path).toURI().toURL().openStream()) {
            final KeyStore keystore = KeyStore.getInstance(type);
            keystore.load(is, password.toCharArray());
            return keystore;
        } catch (Exception e) {
            throw new CertificateException(String.format("Unable to load keystore %s", path), e);
        }
    }

    private String generateJWT(
        String userId,
        String organizationId,
        String environmentId,
        String audience,
        String apiId,
        String redirectUri,
        String application
    ) {
        var issueTime = TimeProvider.now();
        var expirationTime = issueTime.plusSeconds(TTL_SECONDS);
        try {
            KeyStore keystore = this.loadKeyStore(
                System.getProperty(getKeystorePropertyPrefix() + ".keystore.type"),
                System.getProperty(getKeystorePropertyPrefix() + ".keystore.path"),
                System.getProperty(getKeystorePropertyPrefix() + ".keystore.password")
            );
            PrivateKey privateKey = (PrivateKey) keystore.getKey(
                System.getProperty(getKeystorePropertyPrefix() + ".keystore.key.alias"),
                System.getProperty(getKeystorePropertyPrefix() + ".keystore.password").toCharArray()
            );

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .jwtID(UUID.random().toString())
                .issuer(ISSUER)
                .issueTime(new Date(issueTime.toInstant().toEpochMilli()))
                .expirationTime(new Date(expirationTime.toInstant().toEpochMilli()))
                .subject(userId)
                .audience(audience)
                .claim(ORG_CLAIM, organizationId)
                .claim(ENVIRONMENT_CLAIM, environmentId)
                .claim(API_CLAIM, apiId)
                .claim(APPLICATION_CLAIM, application)
                .claim(REDIRECT_URI_CLAIM, redirectUri);
            var claims = claimsBuilder.build();

            // Sign then serialize the JWT.
            var signer = new RSASSASigner(privateKey);
            var header = new JWSHeader.Builder(new JWSAlgorithm(ALGORITHM)).keyID(KID).build();
            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (CertificateException | UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private UserEntity mockUserEntity() {
        UserEntity createdUser = new UserEntity();
        createdUser.setId(JWT_SUBJECT);
        createdUser.setOrganizationId(ORGANIZATION_ID);
        createdUser.setSource(JWT_ISSUER);
        createdUser.setSourceId(JWT_SUBJECT);
        createdUser.setLastname("Doe");
        createdUser.setFirstname("Jane");
        createdUser.setEmail("janedoe@example.com");
        createdUser.setPicture("http://example.com/janedoe/me.jpg");
        return createdUser;
    }
}
