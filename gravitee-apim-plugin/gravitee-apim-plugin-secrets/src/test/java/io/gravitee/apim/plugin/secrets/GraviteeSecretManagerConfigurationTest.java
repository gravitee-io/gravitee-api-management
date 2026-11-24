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
package io.gravitee.apim.plugin.secrets;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.secrets.api.core.Secret;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GraviteeSecretManagerConfigurationTest {

    @Test
    void should_be_enabled_by_default() {
        assertThat(new GraviteeSecretManagerConfiguration(Map.of()).isEnabled()).isTrue();
    }

    @Test
    void should_be_disabled_when_configured_so() {
        assertThat(new GraviteeSecretManagerConfiguration(Map.of("enabled", "false")).isEnabled()).isFalse();
    }

    @Test
    void should_read_the_key_encryption_key() {
        assertThat(new GraviteeSecretManagerConfiguration(Map.of("encryption.key", "a-key")).encryptionKey()).isEqualTo("a-key");
    }

    @Test
    void should_read_a_key_encryption_key_given_as_a_secret_reference() {
        var properties = Map.<String, Object>of("encryption.key", new Secret("a-key"));

        assertThat(new GraviteeSecretManagerConfiguration(properties).encryptionKey()).isEqualTo("a-key");
    }

    @Test
    void should_have_no_key_encryption_key_when_none_is_configured() {
        assertThat(new GraviteeSecretManagerConfiguration(Map.of()).encryptionKey()).isNull();
    }
}
