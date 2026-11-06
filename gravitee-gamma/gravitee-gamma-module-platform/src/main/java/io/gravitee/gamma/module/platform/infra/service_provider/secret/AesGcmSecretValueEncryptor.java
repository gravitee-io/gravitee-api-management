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
package io.gravitee.gamma.module.platform.infra.service_provider.secret;

import io.gravitee.gamma.module.platform.core.secret.port.service_provider.SecretValueEncryptor;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Stand-in until per-organization keys exist: one AES-256 key from configuration serves every organization.
 * A ciphertext is base64 of the 12-byte IV followed by the GCM output, with the organization id as associated data.
 */
public class AesGcmSecretValueEncryptor implements SecretValueEncryptor {

    public static final String KEY_PROPERTY = "modules.platform.secrets.encryption.key";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public AesGcmSecretValueEncryptor(String base64Key) {
        this.key = new SecretKeySpec(decodeKey(base64Key), "AES");
    }

    @Override
    public String encrypt(String organizationId, String plaintext) {
        try {
            var iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            var cipher = cipher(Cipher.ENCRYPT_MODE, organizationId, iv);
            var encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(IV_BYTES + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt a secret value of organization " + organizationId, e);
        }
    }

    @Override
    public String decrypt(String organizationId, String ciphertext) {
        try {
            var bytes = Base64.getDecoder().decode(ciphertext);
            if (bytes.length <= IV_BYTES) {
                throw new IllegalArgumentException("ciphertext is shorter than its IV");
            }
            var cipher = cipher(Cipher.DECRYPT_MODE, organizationId, Arrays.copyOf(bytes, IV_BYTES));
            return new String(cipher.doFinal(bytes, IV_BYTES, bytes.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to decrypt a secret value of organization " + organizationId, e);
        }
    }

    private Cipher cipher(int mode, String organizationId, byte[] iv) throws GeneralSecurityException {
        var cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(organizationId.getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    private static byte[] decodeKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException(KEY_PROPERTY + " is not set. Set it to a base64-encoded 256-bit key.");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(KEY_PROPERTY + " is not valid base64. Set it to a base64-encoded 256-bit key.");
        }
        if (decoded.length != KEY_BYTES) {
            throw new IllegalArgumentException(KEY_PROPERTY + " must decode to 32 bytes, not " + decoded.length + ".");
        }
        return decoded;
    }
}
