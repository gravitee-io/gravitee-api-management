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
package io.gravitee.rest.api.security.oidc;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.CustomLog;

@CustomLog
public class OidcIdTokenCookieCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final String secret;

    public OidcIdTokenCookieCipher(String secret) {
        this.secret = secret;
    }

    public Optional<String> encrypt(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            return Optional.empty();
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(idToken.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
            if (encoded.length() > OidcCookieNames.MAX_COOKIE_VALUE_BYTES) {
                log.warn("OIDC id_token cookie exceeds size limit ({} bytes), skipping cookie", encoded.length());
                return Optional.empty();
            }
            return Optional.of(encoded);
        } catch (Exception e) {
            log.warn("Unable to encrypt OIDC id_token for cookie storage", e);
            return Optional.empty();
        }
    }

    public Optional<String> decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return Optional.empty();
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(encryptedValue);
            if (payload.length <= GCM_IV_LENGTH) {
                return Optional.empty();
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return Optional.of(new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.debug("Unable to decrypt OIDC id_token cookie", e);
            return Optional.empty();
        }
    }

    private SecretKeySpec deriveKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return new SecretKeySpec(digest.digest(secret.getBytes(StandardCharsets.UTF_8)), "AES");
    }
}
