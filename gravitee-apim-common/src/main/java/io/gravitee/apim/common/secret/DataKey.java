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

import java.security.SecureRandom;
import java.util.UUID;

public final class DataKey {

    static final int KEY_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String id;
    private final byte[] material;

    public DataKey(String id, byte[] material) {
        if (id == null || id.isBlank() || id.indexOf(':') >= 0) {
            throw new SecretCryptoException("A data key id must be non-blank and must not hold ':'");
        }
        if (material == null || material.length != KEY_BYTES) {
            throw new SecretCryptoException("Data key " + id + " must be " + KEY_BYTES + " bytes");
        }
        this.id = id;
        this.material = material.clone();
    }

    public static DataKey generate() {
        var material = new byte[KEY_BYTES];
        RANDOM.nextBytes(material);
        return new DataKey(UUID.randomUUID().toString(), material);
    }

    public String id() {
        return id;
    }

    public byte[] material() {
        return material.clone();
    }

    @Override
    public String toString() {
        return "DataKey[id=" + id + "]";
    }
}
