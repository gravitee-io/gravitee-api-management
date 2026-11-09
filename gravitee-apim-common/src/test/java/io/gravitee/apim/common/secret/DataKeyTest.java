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

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DataKeyTest {

    @Test
    void should_generate_a_random_256_bit_key_with_its_own_id() {
        var first = DataKey.generate();
        var second = DataKey.generate();

        assertThat(first.material()).hasSize(32).isNotEqualTo(second.material());
        assertThat(first.id()).isNotBlank().isNotEqualTo(second.id()).doesNotContain(":");
    }

    @Test
    void should_not_let_a_caller_change_its_material() {
        var key = DataKey.generate();
        byte[] material = key.material();
        material[0] ^= 1;

        assertThat(key.material()).isNotEqualTo(material);
    }

    @Test
    void should_not_reveal_its_material_in_to_string() {
        var key = DataKey.generate();

        assertThat(key.toString()).contains(key.id()).doesNotContain(Base64.getEncoder().encodeToString(key.material()));
    }

    @Test
    void should_refuse_material_that_is_not_256_bits() {
        assertThatThrownBy(() -> new DataKey("key-1", new byte[16])).isInstanceOf(SecretCryptoException.class);
    }

    @Test
    void should_refuse_an_id_holding_the_envelope_separator() {
        assertThatThrownBy(() -> new DataKey("key:1", new byte[32])).isInstanceOf(SecretCryptoException.class);
    }
}
