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
import io.gravitee.gamma.module.platform.fixture.RootFixture;
import io.gravitee.gamma.module.platform.fixture.SecretFixture;
import java.time.Duration;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeleteSecretUseCaseTest {

    private final RootFixture rootFixture = new RootFixture();
    private final SecretFixture secrets = rootFixture.secretFixture;

    @Test
    void should_delete_the_secret_so_its_key_no_longer_resolves() {
        // Given
        var secret = secrets.givenASecret(input -> input.key("anthropic-key"));

        // When
        secrets.deleteSecretUseCase.execute(delete(ENVIRONMENT_ID, secret.id()));

        // Then
        assertThat(secrets.secretRepository.findByKey(ORGANIZATION_ID, ENVIRONMENT_ID, "anthropic-key")).isEmpty();
        assertThat(secrets.secretRepository.encryptedValueOf(secret.id())).isEmpty();
    }

    @Test
    void should_record_the_deletion_in_the_audit_log() {
        // Given
        var secret = secrets.givenASecret();
        secrets.clock.advance(Duration.ofMinutes(5));

        // When
        secrets.deleteSecretUseCase.execute(delete(ENVIRONMENT_ID, secret.id()));

        // Then
        assertThat(secrets.auditLog.last()).satisfies(entry -> {
            assertThat(entry.event()).isEqualTo(SecretAuditEvent.SECRET_DELETED);
            assertThat(entry.before()).isEqualTo(secret);
            assertThat(entry.after()).isNull();
            assertThat(entry.actor()).isEqualTo(ACTOR);
            assertThat(entry.at()).isEqualTo(secret.createdAt().plus(Duration.ofMinutes(5)));
        });
    }

    @Test
    void should_not_delete_a_secret_of_another_environment() {
        // Given
        var secret = secrets.givenASecret(input -> input.environmentId("env-2"));

        // When / Then
        assertThatThrownBy(() -> secrets.deleteSecretUseCase.execute(delete(ENVIRONMENT_ID, secret.id()))).isInstanceOf(
            SecretNotFoundException.class
        );
        assertThat(secrets.secretRepository.encryptedValueOf(secret.id())).isPresent();
        assertThat(secrets.auditLog.entries()).hasSize(1);
    }

    @Test
    void should_not_find_an_unknown_secret() {
        assertThatThrownBy(() -> secrets.deleteSecretUseCase.execute(delete(ENVIRONMENT_ID, "unknown"))).isInstanceOf(
            SecretNotFoundException.class
        );
    }

    private static DeleteSecretUseCase.Input delete(String environmentId, String id) {
        return new DeleteSecretUseCase.Input(ORGANIZATION_ID, environmentId, id, ACTOR);
    }
}
