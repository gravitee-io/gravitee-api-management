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

import io.gravitee.gamma.module.platform.core.secret.exception.SecretNotFoundException;
import io.gravitee.gamma.module.platform.core.secret.model.SecretAuditEvent;
import io.gravitee.gamma.module.platform.core.secret.model.SecretValue;
import io.gravitee.gamma.module.platform.fixture.RootFixture;
import io.gravitee.gamma.module.platform.fixture.SecretFixture;
import java.time.Duration;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateSecretUseCaseTest {

    private final RootFixture rootFixture = new RootFixture();
    private final SecretFixture secrets = rootFixture.secretFixture;

    @Test
    void should_rotate_the_value_and_keep_the_key() {
        // Given
        var secret = secrets.givenASecret(input -> input.value(new SecretValue("old-value")));
        secrets.clock.advance(Duration.ofMinutes(5));

        // When
        var rotated = secrets.updateSecretUseCase.execute(rotate(secret.environmentId(), secret.id(), "new-value")).secret();

        // Then
        var stored = secrets.secretRepository.encryptedValueOf(secret.id()).orElseThrow();
        assertThat(secrets.encryptor.decrypt(secret, stored)).isEqualTo("new-value");
        assertThat(rotated.key()).isEqualTo(secret.key());
        assertThat(rotated.path()).isEqualTo(secret.path());
        assertThat(rotated.createdAt()).isEqualTo(secret.createdAt());
        assertThat(rotated.updatedAt()).isEqualTo(secret.createdAt().plus(Duration.ofMinutes(5)));
    }

    @Test
    void should_record_the_rotation_in_the_audit_log() {
        // Given
        var secret = secrets.givenASecret(input -> input.value(new SecretValue("old-value")));
        secrets.clock.advance(Duration.ofMinutes(5));

        // When
        var rotated = secrets.updateSecretUseCase.execute(rotate(secret.environmentId(), secret.id(), "new-value")).secret();

        // Then
        assertThat(secrets.auditLog.last()).satisfies(entry -> {
            assertThat(entry.event()).isEqualTo(SecretAuditEvent.SECRET_UPDATED);
            assertThat(entry.before()).isEqualTo(secret);
            assertThat(entry.after()).isEqualTo(rotated);
            assertThat(entry.actor()).isEqualTo(ACTOR);
            assertThat(entry.at()).isEqualTo(rotated.updatedAt());
        });
    }

    @Test
    void should_not_find_a_secret_of_another_environment() {
        // Given
        var secret = secrets.givenASecret(input -> input.environmentId("env-2"));

        // When / Then
        assertThatThrownBy(() -> secrets.updateSecretUseCase.execute(rotate(ENVIRONMENT_ID, secret.id(), "new-value"))).isInstanceOf(
            SecretNotFoundException.class
        );
        assertThat(secrets.auditLog.entries()).hasSize(1);
    }

    @Test
    void should_not_find_a_secret_of_another_organization() {
        // Given
        var secret = secrets.givenASecret(input -> input.organizationId("org-2"));

        // When / Then
        assertThatThrownBy(() -> secrets.updateSecretUseCase.execute(rotate(ENVIRONMENT_ID, secret.id(), "new-value"))).isInstanceOf(
            SecretNotFoundException.class
        );
    }

    @Test
    void should_not_find_an_unknown_secret() {
        assertThatThrownBy(() -> secrets.updateSecretUseCase.execute(rotate(ENVIRONMENT_ID, "unknown", "new-value"))).isInstanceOf(
            SecretNotFoundException.class
        );
    }

    private static UpdateSecretUseCase.Input rotate(String environmentId, String id, String value) {
        return new UpdateSecretUseCase.Input(ORGANIZATION_ID, environmentId, id, new SecretValue(value), ACTOR);
    }
}
