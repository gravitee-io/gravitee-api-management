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
package io.gravitee.gamma.rest.core.guardrail.use_case;

import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.GUARDRAIL_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.rest.core.guardrail.GuardrailDeploymentFixtures;
import io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetGuardrailUseCaseTest {

    private final InMemoryGuardrailRepository guardrailRepository = new InMemoryGuardrailRepository();

    private GetGuardrailUseCase useCase;

    @BeforeEach
    void setUp() {
        guardrailRepository.reset();
        guardrailRepository.givenGuardrail(aGuardrail().build());
        useCase = new GetGuardrailUseCase(guardrailRepository, GuardrailDeploymentFixtures.deploymentService(guardrailRepository));
    }

    @Test
    void should_return_the_guardrail_of_the_environment() {
        var output = useCase.execute(GetGuardrailUseCase.Input.builder().environmentId(ENVIRONMENT_ID).guardrailId(GUARDRAIL_ID).build());

        assertThat(output.guardrail().getId()).isEqualTo(GUARDRAIL_ID);
    }

    @Test
    void should_not_find_an_unknown_guardrail() {
        assertThatThrownBy(() ->
            useCase.execute(GetGuardrailUseCase.Input.builder().environmentId(ENVIRONMENT_ID).guardrailId("nope").build())
        ).isInstanceOf(GuardrailNotFoundException.class);
    }

    @Test
    void should_not_find_a_guardrail_of_another_environment() {
        assertThatThrownBy(() ->
            useCase.execute(GetGuardrailUseCase.Input.builder().environmentId("other-env").guardrailId(GUARDRAIL_ID).build())
        ).isInstanceOf(GuardrailNotFoundException.class);
    }
}
