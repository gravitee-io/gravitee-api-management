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

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

final class AesGcm {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private AesGcm() {}

    static SecretKey key(byte[] material) {
        return new SecretKeySpec(material, "AES");
    }

    /** Returns the IV followed by the ciphertext and its tag. */
    static byte[] encrypt(SecretKey key, byte[] aad, byte[] plaintext) throws GeneralSecurityException {
        var iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        var cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(aad);
        var encrypted = cipher.doFinal(plaintext);
        return ByteBuffer.allocate(IV_BYTES + encrypted.length).put(iv).put(encrypted).array();
    }

    static byte[] decrypt(SecretKey key, byte[] aad, byte[] ivAndCiphertext) throws GeneralSecurityException {
        if (ivAndCiphertext.length <= IV_BYTES) {
            throw new GeneralSecurityException("ciphertext is shorter than its IV");
        }
        var cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, Arrays.copyOf(ivAndCiphertext, IV_BYTES)));
        cipher.updateAAD(aad);
        return cipher.doFinal(ivAndCiphertext, IV_BYTES, ivAndCiphertext.length - IV_BYTES);
    }
}
