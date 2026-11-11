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
package io.gravitee.apim.common.secret;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * Encrypts a secret value under a data key as {@code v1:<keyId>:<base64(iv || ciphertext || tag)>}. The associated data
 * binds the value to its organization, environment and secret, so a value copied onto another secret fails to decrypt.
 */
public final class SecretEnvelope {

    private static final String VERSION = "v1";
    private static final char SEPARATOR = ':';

    private SecretEnvelope() {}

    public static String seal(DataKey key, SecretScope scope, String plaintext) {
        try {
            var encrypted = AesGcm.encrypt(AesGcm.key(key.material()), aad(scope), plaintext.getBytes(StandardCharsets.UTF_8));
            return VERSION + SEPARATOR + key.id() + SEPARATOR + Base64.getEncoder().encodeToString(encrypted);
        } catch (GeneralSecurityException e) {
            throw new SecretCryptoException("Failed to encrypt the value of secret " + scope.secretId(), e);
        }
    }

    public static String open(DataKey key, SecretScope scope, String envelope) {
        var parsed = parse(envelope);
        if (!parsed.keyId().equals(key.id())) {
            throw new SecretCryptoException("The value of secret " + scope.secretId() + " was not encrypted under data key " + key.id());
        }
        try {
            var decrypted = AesGcm.decrypt(AesGcm.key(key.material()), aad(scope), parsed.payload());
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new SecretCryptoException("Failed to decrypt the value of secret " + scope.secretId(), e);
        }
    }

    // Ids never hold NUL, so two different (organization, environment, secret) triples never give the same bytes.
    private static byte[] aad(SecretScope scope) {
        return String.join("\0", scope.organizationId(), scope.environmentId(), scope.secretId()).getBytes(StandardCharsets.UTF_8);
    }

    private static Parsed parse(String envelope) {
        if (envelope == null) {
            throw malformed();
        }
        int first = envelope.indexOf(SEPARATOR);
        int second = first < 0 ? -1 : envelope.indexOf(SEPARATOR, first + 1);
        if (second < 0 || !VERSION.equals(envelope.substring(0, first)) || second == first + 1 || second == envelope.length() - 1) {
            throw malformed();
        }
        try {
            return new Parsed(envelope.substring(first + 1, second), Base64.getDecoder().decode(envelope.substring(second + 1)));
        } catch (IllegalArgumentException e) {
            throw malformed();
        }
    }

    private static SecretCryptoException malformed() {
        return new SecretCryptoException("The secret value is not a " + VERSION + " envelope");
    }

    private record Parsed(String keyId, byte[] payload) {}
}
