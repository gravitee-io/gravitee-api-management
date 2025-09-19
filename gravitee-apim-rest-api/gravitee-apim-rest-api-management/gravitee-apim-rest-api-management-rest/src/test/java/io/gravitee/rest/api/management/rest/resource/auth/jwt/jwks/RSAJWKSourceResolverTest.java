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
package io.gravitee.rest.api.management.rest.resource.auth.jwt.jwks;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.nimbusds.jose.proc.SecurityContext;
import io.gravitee.rest.api.management.rest.resource.auth.jwt.exceptions.InvalidKeyException;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.Test;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RSAJWKSourceResolverTest {

    private RSAJWKSourceResolver<SecurityContext> sourceResolver;

    @Test
    public void resolveValidPublicKey() throws IOException, NoSuchAlgorithmException {
        KeyPair keyPair = generateKeyPair();
        StringWriter writer = new StringWriter();
        PemWriter pemWriter = new PemWriter(writer);
        pemWriter.writeObject(new PemObject("PUBLIC KEY", keyPair.getPublic().getEncoded()));
        pemWriter.flush();
        pemWriter.close();

        sourceResolver = new RSAJWKSourceResolver<>(writer::toString);
        assertNotNull(sourceResolver.resolve());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotResolveInvalidPublicKey() throws NoSuchAlgorithmException {
        KeyPair keyPair = generateKeyPair();

        sourceResolver = new RSAJWKSourceResolver<>(() ->
            Base64.getEncoder().withoutPadding().encodeToString(keyPair.getPublic().getEncoded())
        );
        sourceResolver.resolve();
        fail("Source resolver must fail for wrong public keys");
    }

    @Test(expected = InvalidKeyException.class)
    public void shouldNotResolveNullPublicKey() {
        sourceResolver = new RSAJWKSourceResolver<>(() -> null);
        sourceResolver.resolve();
        fail("Source resolver must fail for null public keys");
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }
}
