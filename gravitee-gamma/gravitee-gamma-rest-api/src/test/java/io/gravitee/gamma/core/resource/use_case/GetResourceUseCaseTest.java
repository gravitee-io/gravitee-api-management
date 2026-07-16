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
package io.gravitee.gamma.core.resource.use_case;

import static io.gravitee.gamma.core.resource.fixture.ResourceFixture.anAuditInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gamma.inmemory.ResourceCrudServiceInMemory;
import io.gravitee.gamma.core.domain.resource.exception.ResourceNotFoundException;
import io.gravitee.gamma.core.domain.resource.use_case.GetResourceUseCase;
import io.gravitee.gamma.core.resource.fixture.ResourceFixture;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetResourceUseCaseTest {

    private ResourceCrudServiceInMemory repository;
    private GetResourceUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new ResourceCrudServiceInMemory();
        useCase = new GetResourceUseCase(repository);
    }

    @Test
    void should_return_resource_when_it_belongs_to_environment() {
        var existing = ResourceFixture.aResource();
        repository.initWith(List.of(existing));

        var output = useCase.execute(new GetResourceUseCase.Input(anAuditInfo(), existing.id()));

        assertThat(output.resource()).isEqualTo(existing);
    }

    @Test
    void should_throw_when_resource_not_found() {
        assertThatThrownBy(() -> useCase.execute(new GetResourceUseCase.Input(anAuditInfo(), "unknown"))).isInstanceOf(
            ResourceNotFoundException.class
        );
    }

    @Test
    void should_throw_when_resource_belongs_to_a_different_environment() {
        repository.initWith(List.of(ResourceFixture.aResource(r -> r.referenceId("OTHER"))));

        assertThatThrownBy(() -> useCase.execute(new GetResourceUseCase.Input(anAuditInfo(), ResourceFixture.DEFAULT_ID))).isInstanceOf(
            ResourceNotFoundException.class
        );
    }
}
