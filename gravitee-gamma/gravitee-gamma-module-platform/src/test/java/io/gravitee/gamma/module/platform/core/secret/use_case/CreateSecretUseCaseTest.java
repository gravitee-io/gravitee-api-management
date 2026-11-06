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
package io.gravitee.gamma.module.platform.core.secret.use_case;

import static io.gravitee.gamma.module.platform.fixture.SecretFixture.ACTOR;
import static io.gravitee.gamma.module.platform.fixture.SecretFixture.ENVIRONMENT_ID;
import static io.gravitee.gamma.module.platform.fixture.SecretFixture.ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.module.platform.core.secret.exception.InvalidSecretKeyException;
import io.gravitee.gamma.module.platform.core.secret.exception.InvalidSecretValueException;
import io.gravitee.gamma.module.platform.core.secret.exception.SecretKeyAlreadyExistsException;
import io.gravitee.gamma.module.platform.core.secret.model.SecretAuditEvent;
import io.gravitee.gamma.module.platform.core.secret.model.SecretValue;
import io.gravitee.gamma.module.platform.fixture.RootFixture;
import io.gravitee.gamma.module.platform.fixture.SecretFixture;
import java.time.Instant;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateSecretUseCaseTest {

    private final RootFixture rootFixture = new RootFixture();
    private final SecretFixture secrets = rootFixture.secretFixture;

    @Test
    void should_create_a_secret_with_its_path() {
        // When
        var output = secrets.createSecretUseCase.execute(secrets.aCreateInput().id("secret-1").key("anthropic-key").build());

        // Then
        assertThat(output.secret()).satisfies(secret -> {
            assertThat(secret.id()).isEqualTo("secret-1");
            assertThat(secret.organizationId()).isEqualTo(ORGANIZATION_ID);
            assertThat(secret.environmentId()).isEqualTo(ENVIRONMENT_ID);
            assertThat(secret.key()).isEqualTo("anthropic-key");
            assertThat(secret.path()).isEqualTo("/gravitee/anthropic-key");
            assertThat(secret.createdAt()).isEqualTo(Instant.parse("2026-11-06T10:00:00Z"));
            assertThat(secret.updatedAt()).isEqualTo(secret.createdAt());
        });
    }

    @Test
    void should_store_only_the_encrypted_value() {
        // When
        var secret = secrets.givenASecret(input -> input.value(new SecretValue("sk-ant-123")));

        // Then
        var stored = secrets.secretRepository.encryptedValueOf(secret.id()).orElseThrow();
        assertThat(stored).isNotEqualTo("sk-ant-123").doesNotContain("sk-ant-123");
        assertThat(secrets.encryptor.decrypt(secret, stored)).isEqualTo("sk-ant-123");
    }

    @Test
    void should_record_the_creation_in_the_audit_log() {
        // When
        var secret = secrets.givenASecret();

        // Then
        assertThat(secrets.auditLog.entries())
            .singleElement()
            .satisfies(entry -> {
                assertThat(entry.event()).isEqualTo(SecretAuditEvent.SECRET_CREATED);
                assertThat(entry.before()).isNull();
                assertThat(entry.after()).isEqualTo(secret);
                assertThat(entry.actor()).isEqualTo(ACTOR);
                assertThat(entry.at()).isEqualTo(secret.createdAt());
            });
    }

    @Test
    void should_refuse_a_key_that_the_environment_already_holds() {
        // Given
        secrets.givenASecret(input -> input.key("anthropic-key"));

        // When / Then
        assertThatThrownBy(() -> secrets.givenASecret(input -> input.key("anthropic-key"))).isInstanceOf(
            SecretKeyAlreadyExistsException.class
        );
        assertThat(secrets.auditLog.entries()).hasSize(1);
    }

    @Test
    void should_accept_a_key_that_only_another_environment_holds() {
        // Given
        secrets.givenASecret(input -> input.key("anthropic-key").environmentId("env-2"));

        // When
        var secret = secrets.givenASecret(input -> input.key("anthropic-key"));

        // Then
        assertThat(secret.environmentId()).isEqualTo(ENVIRONMENT_ID);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "with space", "slash/key", "{#el}", "é", ".", "..", ".env", "-key", "_key" })
    void should_refuse_an_invalid_key(String key) {
        assertThatThrownBy(() -> secrets.givenASecret(input -> input.key(key))).isInstanceOf(InvalidSecretKeyException.class);
        assertThat(secrets.auditLog.entries()).isEmpty();
    }

    @Test
    void should_accept_a_key_of_128_characters_and_refuse_129() {
        assertThat(secrets.givenASecret(input -> input.key("k".repeat(128))).key()).hasSize(128);
        assertThatThrownBy(() -> secrets.givenASecret(input -> input.key("k".repeat(129)))).isInstanceOf(InvalidSecretKeyException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void should_refuse_an_empty_value(String value) {
        assertThatThrownBy(() -> new SecretValue(value)).isInstanceOf(InvalidSecretValueException.class);
    }

    @Test
    void should_accept_a_value_of_64_KiB_and_refuse_one_byte_more() {
        assertThat(new SecretValue("v".repeat(65536)).plaintext()).hasSize(65536);
        assertThatThrownBy(() -> new SecretValue("v".repeat(65537))).isInstanceOf(InvalidSecretValueException.class);
        assertThatThrownBy(() -> new SecretValue("é".repeat(32769))).isInstanceOf(InvalidSecretValueException.class);
    }

    @Test
    void should_hide_the_value_in_the_input_description() {
        var input = secrets.aCreateInput().value(new SecretValue("sk-ant-123")).build();

        assertThat(input.toString()).doesNotContain("sk-ant-123");
    }
}
