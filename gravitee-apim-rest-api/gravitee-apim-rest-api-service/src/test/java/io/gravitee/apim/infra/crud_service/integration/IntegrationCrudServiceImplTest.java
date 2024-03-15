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
package io.gravitee.apim.infra.crud_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.IntegrationFixture;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IntegrationCrudServiceImplTest {

    IntegrationRepository integrationRepository;

    IntegrationCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        integrationRepository = mock(IntegrationRepository.class);
        service = new IntegrationCrudServiceImpl(integrationRepository);
    }

    @Test
    @SneakyThrows
    void should_create_integration() {
        //Given
        Integration integration = IntegrationFixture.anIntegration();
        when(integrationRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        //When
        Integration createdIntegration = service.create(integration);

        //Then
        assertThat(createdIntegration).isEqualTo(integration);
    }

    @Test
    void should_throw_when_technical_exception_occurs() throws TechnicalException {
        // Given
        var integration = IntegrationFixture.anIntegration();
        when(integrationRepository.create(any())).thenThrow(TechnicalException.class);

        // When
        Throwable throwable = catchThrowable(() -> service.create(integration));

        // Then
        assertThat(throwable)
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessage("Error when creating Integration: Test integration");
    }
}
