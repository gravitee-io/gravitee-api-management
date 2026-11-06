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

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.gamma.module.platform.core.secret.model.Secret;
import io.gravitee.gamma.module.platform.fixture.RootFixture;
import io.gravitee.gamma.module.platform.fixture.SecretFixture;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListSecretsUseCaseTest {

    private final RootFixture rootFixture = new RootFixture();
    private final SecretFixture secrets = rootFixture.secretFixture;

    @Test
    void should_page_the_secrets_of_the_environment_sorted_by_key() {
        // Given
        secrets.givenASecret(input -> input.key("mistral-key"));
        secrets.givenASecret(input -> input.key("anthropic-key"));
        secrets.givenASecret(input -> input.key("cohere-key"));
        secrets.givenASecret(input -> input.key("other-env-key").environmentId("env-2"));

        // When
        var first = secrets.listSecretsUseCase.execute(new ListSecretsUseCase.Input(ORGANIZATION_ID, ENVIRONMENT_ID, 1, 2)).secrets();
        var second = secrets.listSecretsUseCase.execute(new ListSecretsUseCase.Input(ORGANIZATION_ID, ENVIRONMENT_ID, 2, 2)).secrets();

        // Then
        assertThat(first.getContent()).extracting(Secret::key).containsExactly("anthropic-key", "cohere-key");
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(second.getContent()).extracting(Secret::key).containsExactly("mistral-key");
        assertThat(second.getPageNumber()).isEqualTo(2);
    }

    @Test
    void should_return_an_empty_page_for_another_organization() {
        // Given
        secrets.givenASecret();

        // When
        var page = secrets.listSecretsUseCase.execute(new ListSecretsUseCase.Input("org-2", ENVIRONMENT_ID, 1, 10)).secrets();

        // Then
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @ParameterizedTest
    @CsvSource({ "0, 10", "1, 0", "1, 101", "2147483647, 100" })
    void should_refuse_an_invalid_page(int pageNumber, int pageSize) {
        assertThatThrownBy(() ->
            secrets.listSecretsUseCase.execute(new ListSecretsUseCase.Input(ORGANIZATION_ID, ENVIRONMENT_ID, pageNumber, pageSize))
        ).isInstanceOf(ValidationDomainException.class);
    }
}
