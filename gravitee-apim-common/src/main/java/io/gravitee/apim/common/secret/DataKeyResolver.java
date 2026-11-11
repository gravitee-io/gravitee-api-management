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

import java.util.Base64;
import java.util.Optional;

/**
 * Reads and unwraps the data key of an organization, so that every component that decrypts secret values agrees on
 * which stored data keys it can use.
 */
public final class DataKeyResolver {

    private final KeyEncryptionKeyProvider kek;
    private final DataKeyStore store;

    public DataKeyResolver(KeyEncryptionKeyProvider kek, DataKeyStore store) {
        this.kek = kek;
        this.store = store;
    }

    /**
     * @throws KeyEncryptionKeyMismatchException when the data key was wrapped by a key encryption key the provider does not hold
     * @throws SecretCryptoException when the stored data key cannot be unwrapped for that organization
     */
    public Optional<DataKey> resolve(String organizationId) {
        return store.findByOrganizationId(organizationId).map(stored -> unwrap(organizationId, stored));
    }

    private DataKey unwrap(String organizationId, StoredDataKey stored) {
        byte[] wrapped;
        try {
            wrapped = Base64.getDecoder().decode(stored.wrappedKey());
        } catch (IllegalArgumentException e) {
            throw new SecretCryptoException("The data key of organization " + organizationId + " is not valid base64", e);
        }
        return new DataKey(stored.id(), kek.unwrap(organizationId, stored.kekId(), wrapped));
    }
}
