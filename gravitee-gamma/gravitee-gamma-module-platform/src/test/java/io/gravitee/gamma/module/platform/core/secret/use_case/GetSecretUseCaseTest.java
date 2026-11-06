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

import static io.gravitee.gamma.module.platform.fixture.SecretFixture.ENVIRONMENT_ID;
import static io.gravitee.gamma.module.platform.fixture.SecretFixture.ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.module.platform.core.secret.exception.SecretNotFoundException;
import io.gravitee.gamma.module.platform.fixture.RootFixture;
import io.gravitee.gamma.module.platform.fixture.SecretFixture;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetSecretUseCaseTest {

    private final RootFixture rootFixture = new RootFixture();
    private final SecretFixture secrets = rootFixture.secretFixture;

    @Test
    void should_return_the_secret() {
        // Given
        var secret = secrets.givenASecret();

        // When
        var output = secrets.getSecretUseCase.execute(new GetSecretUseCase.Input(ORGANIZATION_ID, ENVIRONMENT_ID, secret.id()));

        // Then
        assertThat(output.secret()).isEqualTo(secret);
    }

    @Test
    void should_not_find_a_secret_of_another_environment() {
        // Given
        var secret = secrets.givenASecret(input -> input.environmentId("env-2"));

        // When / Then
        assertThatThrownBy(() ->
            secrets.getSecretUseCase.execute(new GetSecretUseCase.Input(ORGANIZATION_ID, ENVIRONMENT_ID, secret.id()))
        ).isInstanceOf(SecretNotFoundException.class);
    }
}
