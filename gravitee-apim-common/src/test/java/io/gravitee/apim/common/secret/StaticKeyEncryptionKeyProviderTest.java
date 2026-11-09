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
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StaticKeyEncryptionKeyProviderTest {

    private static final String KEK = base64Key(1);
    private static final DataKey DATA_KEY = DataKey.generate();

    private final StaticKeyEncryptionKeyProvider provider = StaticKeyEncryptionKeyProvider.fromBase64(KEK);

    @Test
    void should_unwrap_what_it_wrapped() {
        byte[] wrapped = provider.wrap("org-1", DATA_KEY.material());

        assertThat(provider.unwrap("org-1", wrapped)).isEqualTo(DATA_KEY.material());
    }

    @Test
    void should_not_hold_the_data_key_in_the_wrapped_key() {
        byte[] wrapped = provider.wrap("org-1", DATA_KEY.material());

        assertThat(Base64.getEncoder().encodeToString(wrapped)).doesNotContain(Base64.getEncoder().encodeToString(DATA_KEY.material()));
        assertThat(wrapped).isNotEqualTo(DATA_KEY.material());
    }

    @Test
    void should_refuse_to_unwrap_the_data_key_of_another_organization() {
        byte[] wrapped = provider.wrap("org-1", DATA_KEY.material());

        assertThatThrownBy(() -> provider.unwrap("org-2", wrapped)).isInstanceOf(SecretCryptoException.class);
    }

    @Test
    void should_refuse_to_unwrap_under_another_key_encryption_key() {
        byte[] wrapped = StaticKeyEncryptionKeyProvider.fromBase64(base64Key(2)).wrap("org-1", DATA_KEY.material());

        assertThatThrownBy(() -> provider.unwrap("org-1", wrapped)).isInstanceOf(SecretCryptoException.class);
    }

    @Test
    void should_refuse_to_unwrap_a_truncated_wrapped_key() {
        assertThatThrownBy(() -> provider.unwrap("org-1", new byte[8])).isInstanceOf(SecretCryptoException.class);
    }

    @Test
    void should_give_the_same_id_for_the_same_key() {
        assertThat(StaticKeyEncryptionKeyProvider.fromBase64(KEK).id()).isEqualTo(provider.id());
    }

    @Test
    void should_give_another_id_for_another_key() {
        assertThat(StaticKeyEncryptionKeyProvider.fromBase64(base64Key(2)).id()).isNotEqualTo(provider.id());
    }

    @Test
    void should_not_reveal_the_key_in_its_id() {
        assertThat(provider.id()).doesNotContain(KEK).hasSize(16);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " " })
    void should_refuse_a_missing_key(String key) {
        assertThatThrownBy(() -> StaticKeyEncryptionKeyProvider.fromBase64(key))
            .isInstanceOf(InvalidKeyEncryptionKeyException.class)
            .hasMessage("is not set");
    }

    @Test
    void should_refuse_a_key_that_is_not_base64_without_echoing_it() {
        assertThatThrownBy(() -> StaticKeyEncryptionKeyProvider.fromBase64("not base64!"))
            .isInstanceOf(InvalidKeyEncryptionKeyException.class)
            .hasMessage("is not valid base64")
            .hasNoCause();
    }

    @Test
    void should_refuse_a_key_of_the_wrong_length_without_echoing_it() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> StaticKeyEncryptionKeyProvider.fromBase64(shortKey))
            .isInstanceOf(InvalidKeyEncryptionKeyException.class)
            .hasMessage("must decode to 32 bytes, not 16");
    }

    @Test
    void should_accept_a_key_with_surrounding_whitespace() {
        assertThat(StaticKeyEncryptionKeyProvider.fromBase64(" " + KEK + "\n").id()).isEqualTo(provider.id());
    }

    private static String base64Key(int seed) {
        var key = new byte[32];
        key[0] = (byte) seed;
        return Base64.getEncoder().encodeToString(key);
    }
}
