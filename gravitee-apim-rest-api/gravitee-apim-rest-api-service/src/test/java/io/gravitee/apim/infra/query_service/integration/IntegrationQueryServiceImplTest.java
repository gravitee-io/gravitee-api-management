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
package io.gravitee.apim.infra.query_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.IntegrationFixture;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.infra.adapter.IntegrationAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IntegrationQueryServiceImplTest {

    IntegrationRepository integrationRepository;

    IntegrationQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        integrationRepository = mock(IntegrationRepository.class);
        service = new IntegrationQueryServiceImpl(integrationRepository);
    }

    @Test
    @SneakyThrows
    void should_list_integrations_matching_environment_id() {
        //Given
        var envId = "my-env";
        var pageable = new PageableImpl(1, 5);
        var expectedIntegration = IntegrationFixture.anIntegration();
        var page = integrationPage(pageable, expectedIntegration);
        when(integrationRepository.findAllByEnvironment(any(), any())).thenReturn(page);

        //When
        Page<Integration> responsePage = service.findByEnvironment(envId, pageable);

        //Then
        assertThat(responsePage).isNotNull();
        assertThat(responsePage.getPageNumber()).isEqualTo(1);
        assertThat(responsePage.getPageElements()).isEqualTo(1);
        assertThat(responsePage.getTotalElements()).isEqualTo(1);
        assertThat(responsePage.getContent().get(0)).isEqualTo(expectedIntegration);
    }

    @Test
    @SneakyThrows
    void should_throw_when_technical_exception_occurs() {
        // Given
        var envId = "different-env";
        var pageable = new PageableImpl(1, 5);
        when(integrationRepository.findAllByEnvironment(any(), any())).thenThrow(TechnicalException.class);

        // When
        Throwable throwable = catchThrowable(() -> service.findByEnvironment(envId, pageable));

        // Then
        assertThat(throwable)
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessage("An error occurred while finding Integrations by environment id: different-env");
    }

    @Test
    @SneakyThrows
    void should_list_integrations_matching_environment_id_and_groups() {
        //Given
        var envId = "my-env";
        var pageable = new PageableImpl(1, 5);
        var expectedIntegration = IntegrationFixture.anIntegration();
        var page = integrationPage(pageable, expectedIntegration);
        when(integrationRepository.findAllByEnvironmentAndGroups(any(), any(), any())).thenReturn(page);

        //When
        Page<Integration> responsePage = service.findByEnvironmentAndGroups(envId, Set.of(), pageable);

        //Then
        assertThat(responsePage).isNotNull();
        assertThat(responsePage.getPageNumber()).isEqualTo(1);
        assertThat(responsePage.getPageElements()).isEqualTo(1);
        assertThat(responsePage.getTotalElements()).isEqualTo(1);
        assertThat(responsePage.getContent().get(0)).isEqualTo(expectedIntegration);
    }

    Page<io.gravitee.repository.management.model.Integration> integrationPage(Pageable pageable, Integration integration) {
        var repositoryIntegration = IntegrationAdapter.INSTANCE.toRepository(integration);
        var integrations = List.of(repositoryIntegration);
        return new Page<>(integrations, pageable.getPageNumber(), integrations.size(), integrations.size());
    }
}
