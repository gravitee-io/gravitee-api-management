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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.Algorithm;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ExternalAuthenticationResourceTest extends AbstractResourceTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String JWT_ISSUER = "test-issuer";
    private static final String JWT_SUBJECT = "test-user-id";
    private static final String EC_KID = "ec-256";
    private static final String RSA_KID = "rsa-256";
    private static final String PATH = "auth/external";
    private static final Map<String, KeyPair> KEY_PAIRS = new ConcurrentHashMap<>();

    @Autowired
    private InstallationAccessQueryService installationAccessQueryService;

    @Override
    protected String contextPath() {
        return "";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        super.decorate(resourceConfig);
        Resource.Builder resourceBuilder = Resource.builder(JWKSResource.class);
        resourceConfig.registerResources(resourceBuilder.build());
    }

    @BeforeClass
    public static void initKeyPairs() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            keyPairGenerator.initialize(256, random);
            KEY_PAIRS.put(EC_KID, keyPairGenerator.generateKeyPair());

            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KEY_PAIRS.put(RSA_KID, keyPairGenerator.generateKeyPair());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before
    public void init() {
        System.setProperty("auth.external.enabled", "true");
        System.setProperty("auth.external.issuer", JWT_ISSUER);
        when(installationAccessQueryService.getConsoleUrl(ORGANIZATION_ID)).thenReturn(
            rootTarget(".well-known/jwks.json").getUri().toString()
        );

        reset(userService);
    }

    @Test
    public void shouldNotAuthenticateWithExternalAuthenticationIsNotEnabled() {
        System.clearProperty("auth.external.enabled");
        final Response response = rootTarget(PATH).queryParam("token", "test").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWhenUserNotFound() throws JOSEException {
        String jwt = generateMacToken();
        when(userService.findBySource(ORGANIZATION_ID, JWT_ISSUER, JWT_SUBJECT, true)).thenThrow(new UserNotFoundException(JWT_SUBJECT));

        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();

        // -- VERIFY
        verify(userService, times(1)).findBySource(ORGANIZATION_ID, JWT_ISSUER, JWT_SUBJECT, true);
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldAuthenticateWithValidMacToken() throws JOSEException {
        String jwt = generateMacToken();
        //mock DB find user by name
        UserEntity userEntity = mockUserEntity();
        when(userService.findBySource(ORGANIZATION_ID, JWT_ISSUER, JWT_SUBJECT, true)).thenReturn(userEntity);

        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWithInvalidValidMacToken() throws JOSEException {
        System.setProperty("auth.external.algorithm", Algorithm.HS256.alg);
        System.setProperty("auth.external.verificationKey", "ozhbx5HJCS41NzKrBSQ0vZU1WOmG0Uhm");

        final Response response = rootTarget(PATH).queryParam("token", "jwt").request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWithMissingOrgId() throws JOSEException, IOException {
        JWTClaimsSet payload = getJwtClaimsSet(JWT_ISSUER, JWT_SUBJECT, null, ENVIRONMENT_ID);
        String jwt = generateRSAToken(payload);
        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWithMissingEnvId() throws JOSEException, IOException {
        JWTClaimsSet payload = getJwtClaimsSet(JWT_ISSUER, JWT_SUBJECT, ORGANIZATION_ID, null);
        String jwt = generateRSAToken(payload);
        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWithWrongIssuer() throws JOSEException, IOException {
        JWTClaimsSet payload = getJwtClaimsSet("test", JWT_SUBJECT, ORGANIZATION_ID, null);
        String jwt = generateRSAToken(payload);
        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWithWrongUser() throws JOSEException, IOException {
        JWTClaimsSet payload = getJwtClaimsSet(JWT_ISSUER, "test", ORGANIZATION_ID, null);
        String jwt = generateRSAToken(payload);

        //mock DB find user by name
        UserEntity userEntity = mockUserEntity();
        when(userService.findBySource(ORGANIZATION_ID, JWT_ISSUER, JWT_SUBJECT, true)).thenReturn(userEntity);

        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWithExpiredToken() throws JOSEException, IOException {
        JWTClaimsSet payload = getJwtClaimsSet(
            JWT_ISSUER,
            JWT_SUBJECT,
            ORGANIZATION_ID,
            ENVIRONMENT_ID,
            Date.from(Instant.now().minus(2, ChronoUnit.MINUTES))
        );
        String jwt = generateRSAToken(payload);
        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldAuthenticateWithValidECToken() throws JOSEException, IOException {
        String jwt = generateECToken();
        //mock DB find user by name
        UserEntity userEntity = mockUserEntity();
        when(userService.findBySource(ORGANIZATION_ID, JWT_ISSUER, JWT_SUBJECT, true)).thenReturn(userEntity);

        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWithValidECTokenButBadlyFormattedKey() throws JOSEException, IOException {
        String jwt = generateECToken();
        System.setProperty(
            "auth.external.verificationKey",
            Base64.getEncoder().encodeToString(KEY_PAIRS.get(EC_KID).getPublic().getEncoded())
        );

        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWithValidECTokenWrongKeyId() throws JOSEException, IOException {
        KeyPair keyPair = KEY_PAIRS.get(EC_KID);
        String jwt = generateToken(keyPair, new ECDSASigner((ECPrivateKey) keyPair.getPrivate()), RSA_KID, Algorithm.ES256.alg);
        System.setProperty("auth.external.verificationKey", rootTarget(".well-known/jwks.json").getUriBuilder().toTemplate());

        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWithValidRSATokenButBadlyFormattedKey() throws JOSEException, IOException {
        String jwt = generateRSAToken();
        System.setProperty(
            "auth.external.verificationKey",
            Base64.getEncoder().encodeToString(KEY_PAIRS.get(RSA_KID).getPublic().getEncoded())
        );

        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWithValidRSATokenWrongKeyId() throws JOSEException, IOException {
        KeyPair keyPair = KEY_PAIRS.get(RSA_KID);
        String jwt = generateToken(keyPair, new RSASSASigner(keyPair.getPrivate()), EC_KID, Algorithm.RS256.alg);
        System.setProperty("auth.external.verificationKey", rootTarget(".well-known/jwks.json").getUriBuilder().toTemplate());

        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldAuthenticateWithValidRSAToken() throws JOSEException, IOException {
        String jwt = generateRSAToken();
        //mock DB find user by name
        UserEntity userEntity = mockUserEntity();
        when(userService.findBySource(ORGANIZATION_ID, JWT_ISSUER, JWT_SUBJECT, true)).thenReturn(userEntity);

        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldAuthenticateWithValidJWKSUrlAndRsaToken() throws JOSEException, IOException {
        String jwt = generateRSAToken();
        System.setProperty("auth.external.verificationKey", rootTarget(".well-known/jwks.json").getUriBuilder().toTemplate());
        //mock DB find user by name
        UserEntity userEntity = mockUserEntity();
        when(userService.findBySource(ORGANIZATION_ID, JWT_ISSUER, JWT_SUBJECT, true)).thenReturn(userEntity);

        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldAuthenticateWithValidJWKSUrlAndEcToken() throws JOSEException, IOException {
        String jwt = generateECToken();
        System.setProperty("auth.external.verificationKey", rootTarget(".well-known/jwks.json").getUriBuilder().toTemplate());
        //mock DB find user by name
        UserEntity userEntity = mockUserEntity();
        when(userService.findBySource(ORGANIZATION_ID, JWT_ISSUER, JWT_SUBJECT, true)).thenReturn(userEntity);

        final Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWithUnsupportedAlgorithm() throws JOSEException {
        String jwt = generateMacToken();
        System.setProperty("auth.external.algorithm", "PS256");
        Response response = rootTarget(PATH).queryParam("token", jwt).request().get();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void shouldNotAuthenticateWithoutVerificationKey() {
        System.setProperty("auth.external.algorithm", Algorithm.HS256.alg);
        System.clearProperty("auth.external.verificationKey");
        Response response = rootTarget(PATH).queryParam("token", "jwt").request().get();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    private static String generateMacToken() throws JOSEException {
        // Generate random 256-bit (32-byte) shared secret
        SecureRandom random = new SecureRandom();
        byte[] sharedSecret = new byte[32];
        random.nextBytes(sharedSecret);
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(sharedSecret);

        System.setProperty("auth.external.verificationKey", key);
        System.setProperty("auth.external.algorithm", Algorithm.HS256.alg);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build();

        JWTClaimsSet payload = getJwtClaimsSet();

        SignedJWT signedJWT = new SignedJWT(header, payload);
        signedJWT.sign(new MACSigner(key));
        return signedJWT.serialize();
    }

    private static String generateECToken() throws JOSEException, IOException {
        KeyPair keyPair = KEY_PAIRS.get(EC_KID);
        return generateToken(keyPair, new ECDSASigner((ECPrivateKey) keyPair.getPrivate()), EC_KID, Algorithm.ES256.alg);
    }

    private static String generateRSAToken() throws JOSEException, IOException {
        KeyPair keyPair = KEY_PAIRS.get(RSA_KID);
        return generateToken(keyPair, new RSASSASigner(keyPair.getPrivate()), RSA_KID, Algorithm.RS256.alg);
    }

    private static String generateRSAToken(JWTClaimsSet payload) throws JOSEException, IOException {
        KeyPair keyPair = KEY_PAIRS.get(RSA_KID);
        return generateToken(keyPair, new RSASSASigner(keyPair.getPrivate()), RSA_KID, Algorithm.RS256.alg, payload);
    }

    private static String generateToken(KeyPair keyPair, JWSSigner signer, String keyId, String algorithm)
        throws JOSEException, IOException {
        return generateToken(keyPair, signer, keyId, algorithm, getJwtClaimsSet());
    }

    private static String generateToken(KeyPair keyPair, JWSSigner signer, String keyId, String algorithm, JWTClaimsSet payload)
        throws JOSEException, IOException {
        StringWriter writer = new StringWriter();
        PemWriter pemWriter = new PemWriter(writer);
        pemWriter.writeObject(new PemObject("PUBLIC KEY", keyPair.getPublic().getEncoded()));
        pemWriter.flush();
        pemWriter.close();

        System.setProperty("auth.external.verificationKey", writer.toString());
        System.setProperty("auth.external.algorithm", algorithm);
        JWSHeader header = new JWSHeader.Builder(Algorithm.valueOf(algorithm).getAlgorithm()).type(JOSEObjectType.JWT).keyID(keyId).build();

        SignedJWT signedJWT = new SignedJWT(header, payload);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private static JWTClaimsSet getJwtClaimsSet() {
        return getJwtClaimsSet(JWT_ISSUER, JWT_SUBJECT, ORGANIZATION_ID, ENVIRONMENT_ID);
    }

    private static JWTClaimsSet getJwtClaimsSet(String iss, String sub, String org, String env) {
        return getJwtClaimsSet(iss, sub, org, env, Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)));
    }

    private static JWTClaimsSet getJwtClaimsSet(String iss, String sub, String org, String env, Date exp) {
        return new JWTClaimsSet.Builder()
            .issuer(iss)
            .subject(sub)
            .claim(ExternalAuthenticationResource.ORG_CLAIM, org)
            .claim(ExternalAuthenticationResource.ENVIRONMENT_CLAIM, env)
            .claim("token", "test-token")
            .issueTime(new Date())
            .expirationTime(exp)
            .build();
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

    @Path(".well-known/jwks.json")
    public static class JWKSResource {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response jwks() {
            KeyPair ecKeyPair = KEY_PAIRS.get(EC_KID);
            final ECKey ecKey = new ECKey.Builder(Curve.P_256, (ECPublicKey) ecKeyPair.getPublic())
                .privateKey(ecKeyPair.getPrivate())
                .keyID(EC_KID)
                .algorithm(JWSAlgorithm.ES256)
                .build();

            KeyPair rsaKeyPair = KEY_PAIRS.get(RSA_KID);
            final RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) rsaKeyPair.getPublic())
                .privateKey(rsaKeyPair.getPrivate())
                .keyID(RSA_KID)
                .algorithm(JWSAlgorithm.RS256)
                .build();

            return Response.ok(new JWKSet(List.of(ecKey, rsaKey)).toString()).build();
        }
    }
}
