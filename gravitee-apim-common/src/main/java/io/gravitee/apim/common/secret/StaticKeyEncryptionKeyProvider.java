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
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * The on-prem key encryption key: a base64 256-bit AES key read from configuration.
 */
public final class StaticKeyEncryptionKeyProvider implements KeyEncryptionKeyProvider {

    private static final byte[] ID_LABEL = "gravitee-secrets-kek-id".getBytes(StandardCharsets.UTF_8);
    private static final String WRAP_AAD_PREFIX = "gravitee-secrets-dek\0";
    private static final int ID_BYTES = 8;

    private final SecretKey key;
    private final String id;

    private StaticKeyEncryptionKeyProvider(byte[] material) {
        this.key = AesGcm.key(material);
        this.id = fingerprint(material);
    }

    public static StaticKeyEncryptionKeyProvider fromBase64(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new InvalidKeyEncryptionKeyException("is not set");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidKeyEncryptionKeyException("is not valid base64");
        }
        if (decoded.length != DataKey.KEY_BYTES) {
            throw new InvalidKeyEncryptionKeyException("must decode to " + DataKey.KEY_BYTES + " bytes, not " + decoded.length);
        }
        return new StaticKeyEncryptionKeyProvider(decoded);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean canUnwrap(String kekId) {
        return id.equals(kekId);
    }

    @Override
    public byte[] wrap(String organizationId, byte[] dataKey) {
        try {
            return AesGcm.encrypt(key, wrapAad(organizationId), dataKey);
        } catch (GeneralSecurityException e) {
            throw new SecretCryptoException("Failed to wrap the data key of organization " + organizationId, e);
        }
    }

    @Override
    public byte[] unwrap(String organizationId, String kekId, byte[] wrappedDataKey) {
        if (!canUnwrap(kekId)) {
            throw new KeyEncryptionKeyMismatchException(
                "The data key of organization " + organizationId + " was wrapped by another key encryption key"
            );
        }
        try {
            return AesGcm.decrypt(key, wrapAad(organizationId), wrappedDataKey);
        } catch (GeneralSecurityException e) {
            throw new SecretCryptoException("Failed to unwrap the data key of organization " + organizationId, e);
        }
    }

    // The organization id is bound to the wrapped key, so a wrapped key copied onto another organization fails to unwrap.
    private static byte[] wrapAad(String organizationId) {
        return (WRAP_AAD_PREFIX + organizationId).getBytes(StandardCharsets.UTF_8);
    }

    private static String fingerprint(byte[] material) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(material, "HmacSHA256"));
            var digest = mac.doFinal(ID_LABEL);
            return HexFormat.of().formatHex(digest, 0, ID_BYTES);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
