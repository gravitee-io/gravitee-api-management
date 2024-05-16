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
package io.gravitee.apim.core.integration.use_case;

import static java.util.Optional.of;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import fixtures.core.model.IntegrationFixture;
import inmemory.IntegrationQueryServiceInMemory;
import io.gravitee.apim.core.integration.query_service.IntegrationQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GetIntegrationsUseCaseTest {

    private static final String ENV_ID = "my-env";
    private static final int PAGE_NUMBER = 1;
    private static final int PAGE_SIZE = 5;
    private static final Pageable pageable = new PageableImpl(PAGE_NUMBER, PAGE_SIZE);

    IntegrationQueryServiceInMemory integrationQueryServiceInMemory = new IntegrationQueryServiceInMemory();

    GetIntegrationsUseCase usecase;

    @BeforeEach
    void setUp() {
        IntegrationQueryService integrationQueryService = integrationQueryServiceInMemory;
        usecase = new GetIntegrationsUseCase(integrationQueryService);
    }

    @AfterEach
    void tearDown() {
        integrationQueryServiceInMemory.reset();
    }

    @Test
    void should_return_integrations_with_specific_env_id() {
        //Given
        var expected = IntegrationFixture.anIntegration();
        integrationQueryServiceInMemory.initWith(
            List.of(expected, IntegrationFixture.anIntegration("falseEnvID"), IntegrationFixture.anIntegration("anotherFalseEnvID"))
        );
        var input = GetIntegrationsUseCase.Input.builder().environmentId(ENV_ID).pageable(of(pageable)).build();

        //When
        var output = usecase.execute(input);

        //Then
        assertThat(output).isNotNull();
        assertThat(output.integrations())
            .extracting(Page::getContent, Page::getPageNumber, Page::getPageElements, Page::getTotalElements)
            .containsExactly(
                List.of(expected),
                PAGE_NUMBER,
                output.integrations().getPageElements(),
                (long) output.integrations().getContent().size()
            );
    }

    @Test
    void should_return_integrations_with_default_pageable() {
        //Given
        var expected = IntegrationFixture.anIntegration();
        integrationQueryServiceInMemory.initWith(List.of(expected));
        var input = new GetIntegrationsUseCase.Input(ENV_ID);

        //When
        var output = usecase.execute(input);

        //Then
        assertThat(output).isNotNull();
        assertThat(output.integrations())
            .extracting(Page::getContent, Page::getPageNumber, Page::getPageElements, Page::getTotalElements)
            .containsExactly(
                List.of(expected),
                PAGE_NUMBER,
                output.integrations().getPageElements(),
                (long) output.integrations().getContent().size()
            );
    }
}
