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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.IntegrationFixture;
import fixtures.core.model.LicenseFixtures;
import inmemory.InMemoryAlternative;
import inmemory.IntegrationAgentInMemory;
import inmemory.IntegrationCrudServiceInMemory;
import inmemory.IntegrationJobCrudServiceInMemory;
import inmemory.LicenseCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationJob;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StartIngestIntegrationApisUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String INTEGRATION_ID = "integration-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    IntegrationCrudServiceInMemory integrationCrudService = new IntegrationCrudServiceInMemory();
    IntegrationJobCrudServiceInMemory integrationJobCrudService = new IntegrationJobCrudServiceInMemory();

    IntegrationAgentInMemory integrationAgent = new IntegrationAgentInMemory();
    LicenseManager licenseManager = mock(LicenseManager.class);

    private StartIngestIntegrationApisUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(seed -> seed != null ? seed : "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        useCase =
            new StartIngestIntegrationApisUseCase(
                integrationCrudService,
                integrationJobCrudService,
                integrationAgent,
                new LicenseDomainService(new LicenseCrudServiceInMemory(), licenseManager)
            );

        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anEnterpriseLicense());
    }

    @AfterEach
    void tearDown() {
        Stream.of(integrationCrudService, integrationJobCrudService).forEach(InMemoryAlternative::reset);
        reset(licenseManager);
    }

    @Test
    void should_create_a_job_and_return_pending_status_when_a_job_has_started() {
        // Given
        givenAnIntegration(IntegrationFixture.anIntegration(ENVIRONMENT_ID).withId(INTEGRATION_ID));
        integrationAgent.configureApisNumberToIngest(INTEGRATION_ID, 10L);

        // When
        var result = useCase
            .execute(new StartIngestIntegrationApisUseCase.Input(INTEGRATION_ID, AUDIT_INFO))
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .values();

        // Then
        assertThat(result).containsExactly(IntegrationJob.Status.PENDING);
        assertThat(integrationJobCrudService.storage())
            .contains(
                IntegrationJob
                    .builder()
                    .id("generated-id")
                    .sourceId(INTEGRATION_ID)
                    .environmentId(ENVIRONMENT_ID)
                    .initiatorId(USER_ID)
                    .status(IntegrationJob.Status.PENDING)
                    .upperLimit(10L)
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_return_done_status_when_no_job_has_started_because_no_apis_to_ingest() {
        // Given
        givenAnIntegration(IntegrationFixture.anIntegration(ENVIRONMENT_ID).withId(INTEGRATION_ID));
        integrationAgent.configureApisNumberToIngest(INTEGRATION_ID, 0L);

        // When
        var result = useCase
            .execute(new StartIngestIntegrationApisUseCase.Input(INTEGRATION_ID, AUDIT_INFO))
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .values();

        // Then
        assertThat(integrationJobCrudService.storage()).isEmpty();
        assertThat(result).containsExactly(IntegrationJob.Status.SUCCESS);
    }

    @Test
    void should_throw_when_no_license_found() {
        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anOssLicense());

        useCase
            .execute(new StartIngestIntegrationApisUseCase.Input(INTEGRATION_ID, AUDIT_INFO))
            .test()
            .assertError(NotAllowedDomainException.class);
    }

    @Test
    void should_throw_when_no_integration_is_found() {
        // When
        var obs = useCase.execute(new StartIngestIntegrationApisUseCase.Input("unknown", AUDIT_INFO)).test();

        // Then
        obs.assertError(IntegrationNotFoundException.class);
    }

    private void givenAnIntegration(Integration integration) {
        integrationCrudService.initWith(List.of(integration));
    }
}
