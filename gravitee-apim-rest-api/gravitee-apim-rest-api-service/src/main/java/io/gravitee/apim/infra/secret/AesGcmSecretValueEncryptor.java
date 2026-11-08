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
package io.gravitee.apim.infra.secret;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.secret.exception.SecretEncryptionUnavailableException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.service_provider.SecretValueEncryptor;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Stand-in until per-organization keys exist: one AES-256 key from configuration serves every organization.
 * A ciphertext is base64 of the 12-byte IV followed by the GCM output. The associated data is the organization id,
 * the environment id and the secret id, so a ciphertext copied onto another secret fails to decrypt.
 * Without a valid key the Management API still starts, and only encrypt and decrypt fail.
 */
@CustomLog
@Service
public class AesGcmSecretValueEncryptor implements SecretValueEncryptor {

    public static final String KEY_PROPERTY = "api.secrets.encryption.key";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmSecretValueEncryptor(@Value("${" + KEY_PROPERTY + ":#{null}}") String base64Key) {
        this.key = toKey(base64Key);
    }

    @Override
    public String encrypt(Secret secret, String plaintext) {
        try {
            var iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            var cipher = cipher(Cipher.ENCRYPT_MODE, secret, iv);
            var encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(IV_BYTES + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new TechnicalDomainException("Failed to encrypt the value of secret " + secret.id(), e);
        }
    }

    @Override
    public String decrypt(Secret secret, String ciphertext) {
        try {
            var bytes = Base64.getDecoder().decode(ciphertext);
            if (bytes.length <= IV_BYTES) {
                throw new IllegalArgumentException("ciphertext is shorter than its IV");
            }
            var cipher = cipher(Cipher.DECRYPT_MODE, secret, Arrays.copyOf(bytes, IV_BYTES));
            return new String(cipher.doFinal(bytes, IV_BYTES, bytes.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new TechnicalDomainException("Failed to decrypt the value of secret " + secret.id(), e);
        }
    }

    private Cipher cipher(int mode, Secret secret, byte[] iv) throws GeneralSecurityException {
        if (key == null) {
            throw new SecretEncryptionUnavailableException(KEY_PROPERTY);
        }
        var cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
        // Ids never hold NUL, so two different (organization, environment, secret) triples never give the same bytes.
        cipher.updateAAD(String.join("\0", secret.organizationId(), secret.environmentId(), secret.id()).getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    private static SecretKeySpec toKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            log.warn("{} is not set: secrets cannot be created or rotated. Set it to a base64-encoded 256-bit key.", KEY_PROPERTY);
            return null;
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            log.warn("{} is not valid base64: secrets cannot be created or rotated. Set it to a base64-encoded 256-bit key.", KEY_PROPERTY);
            return null;
        }
        if (decoded.length != KEY_BYTES) {
            log.warn("{} must decode to 32 bytes, not {}: secrets cannot be created or rotated.", KEY_PROPERTY, decoded.length);
            return null;
        }
        return new SecretKeySpec(decoded, "AES");
    }
}
