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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.IntegrationFixture;
import fixtures.core.model.LicenseFixtures;
import inmemory.InMemoryAlternative;
import inmemory.IntegrationCrudServiceInMemory;
import inmemory.LicenseCrudServiceInMemory;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.use_case.CreateIntegrationUseCase.Input;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateIntegrationUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String INTEGRATION_ID = "generated-id";
    private static final String NAME = "test-name";
    private static final String DESCRIPTION = "integration-description";
    private static final String PROVIDER = "test-provider";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ENV_ID = "my-env";
    private static final Integration.AgentStatus AGENT_STATUS = Integration.AgentStatus.DISCONNECTED;

    IntegrationCrudServiceInMemory integrationCrudServiceInMemory = new IntegrationCrudServiceInMemory();
    LicenseManager licenseManager = mock(LicenseManager.class);

    CreateIntegrationUseCase usecase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> INTEGRATION_ID);
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        IntegrationCrudService integrationCrudService = integrationCrudServiceInMemory;
        usecase =
            new CreateIntegrationUseCase(
                integrationCrudService,
                new LicenseDomainService(new LicenseCrudServiceInMemory(), licenseManager)
            );

        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anEnterpriseLicense());
    }

    @AfterEach
    void tearDown() {
        Stream.of(integrationCrudServiceInMemory).forEach(InMemoryAlternative::reset);
        reset(licenseManager);
    }

    @Test
    void should_create_new_integration() {
        //Given
        var input = new Input(IntegrationFixture.anIntegration(), ORGANIZATION_ID);

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
                Integration::getUpdatedAt,
                Integration::getAgentStatus
            )
            .containsExactly(
                NAME,
                DESCRIPTION,
                PROVIDER,
                ENV_ID,
                ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()),
                ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()),
                AGENT_STATUS
            );
    }

    @Test
    void should_throw_when_no_enterprise_license_found() {
        // Given
        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anOssLicense());

        // When
        var throwable = Assertions.catchThrowable(() -> usecase.execute(new Input(IntegrationFixture.anIntegration(), ORGANIZATION_ID)));

        // Then
        assertThat(throwable).isInstanceOf(NotAllowedDomainException.class);
    }
}
