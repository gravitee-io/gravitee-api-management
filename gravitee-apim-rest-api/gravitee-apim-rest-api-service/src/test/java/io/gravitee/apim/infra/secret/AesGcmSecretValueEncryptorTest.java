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
package io.gravitee.apim.infra.secret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.secret.exception.SecretEncryptionUnavailableException;
import io.gravitee.apim.core.secret.model.Secret;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AesGcmSecretValueEncryptorTest {

    private static final String TEST_KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final Instant AT = Instant.parse("2026-11-06T10:00:00Z");
    private static final Secret SECRET = new Secret("secret-1", "org-1", "env-1", "anthropic-key", AT, AT);

    private final AesGcmSecretValueEncryptor encryptor = new AesGcmSecretValueEncryptor(TEST_KEY);

    @Test
    void should_decrypt_what_it_encrypted() {
        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");

        assertThat(encryptor.decrypt(SECRET, ciphertext)).isEqualTo("sk-ant-123");
    }

    @Test
    void should_not_hold_the_plaintext_in_the_ciphertext() {
        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");

        assertThat(ciphertext).doesNotContain("sk-ant-123");
        assertThat(new String(Base64.getDecoder().decode(ciphertext))).doesNotContain("sk-ant-123");
    }

    @Test
    void should_give_a_different_ciphertext_for_each_encryption_of_one_value() {
        assertThat(encryptor.encrypt(SECRET, "sk-ant-123")).isNotEqualTo(encryptor.encrypt(SECRET, "sk-ant-123"));
    }

    @Test
    void should_refuse_to_decrypt_the_ciphertext_of_another_organization() {
        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");

        var other = SECRET.toBuilder().organizationId("org-2").build();
        assertThatThrownBy(() -> encryptor.decrypt(other, ciphertext)).isInstanceOf(TechnicalDomainException.class);
    }

    @Test
    void should_refuse_to_decrypt_the_ciphertext_of_another_environment() {
        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");

        var other = SECRET.toBuilder().environmentId("env-2").build();
        assertThatThrownBy(() -> encryptor.decrypt(other, ciphertext)).isInstanceOf(TechnicalDomainException.class);
    }

    @Test
    void should_refuse_to_decrypt_the_ciphertext_of_another_secret_of_the_environment() {
        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");

        var other = SECRET.toBuilder().id("secret-2").key("prod-db-password").build();
        assertThatThrownBy(() -> encryptor.decrypt(other, ciphertext)).isInstanceOf(TechnicalDomainException.class);
    }

    @Test
    void should_refuse_to_decrypt_under_another_key() {
        var otherKey = new byte[32];
        otherKey[0] = 1;
        String ciphertext = new AesGcmSecretValueEncryptor(Base64.getEncoder().encodeToString(otherKey)).encrypt(SECRET, "sk-ant-123");

        assertThatThrownBy(() -> encryptor.decrypt(SECRET, ciphertext)).isInstanceOf(TechnicalDomainException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "AAAA", "not base64!" })
    void should_refuse_to_decrypt_a_malformed_ciphertext(String ciphertext) {
        assertThatThrownBy(() -> encryptor.decrypt(SECRET, ciphertext)).isInstanceOf(TechnicalDomainException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "not base64!", "c2hvcnQ=" })
    void should_start_with_a_missing_or_invalid_key_and_refuse_to_encrypt(String key) {
        var unavailable = new AesGcmSecretValueEncryptor(key);

        assertThatThrownBy(() -> unavailable.encrypt(SECRET, "sk-ant-123"))
            .isInstanceOf(SecretEncryptionUnavailableException.class)
            .hasMessageContaining(AesGcmSecretValueEncryptor.KEY_PROPERTY);
        assertThatThrownBy(() -> unavailable.decrypt(SECRET, "AAAAAAAAAAAAAAAAAAAAAAAA")).isInstanceOf(
            SecretEncryptionUnavailableException.class
        );
    }
}
