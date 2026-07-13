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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.CustomLog;

@CustomLog
public class OidcIdTokenCookieCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final byte FORMAT_GZIP = 1;
    private static final byte[] KEY_DERIVATION_DOMAIN = "gravitee-apim-oidc-id-token-cookie-v1".getBytes(StandardCharsets.UTF_8);

    private final String secret;

    public OidcIdTokenCookieCipher(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("OIDC id_token cookie encryption secret must not be blank");
        }
        this.secret = secret;
    }

    public Optional<String> encrypt(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            return Optional.empty();
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            byte[] plaintext = gzip(idToken.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            ByteBuffer buffer = ByteBuffer.allocate(1 + iv.length + ciphertext.length);
            buffer.put(FORMAT_GZIP);
            buffer.put(iv);
            buffer.put(ciphertext);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
            if (encoded.length() > OidcCookieNames.MAX_COOKIE_VALUE_BYTES) {
                log.error(
                    "OIDC id_token cookie exceeds size limit ({} > {} bytes). " +
                        "IdP logout will proceed without id_token_hint for this session.",
                    encoded.length(),
                    OidcCookieNames.MAX_COOKIE_VALUE_BYTES
                );
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

            byte format = payload[0];
            ByteBuffer buffer;
            if (format == FORMAT_GZIP) {
                if (payload.length <= 1 + GCM_IV_LENGTH) {
                    return Optional.empty();
                }
                buffer = ByteBuffer.wrap(payload, 1, payload.length - 1);
            } else {
                // Legacy payloads stored IV as the first bytes without a format marker.
                buffer = ByteBuffer.wrap(payload);
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decrypted = cipher.doFinal(ciphertext);

            if (format == FORMAT_GZIP) {
                return Optional.of(new String(gunzip(decrypted), StandardCharsets.UTF_8));
            }
            return Optional.of(new String(decrypted, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.debug("Unable to decrypt OIDC id_token cookie", e);
            return Optional.empty();
        }
    }

    private static byte[] gzip(byte[] input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(output)) {
            gzipOutputStream.write(input);
        }
        return output.toByteArray();
    }

    private static byte[] gunzip(byte[] input) throws Exception {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input);
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {
            return gzipInputStream.readAllBytes();
        }
    }

    private SecretKeySpec deriveKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(secret.getBytes(StandardCharsets.UTF_8));
        digest.update(KEY_DERIVATION_DOMAIN);
        return new SecretKeySpec(digest.digest(), "AES");
    }
}
