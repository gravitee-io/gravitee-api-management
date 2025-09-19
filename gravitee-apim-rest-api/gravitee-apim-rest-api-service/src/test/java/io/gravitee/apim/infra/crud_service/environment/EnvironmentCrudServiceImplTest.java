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
package io.gravitee.apim.infra.crud_service.environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.infra.adapter.EnvironmentAdapter;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnvironmentCrudServiceImplTest {

    EnvironmentRepository environmentRepository;
    EnvironmentCrudService service;

    @BeforeEach
    void setUp() {
        environmentRepository = mock(EnvironmentRepository.class);
        service = new EnvironmentCrudServiceImpl(environmentRepository);
    }

    @Test
    void should_find_environment_by_id() {
        Environment environment = anEnvironment();
        givenEnvironment(environment);

        assertThat(service.get(environment.getId())).isEqualTo(environment);
    }

    @Test
    void should_throw_exception_if_environment_not_found() {
        var throwable = catchThrowable(() -> service.get("unknown"));
        assertThat(throwable).isInstanceOf(EnvironmentNotFoundException.class);
    }

    @SneakyThrows
    private void givenEnvironment(Environment environment) {
        when(environmentRepository.findById(environment.getId())).thenReturn(
            Optional.of(EnvironmentAdapter.INSTANCE.toRepository(environment))
        );
    }

    private Environment anEnvironment() {
        return Environment.builder().id("environment-id").build();
    }
}
