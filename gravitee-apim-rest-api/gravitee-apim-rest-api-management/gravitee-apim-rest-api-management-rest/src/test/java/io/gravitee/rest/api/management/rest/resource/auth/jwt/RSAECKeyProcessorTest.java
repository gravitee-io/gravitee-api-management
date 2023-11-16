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
package io.gravitee.rest.api.management.rest.resource.auth.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.exceptions.InvalidTokenException;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks.ECJWKSourceResolver;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks.RSAJWKSourceResolver;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RSAECKeyProcessorTest extends AbstractKeyProcessorTest {

    private RSAECKeyProcessor<SecurityContext> keyProcessor;

    @Test
    public void processValidRSATokenAndAlgorithm() throws NoSuchAlgorithmException, JOSEException {
        keyProcessor = new RSAECKeyProcessor<>(claimsVerifier);
        KeyPair keyPair = getRsaKeyPair();
        String token = generateRSAToken(keyPair);
        keyProcessor.setJwkSourceResolver(new RSAJWKSourceResolver<>(() -> formatPem(keyPair.getPublic())));
        JWTClaimsSet process = keyProcessor.process(Algorithm.RS256, token);
        Assert.assertNotNull(process);
    }

    @Test(expected = InvalidTokenException.class)
    public void shouldNotProcessValidRSATokenAndWrongAlgorithm() throws NoSuchAlgorithmException, JOSEException {
        keyProcessor = new RSAECKeyProcessor<>(claimsVerifier);
        KeyPair keyPair = getRsaKeyPair();
        String token = generateRSAToken(keyPair);
        keyProcessor.setJwkSourceResolver(new RSAJWKSourceResolver<>(() -> formatPem(keyPair.getPublic())));
        keyProcessor.process(Algorithm.ES256, token);
        Assert.fail("should not parse token with wrong algorithm");
    }

    @Test
    public void processValidECTokenAndAlgorithm() throws NoSuchAlgorithmException, JOSEException {
        keyProcessor = new RSAECKeyProcessor<>(claimsVerifier);
        KeyPair keyPair = getCEKeyPair();
        String token = generateECToken(keyPair);
        keyProcessor.setJwkSourceResolver(new ECJWKSourceResolver<>(() -> formatPem(keyPair.getPublic())));
        JWTClaimsSet process = keyProcessor.process(Algorithm.ES256, token);
        Assert.assertNotNull(process);
    }

    @Test(expected = InvalidTokenException.class)
    public void shouldNotProcessValidECTokenAndWrongAlgorithm() throws NoSuchAlgorithmException, JOSEException {
        keyProcessor = new RSAECKeyProcessor<>(claimsVerifier);
        KeyPair keyPair = getCEKeyPair();
        String token = generateECToken(keyPair);
        keyProcessor.setJwkSourceResolver(new ECJWKSourceResolver<>(() -> formatPem(keyPair.getPublic())));
        keyProcessor.process(Algorithm.RS256, token);
        Assert.fail("should not parse token with wrong algorithm");
    }
}
