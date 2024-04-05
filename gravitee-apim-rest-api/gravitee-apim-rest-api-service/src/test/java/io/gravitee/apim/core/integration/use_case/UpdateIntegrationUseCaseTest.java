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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import fixtures.core.model.IntegrationFixture;
import inmemory.IntegrationCrudServiceInMemory;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.*;

public class UpdateIntegrationUseCaseTest {

    private static final String INTEGRATION_ID = "integration-id";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String PROVIDER = "test-provider";
    private static final String ENV_ID = "my-env";
    private static final ZonedDateTime CREATED_DATE = ZonedDateTime
        .parse("2020-02-03T20:22:02.00Z")
        .withZoneSameLocal(ZoneId.systemDefault());
    private static final Integration.AgentStatus AGENT_STATUS = Integration.AgentStatus.DISCONNECTED;

    IntegrationCrudServiceInMemory integrationCrudServiceInMemory = new IntegrationCrudServiceInMemory();

    UpdateIntegrationUseCase usecase;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @BeforeEach
    void setUp() {
        IntegrationCrudService integrationCrudService = integrationCrudServiceInMemory;
        integrationCrudServiceInMemory.initWith(List.of(IntegrationFixture.anIntegration()));

        usecase = new UpdateIntegrationUseCase(integrationCrudService);
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @AfterEach
    void tearDownEach() {
        integrationCrudServiceInMemory.reset();
    }

    @Test
    void should_update_integration() {
        //Given
        Integration updateIntegration = Integration
            .builder()
            .id(INTEGRATION_ID)
            .name("updated-integration")
            .description("updated-description")
            .build();

        var input = UpdateIntegrationUseCase.Input.builder().integration(updateIntegration).build();

        //When
        var output = usecase.execute(input);

        //Then
        assertThat(output.integration()).isNotNull();
        assertThat(output.integration())
            .isNotNull()
            .isEqualTo(
                new Integration()
                    .toBuilder()
                    .id(INTEGRATION_ID)
                    .name("updated-integration")
                    .description("updated-description")
                    .environmentId(ENV_ID)
                    .provider(PROVIDER)
                    .createdAt(CREATED_DATE)
                    .agentStatus(AGENT_STATUS)
                    .updatedAt(ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    public void should_throw_exception_when_integration_to_update_not_found() {
        var updateIntegration = Integration
            .builder()
            .id("not-existing-integration")
            .name("updated-integration")
            .description("updated-description")
            .build();

        var input = UpdateIntegrationUseCase.Input.builder().integration(updateIntegration).build();

        assertThatExceptionOfType(IntegrationNotFoundException.class)
            .isThrownBy(() -> usecase.execute(input))
            .withMessage("Integration not found.");
    }
}
