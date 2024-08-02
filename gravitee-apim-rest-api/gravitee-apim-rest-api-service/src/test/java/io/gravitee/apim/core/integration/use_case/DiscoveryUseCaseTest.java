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
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.IntegrationApiFixtures;
import fixtures.core.model.IntegrationFixture;
import fixtures.core.model.LicenseFixtures;
import inmemory.ApiQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IntegrationAgentInMemory;
import inmemory.IntegrationCrudServiceInMemory;
import inmemory.LicenseCrudServiceInMemory;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.node.api.license.LicenseManager;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DiscoveryUseCaseTest {

    private static final String ENV_ID = "my-env";
    private static final String ORGANIZATION_ID = "my-org";
    private static final String INTEGRATION_ID = "integration-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENV_ID, USER_ID);

    IntegrationAgentInMemory integrationAgent = new IntegrationAgentInMemory();
    ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    IntegrationCrudServiceInMemory integrationCrudService = new IntegrationCrudServiceInMemory();
    LicenseManager licenseManager = mock(LicenseManager.class);
    DiscoveryUseCase usecase;

    @BeforeEach
    void setUp() {
        usecase =
            new DiscoveryUseCase(
                integrationAgent,
                new LicenseDomainService(new LicenseCrudServiceInMemory(), licenseManager),
                apiQueryService,
                integrationCrudService
            );

        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anEnterpriseLicense());
    }

    @AfterEach
    void tearDown() {
        Stream.of(integrationAgent, integrationCrudService, apiQueryService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_throw_exception_if_no_license_found() {
        // Given
        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anOssLicense());

        // When
        usecase.execute(new DiscoveryUseCase.Input(INTEGRATION_ID, AUDIT_INFO)).test().assertError(NotAllowedDomainException.class);
    }

    @Test
    void should_throw_exception_if_no_integration_found() {
        //When
        usecase.execute(new DiscoveryUseCase.Input(INTEGRATION_ID, AUDIT_INFO)).test().assertError(IntegrationNotFoundException.class);
    }

    @Test
    void should_discover_new_api_if_this_api_not_exist() {
        //Given
        integrationCrudService.initWith(List.of(IntegrationFixture.anIntegration(ENV_ID)));
        IntegrationApi api = IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID);
        integrationAgent.initWith(List.of(api));
        apiQueryService.initWith(List.of());

        //When
        var result = usecase.execute(new DiscoveryUseCase.Input(INTEGRATION_ID, AUDIT_INFO)).blockingGet();

        //Then
        assertThat(result.apis())
            .containsExactlyInAnyOrder(new DiscoveryUseCase.Output.PreviewApi(api.id(), api.name(), DiscoveryUseCase.Output.State.NEW));
    }

    @Test
    void should_not_discover_new_api_if_this_api_is_already_ingested() {
        //Given
        integrationCrudService.initWith(List.of(IntegrationFixture.anIntegration(ENV_ID)));
        IntegrationApi api = IntegrationApiFixtures.anIntegrationApiForIntegration(INTEGRATION_ID);
        var discoveredApiId = ApiModelFactory.generateFederatedApiId(ENV_ID, INTEGRATION_ID, api);
        integrationAgent.initWith(List.of(api));
        apiQueryService.initWith(List.of(ApiFixtures.aFederatedApi().toBuilder().id(discoveredApiId).name("A name").build()));

        //When
        var result = usecase.execute(new DiscoveryUseCase.Input(INTEGRATION_ID, AUDIT_INFO)).blockingGet();

        //Then
        assertThat(result.apis())
            .containsExactlyInAnyOrder(new DiscoveryUseCase.Output.PreviewApi(api.id(), api.name(), DiscoveryUseCase.Output.State.UPDATE));
    }
}
