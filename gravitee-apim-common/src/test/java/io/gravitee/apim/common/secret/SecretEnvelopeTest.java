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
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecretEnvelopeTest {

    private static final SecretScope SCOPE = new SecretScope("org-1", "env-1", "secret-1");
    private static final DataKey KEY = DataKey.generate();

    @Test
    void should_open_what_it_sealed() {
        String envelope = SecretEnvelope.seal(KEY, SCOPE, "sk-ant-123");

        assertThat(SecretEnvelope.open(KEY, SCOPE, envelope)).isEqualTo("sk-ant-123");
    }

    @Test
    void should_prefix_the_envelope_with_the_format_version_and_the_key_id() {
        String envelope = SecretEnvelope.seal(KEY, SCOPE, "sk-ant-123");

        assertThat(envelope).startsWith("v1:" + KEY.id() + ":");
    }

    @Test
    void should_not_hold_the_plaintext_in_the_envelope() {
        String envelope = SecretEnvelope.seal(KEY, SCOPE, "sk-ant-123");
        String payload = envelope.substring(envelope.lastIndexOf(':') + 1);

        assertThat(envelope).doesNotContain("sk-ant-123");
        assertThat(new String(Base64.getDecoder().decode(payload))).doesNotContain("sk-ant-123");
    }

    @Test
    void should_give_a_different_envelope_for_each_seal_of_one_value() {
        assertThat(SecretEnvelope.seal(KEY, SCOPE, "sk-ant-123")).isNotEqualTo(SecretEnvelope.seal(KEY, SCOPE, "sk-ant-123"));
    }

    @Test
    void should_refuse_to_open_the_envelope_of_another_organization() {
        String envelope = SecretEnvelope.seal(KEY, SCOPE, "sk-ant-123");

        var other = new SecretScope("org-2", "env-1", "secret-1");
        assertThatThrownBy(() -> SecretEnvelope.open(KEY, other, envelope)).isInstanceOf(SecretCryptoException.class);
    }

    @Test
    void should_refuse_to_open_the_envelope_of_another_environment() {
        String envelope = SecretEnvelope.seal(KEY, SCOPE, "sk-ant-123");

        var other = new SecretScope("org-1", "env-2", "secret-1");
        assertThatThrownBy(() -> SecretEnvelope.open(KEY, other, envelope)).isInstanceOf(SecretCryptoException.class);
    }

    @Test
    void should_refuse_to_open_an_envelope_copied_onto_another_secret() {
        String envelope = SecretEnvelope.seal(KEY, SCOPE, "sk-ant-123");

        var other = new SecretScope("org-1", "env-1", "secret-2");
        assertThatThrownBy(() -> SecretEnvelope.open(KEY, other, envelope)).isInstanceOf(SecretCryptoException.class);
    }

    @Test
    void should_refuse_to_open_under_another_data_key() {
        String envelope = SecretEnvelope.seal(DataKey.generate(), SCOPE, "sk-ant-123");

        assertThatThrownBy(() -> SecretEnvelope.open(KEY, SCOPE, envelope))
            .isInstanceOf(SecretCryptoException.class)
            .hasMessageNotContaining("sk-ant-123");
    }

    @Test
    void should_refuse_to_open_under_a_key_of_the_same_material_with_another_id() {
        String envelope = SecretEnvelope.seal(KEY, SCOPE, "sk-ant-123");

        var sameMaterial = new DataKey("another-id", KEY.material());
        assertThatThrownBy(() -> SecretEnvelope.open(sameMaterial, SCOPE, envelope)).isInstanceOf(SecretCryptoException.class);
    }

    @ParameterizedTest
    @ValueSource(
        strings = { "", "AAAA", "v1:", "v1:key", "v1:key:", "v1:key:not base64!", "v1:key:AAAA", "v2:key:AAAAAAAAAAAAAAAAAAAAAAAAAAAA" }
    )
    void should_refuse_a_malformed_envelope(String envelope) {
        assertThatThrownBy(() -> SecretEnvelope.open(new DataKey("key", KEY.material()), SCOPE, envelope)).isInstanceOf(
            SecretCryptoException.class
        );
    }
}
