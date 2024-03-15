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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import fixtures.core.model.IntegrationFixture;
import inmemory.IntegrationCrudServiceInMemory;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateIntegrationUseCaseTest {

    private static final String INTEGRATION_ID = "generated-id";
    private static final String NAME = "Test integration";
    private static final String DESCRIPTION = "Test description";
    private static final String PROVIDER = "ForTestPurpose";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ENV_ID = "my-env";

    IntegrationCrudServiceInMemory integrationCrudServiceInMemory = new IntegrationCrudServiceInMemory();

    CreateIntegrationUseCase usecase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> INTEGRATION_ID);
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @BeforeEach
    void setUp() {
        IntegrationCrudService integrationCrudService = integrationCrudServiceInMemory;
        usecase = new CreateIntegrationUseCase(integrationCrudService);
    }

    @AfterEach
    void tearDown() {
        integrationCrudServiceInMemory.reset();
    }

    @Test
    void should_create_new_integration() {
        //Given
        var integration = IntegrationFixture.anIntegration();
        var input = CreateIntegrationUseCase.Input.builder().integration(integration).build();

        //When
        CreateIntegrationUseCase.Output output = usecase.execute(input);

        //Then
        assertThat(output).isNotNull();
        assertThat(output.createdIntegration().getId()).isEqualTo(INTEGRATION_ID);
        assertThat(output.createdIntegration())
            .extracting(
                Integration::getName,
                Integration::getDescription,
                Integration::getProvider,
                Integration::getEnvironmentId,
                Integration::getCreatedAt,
                Integration::getUpdatedAt
            )
            .containsExactly(
                NAME,
                DESCRIPTION,
                PROVIDER,
                ENV_ID,
                ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()),
                ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault())
            );
    }
}
