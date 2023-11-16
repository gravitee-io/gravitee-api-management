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

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.exceptions.InvalidTokenException;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks.MACJWKSourceResolver;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HMACKeyProcessorTest extends AbstractKeyProcessorTest {

    private HMACKeyProcessor<SecurityContext> keyProcessor;

    @Test
    public void processValidTokenAndKey() throws JOSEException {
        keyProcessor = new HMACKeyProcessor<>(claimsVerifier);
        String key = generateKey();
        String token = generateMacToken(key);
        keyProcessor.setJwkSourceResolver(new MACJWKSourceResolver<>(() -> key));
        JWTClaimsSet process = keyProcessor.process(Algorithm.HS256, token);
        Assert.assertNotNull(process);
    }

    @Test(expected = InvalidTokenException.class)
    public void shouldNotProcessValidTokenAndKeyButWrongAlgorithm() throws JOSEException {
        keyProcessor = new HMACKeyProcessor<>(claimsVerifier);
        String key = generateKey();
        String token = generateMacToken(key);
        keyProcessor.setJwkSourceResolver(new MACJWKSourceResolver<>(() -> key));
        keyProcessor.process(Algorithm.RS256, token);
        Assert.fail("should not process a token with wrong algorithm");
    }

    @Test(expected = KeyLengthException.class)
    public void shouldNotProcessValidTokenAndWrongKey() throws JOSEException {
        keyProcessor = new HMACKeyProcessor<>(claimsVerifier);
        String key = "ozhbx5HJCS41NzKrBSQ0vZU1WO";
        String token = generateMacToken(key);
        keyProcessor.setJwkSourceResolver(new MACJWKSourceResolver<>(() -> key));
        keyProcessor.process(Algorithm.HS256, token);
        Assert.fail("should not process a token generated with a short key");
    }

    private static String generateMacToken(String key) throws JOSEException {
        System.setProperty("auth.external.verificationKey", key);
        System.setProperty("auth.external.algorithm", Algorithm.HS256.alg);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build();

        JWTClaimsSet payload = getJwtClaimsSet();

        SignedJWT signedJWT = new SignedJWT(header, payload);
        signedJWT.sign(new MACSigner(key));
        return signedJWT.serialize();
    }

    private static String generateKey() {
        // Generate random 256-bit (32-byte) shared secret
        SecureRandom random = new SecureRandom();
        byte[] sharedSecret = new byte[32];
        random.nextBytes(sharedSecret);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sharedSecret);
    }
}
