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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.exceptions.InvalidTokenException;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks.RemoteJWKSourceResolver;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */

@RunWith(MockitoJUnitRunner.class)
public class JWKSKeyProcessorTest extends AbstractKeyProcessorTest {

    private JWKSKeyProcessor<SecurityContext> keyProcessor;

    @Mock
    private RemoteJWKSourceResolver<SecurityContext> sourceResolver = mock(RemoteJWKSourceResolver.class);

    @Test
    public void processValidRSAToken() throws NoSuchAlgorithmException, JOSEException {
        KeyPair keyPair = getRsaKeyPair();
        String token = generateRSAToken(keyPair);
        keyProcessor = new JWKSKeyProcessor<>(claimsVerifier);
        keyProcessor.setJwkSourceResolver(sourceResolver);

        when(sourceResolver.resolve()).thenReturn(new ImmutableJWKSet<>(generateJWKSConfiguration(keyPair, JWSAlgorithm.RS256, RSA_KID)));
        JWTClaimsSet process = keyProcessor.process(Algorithm.RS256, token);
        assertNotNull(process);
    }

    @Test(expected = InvalidTokenException.class)
    public void shouldNotProcessValidRSATokenWithoutRightKey() throws NoSuchAlgorithmException, JOSEException {
        KeyPair keyPair = getRsaKeyPair();
        String token = generateRSAToken(keyPair);
        keyProcessor = new JWKSKeyProcessor<>(claimsVerifier);
        keyProcessor.setJwkSourceResolver(sourceResolver);
        when(sourceResolver.resolve()).thenReturn(new ImmutableJWKSet<>(generateJWKSConfiguration(keyPair, JWSAlgorithm.RS256, EC_KID)));
        keyProcessor.process(Algorithm.RS256, token);
        fail("Should not process a token without its corresponding key");
    }

    @Test
    public void processValidECToken() throws NoSuchAlgorithmException, JOSEException {
        KeyPair keyPair = getCEKeyPair();
        String token = generateECToken(keyPair);
        keyProcessor = new JWKSKeyProcessor<>(claimsVerifier);
        keyProcessor.setJwkSourceResolver(sourceResolver);
        when(sourceResolver.resolve()).thenReturn(new ImmutableJWKSet<>(generateJWKSConfiguration(keyPair, JWSAlgorithm.ES256, EC_KID)));
        JWTClaimsSet process = keyProcessor.process(Algorithm.ES256, token);
        assertNotNull(process);
    }

    @Test(expected = InvalidTokenException.class)
    public void shouldNotProcessValidECTokenWithoutRightKey() throws NoSuchAlgorithmException, JOSEException {
        KeyPair keyPair = getCEKeyPair();
        String token = generateECToken(keyPair);
        keyProcessor = new JWKSKeyProcessor<>(claimsVerifier);
        keyProcessor.setJwkSourceResolver(sourceResolver);
        when(sourceResolver.resolve()).thenReturn(new ImmutableJWKSet<>(generateJWKSConfiguration(keyPair, JWSAlgorithm.ES256, RSA_KID)));
        keyProcessor.process(Algorithm.ES256, token);
        fail("Should not process a token without its corresponding key");
    }

    private JWKSet generateJWKSConfiguration(KeyPair keyPair, JWSAlgorithm algorithm, String kid) {
        if (algorithm == JWSAlgorithm.RS256) {
            RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyID(kid)
                .algorithm(algorithm)
                .build();

            return new JWKSet(List.of(rsaKey));
        } else if (algorithm == JWSAlgorithm.ES256) {
            ECKey ecKey = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyID(kid)
                .algorithm(JWSAlgorithm.ES256)
                .build();
            return new JWKSet(List.of(ecKey));
        }

        return new JWKSet();
    }
}
