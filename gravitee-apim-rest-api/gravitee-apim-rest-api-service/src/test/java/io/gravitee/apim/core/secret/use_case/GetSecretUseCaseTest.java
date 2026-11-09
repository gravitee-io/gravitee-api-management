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
package io.gravitee.apim.core.secret.use_case;

import static fixtures.core.model.SecretFixtures.aSecret;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.AbstractUseCaseTest;
import inmemory.SecretCrudServiceInMemory;
import io.gravitee.apim.core.secret.exception.SecretNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetSecretUseCaseTest extends AbstractUseCaseTest {

    private final SecretCrudServiceInMemory secretCrudService = new SecretCrudServiceInMemory();

    private GetSecretUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetSecretUseCase(secretCrudService);
    }

    @Test
    void should_get_a_secret_of_the_environment() {
        var secret = secretCrudService.create(aSecret(), "ciphertext");

        assertThat(useCase.execute(new GetSecretUseCase.Input(secret.id(), ORG_ID, ENV_ID)).secret()).isEqualTo(secret);
    }

    @Test
    void should_not_find_a_secret_of_another_environment() {
        var secret = secretCrudService.create(aSecret().toBuilder().environmentId("env-2").build(), "ciphertext");

        assertThatThrownBy(() -> useCase.execute(new GetSecretUseCase.Input(secret.id(), ORG_ID, ENV_ID))).isInstanceOf(
            SecretNotFoundException.class
        );
    }
}
