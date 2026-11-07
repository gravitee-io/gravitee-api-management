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
package io.gravitee.gamma.module.platform.infra.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.module.platform.core.secret.exception.SecretEncryptionUnavailableException;
import io.gravitee.gamma.module.platform.core.secret.model.Secret;
import io.gravitee.gamma.module.platform.infra.service_provider.secret.AesGcmSecretValueEncryptor;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecretConfigurationTest {

    private static final Instant AT = Instant.parse("2026-11-06T10:00:00Z");
    private static final Secret SECRET = new Secret("secret-1", "org-1", "env-1", "anthropic-key", AT, AT);

    private final SecretConfiguration configuration = new SecretConfiguration();

    @Test
    void should_encrypt_with_the_configured_key() {
        var encryptor = configuration.platformSecretValueEncryptor(environmentWithKey(Base64.getEncoder().encodeToString(new byte[32])));

        assertThat(encryptor.decrypt(SECRET, encryptor.encrypt(SECRET, "sk-ant-123"))).isEqualTo("sk-ant-123");
    }

    @Test
    void should_start_without_a_key_and_refuse_to_encrypt() {
        var encryptor = configuration.platformSecretValueEncryptor(environmentWithKey(null));

        assertThatThrownBy(() -> encryptor.encrypt(SECRET, "sk-ant-123"))
            .isInstanceOf(SecretEncryptionUnavailableException.class)
            .hasMessageContaining(AesGcmSecretValueEncryptor.KEY_PROPERTY);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "not base64!", "c2hvcnQ=" })
    void should_start_with_an_invalid_key_and_refuse_to_encrypt(String key) {
        var encryptor = configuration.platformSecretValueEncryptor(environmentWithKey(key));

        assertThatThrownBy(() -> encryptor.encrypt(SECRET, "sk-ant-123")).isInstanceOf(SecretEncryptionUnavailableException.class);
    }

    private static StandardEnvironment environmentWithKey(String key) {
        var environment = new StandardEnvironment();
        Map<String, Object> properties = new HashMap<>();
        if (key != null) {
            properties.put(AesGcmSecretValueEncryptor.KEY_PROPERTY, key);
        }
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        return environment;
    }
}
