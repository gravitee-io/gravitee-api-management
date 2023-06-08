/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource.auth.jwt.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class PublicKeyHelper {

    private static final Pattern SSH_PUB_KEY = Pattern.compile("(ssh-(rsa|dsa) )?([A-Za-z0-9/+]+=*) ?(.*)");

    private static final byte[] PREFIX = new byte[] { 0, 0, 0, 7, 's', 's', 'h', '-', 'r', 's', 'a' };

    private static final String SSH_RSA_ALG = "ssh-rsa";

    /**
     * Generate RSA Public Key from the ssh-(rsa|dsa) ([A-Za-z0-9/+]+=*) (.*) stored key.
     * @param key String.
     * @return RSAPublicKey
     */
    public static RSAPublicKey parsePublicKey(String key) {
        Matcher m = SSH_PUB_KEY.matcher(key.trim());

        if (m.matches()) {
            String alg = m.group(1);
            String encKey = m.group(3);

            if (alg != null && !alg.startsWith(SSH_RSA_ALG)) {
                throw new IllegalArgumentException("Only RSA is currently supported, but algorithm was " + alg);
            }

            return parseSSHPublicKey(encKey);
        }

        return null;
    }

    /**
     * <pre>
     * Each rsa key should start with xxxxssh-rsa and then contains two big integer (modulus & exponent) which are prime number.
     * The modulus & exponent are used to generate the RSA Public Key.
     * <a href="https://en.wikipedia.org/wiki/RSA_(cryptosystem)">See wiki explanations for deeper understanding</a>
     * </pre>
     * @param encKey String
     * @return RSAPublicKey
     */
    private static RSAPublicKey parseSSHPublicKey(String encKey) {
        ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(StandardCharsets.UTF_8.encode(encKey)).array());

        byte[] prefix = new byte[11];

        try {
            if (in.read(prefix) != 11 || !Arrays.equals(PREFIX, prefix)) {
                throw new IllegalArgumentException("SSH key prefix not found");
            }

            BigInteger e = new BigInteger(readBigInteger(in)); //public exponent
            BigInteger n = new BigInteger(readBigInteger(in)); //modulus

            return createPublicKey(n, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static RSAPublicKey createPublicKey(BigInteger n, BigInteger e) {
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * bytes are not in the good order, they are in the big endian format, we reorder them before reading them...
     * Each time you call this method, the buffer position will move, so result are differents...
     * @param in byte array of a public encryption key without 11 "xxxxssh-rsa" first byte.
     * @return BigInteger public exponent on first call, then modulus.
     * @throws IOException
     */
    private static byte[] readBigInteger(ByteArrayInputStream in) throws IOException {
        byte[] b = new byte[4];

        if (in.read(b) != 4) {
            throw new IOException("Expected length data as 4 bytes");
        }

        int l = ByteBuffer.wrap(b).getInt();

        b = new byte[l];

        if (in.read(b) != l) {
            throw new IOException("Expected " + l + " key bytes");
        }

        return b;
    }
}
