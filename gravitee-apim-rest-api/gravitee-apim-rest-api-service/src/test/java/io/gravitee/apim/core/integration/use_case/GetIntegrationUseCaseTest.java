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
import inmemory.InMemoryAlternative;
import inmemory.IntegrationCrudServiceInMemory;
import inmemory.LicenseCrudServiceInMemory;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.use_case.GetIntegrationUseCase.Input;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetIntegrationUseCaseTest {

    private static final String INTEGRATION_ID = "generated-id";
    private static final String NAME = "test-name";
    private static final String DESCRIPTION = "integration-description";
    private static final String PROVIDER = "test-provider";
    private static final ZonedDateTime CREATED_AT = Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault());
    private static final ZonedDateTime UPDATED_AT = CREATED_AT;
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENV_ID = "my-env";
    private static final Integration.AgentStatus AGENT_STATUS = Integration.AgentStatus.DISCONNECTED;

    IntegrationCrudServiceInMemory integrationCrudServiceInMemory = new IntegrationCrudServiceInMemory();
    LicenseCrudServiceInMemory licenseCrudService = new LicenseCrudServiceInMemory();

    GetIntegrationUseCase usecase;

    @BeforeEach
    void setUp() {
        usecase = new GetIntegrationUseCase(integrationCrudServiceInMemory, new LicenseDomainService(licenseCrudService));
        var integration = List.of(IntegrationFixture.anIntegration().withId(INTEGRATION_ID));
        integrationCrudServiceInMemory.initWith(integration);

        licenseCrudService.createOrganizationLicense(ORGANIZATION_ID, "license-base64");
    }

    @AfterEach
    void tearDown() {
        Stream.of(integrationCrudServiceInMemory, licenseCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_get_integration() {
        //Given
        var input = new Input(INTEGRATION_ID, ORGANIZATION_ID);

        //When
        GetIntegrationUseCase.Output output = usecase.execute(input);

        //Then
        assertThat(output).isNotNull();
        assertThat(output.integration().getId()).isEqualTo(INTEGRATION_ID);
        assertThat(output.integration())
            .extracting(
                Integration::getName,
                Integration::getDescription,
                Integration::getProvider,
                Integration::getEnvironmentId,
                Integration::getCreatedAt,
                Integration::getUpdatedAt,
                Integration::getAgentStatus
            )
            .containsExactly(NAME, DESCRIPTION, PROVIDER, ENV_ID, CREATED_AT, UPDATED_AT, AGENT_STATUS);
    }

    @Test
    void should_throw_error_when_integration_not_found() {
        var input = new Input("not-existing-integration-id", ORGANIZATION_ID);

        assertThatExceptionOfType(IntegrationNotFoundException.class)
            .isThrownBy(() -> usecase.execute(input))
            .withMessage("Integration not found.");
    }

    @Test
    void should_throw_when_no_license_found() {
        // Given
        licenseCrudService.reset();

        // When
        var throwable = Assertions.catchThrowable(() -> usecase.execute(new Input(INTEGRATION_ID, ORGANIZATION_ID)));

        // Then
        assertThat(throwable).isInstanceOf(NotAllowedDomainException.class);
    }
}
