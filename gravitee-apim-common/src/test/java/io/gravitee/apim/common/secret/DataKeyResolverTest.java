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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DataKeyResolverTest {

    private static final DataKey DATA_KEY = DataKey.generate();

    private final StaticKeyEncryptionKeyProvider kek = StaticKeyEncryptionKeyProvider.fromBase64(base64Key(1));
    private final Map<String, StoredDataKey> stored = new HashMap<>();
    private final DataKeyResolver resolver = new DataKeyResolver(kek, organizationId -> Optional.ofNullable(stored.get(organizationId)));

    @Test
    void should_resolve_the_stored_data_key_of_an_organization() {
        stored.put("org-1", wrapped("org-1", kek));

        var resolved = resolver.resolve("org-1").orElseThrow();

        assertThat(resolved.id()).isEqualTo(DATA_KEY.id());
        assertThat(resolved.material()).isEqualTo(DATA_KEY.material());
    }

    @Test
    void should_resolve_nothing_for_an_organization_without_a_data_key() {
        assertThat(resolver.resolve("org-1")).isEmpty();
    }

    @Test
    void should_refuse_a_data_key_stored_under_another_key_encryption_key() {
        stored.put("org-1", wrapped("org-1", StaticKeyEncryptionKeyProvider.fromBase64(base64Key(2))));

        assertThatThrownBy(() -> resolver.resolve("org-1")).isInstanceOf(KeyEncryptionKeyMismatchException.class);
    }

    @Test
    void should_refuse_a_data_key_stored_for_another_organization() {
        stored.put("org-2", wrapped("org-1", kek));

        assertThatThrownBy(() -> resolver.resolve("org-2"))
            .isInstanceOf(SecretCryptoException.class)
            .hasMessageContaining("org-2");
    }

    @Test
    void should_refuse_a_wrapped_data_key_that_is_not_base64() {
        stored.put("org-1", new StoredDataKey(DATA_KEY.id(), kek.id(), "not base64!"));

        assertThatThrownBy(() -> resolver.resolve("org-1"))
            .isInstanceOf(SecretCryptoException.class)
            .hasMessageContaining("org-1");
    }

    private static StoredDataKey wrapped(String organizationId, KeyEncryptionKeyProvider wrapper) {
        return new StoredDataKey(
            DATA_KEY.id(),
            wrapper.id(),
            Base64.getEncoder().encodeToString(wrapper.wrap(organizationId, DATA_KEY.material()))
        );
    }

    private static String base64Key(int seed) {
        var key = new byte[32];
        key[0] = (byte) seed;
        return Base64.getEncoder().encodeToString(key);
    }
}
