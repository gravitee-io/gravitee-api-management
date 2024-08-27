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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.IntegrationFixture;
import fixtures.core.model.LicenseFixtures;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IntegrationCrudServiceInMemory;
import inmemory.LicenseCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.group.domain_service.ValidateGroupsDomainService;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.IntegrationGroupValidationException;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.use_case.UpdateIntegrationUseCase.Input;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.node.api.license.LicenseManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class UpdateIntegrationUseCaseTest {

    private static final String INTEGRATION_ID = "integration-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String PROVIDER = "test-provider";
    private static final String ENV_ID = "my-env";
    private static final String USER_ID = "my-user";
    private static final String GROUP_ID = "group-id";
    private static final ZonedDateTime CREATED_DATE = Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault());
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENV_ID, USER_ID);

    IntegrationCrudServiceInMemory integrationCrudServiceInMemory = new IntegrationCrudServiceInMemory();
    GroupQueryServiceInMemory queryServiceInMemory = new GroupQueryServiceInMemory();
    LicenseManager licenseManager = mock(LicenseManager.class);

    UpdateIntegrationUseCase usecase;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @BeforeEach
    void setUp() {
        IntegrationCrudService integrationCrudService = integrationCrudServiceInMemory;
        integrationCrudServiceInMemory.initWith(List.of(IntegrationFixture.anIntegration()));

        var validateGroupDomainService = new ValidateGroupsDomainService(queryServiceInMemory);

        usecase =
            new UpdateIntegrationUseCase(
                integrationCrudService,
                new LicenseDomainService(new LicenseCrudServiceInMemory(), licenseManager),
                validateGroupDomainService
            );

        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anEnterpriseLicense());
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @AfterEach
    void tearDownEach() {
        Stream.of(integrationCrudServiceInMemory).forEach(InMemoryAlternative::reset);
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

        //When
        var output = usecase.execute(new Input(updateIntegration, AUDIT_INFO));

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
                    .updatedAt(ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()))
                    .build()
            );
    }

    @Nested
    class ManageGroups {

        @Test
        public void should_add_group_to_an_integration() {
            //Given
            givenExistingGroup(List.of(Group.builder().id(GROUP_ID).name("group-name").build()));
            Integration updateIntegration = Integration
                .builder()
                .id(INTEGRATION_ID)
                .name("updated-integration")
                .description("updated-description")
                .groups(Set.of("group-id"))
                .build();

            //When
            var output = usecase.execute(new Input(updateIntegration, AUDIT_INFO));

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
                        .updatedAt(ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()))
                        .groups(Set.of(GROUP_ID))
                        .build()
                );
        }

        @Test
        public void should_remove_group_from_an_integration() {
            //Given
            integrationCrudServiceInMemory.initWith(
                List.of(IntegrationFixture.anIntegration().toBuilder().groups(Set.of(GROUP_ID)).build())
            );
            givenExistingGroup(List.of(Group.builder().id(GROUP_ID).name("group-name").build()));
            Integration updateIntegration = Integration
                .builder()
                .id(INTEGRATION_ID)
                .name("updated-integration")
                .description("updated-description")
                .build();

            //When
            var output = usecase.execute(new Input(updateIntegration, AUDIT_INFO));

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
                        .updatedAt(ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()))
                        .build()
                );
        }

        @Test
        public void should_throw_an_exception_when_group_not_found() {
            Integration updateIntegration = Integration
                .builder()
                .id(INTEGRATION_ID)
                .name("updated-integration")
                .description("updated-description")
                .groups(Set.of("group-id"))
                .build();

            assertThatExceptionOfType(IntegrationGroupValidationException.class)
                .isThrownBy(() -> usecase.execute(new Input(updateIntegration, AUDIT_INFO)))
                .withMessage("Group validation failed during integration integration-id update");
        }
    }

    @Test
    public void should_throw_exception_when_integration_to_update_not_found() {
        var updateIntegration = Integration
            .builder()
            .id("not-existing-integration")
            .name("updated-integration")
            .description("updated-description")
            .build();

        assertThatExceptionOfType(IntegrationNotFoundException.class)
            .isThrownBy(() -> usecase.execute(new Input(updateIntegration, AUDIT_INFO)))
            .withMessage("Integration not found.");
    }

    @Test
    void should_throw_when_no_enterprise_license_found() {
        // Given
        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anOssLicense());

        // When
        var throwable = Assertions.catchThrowable(() ->
            usecase.execute(new Input(Integration.builder().id(INTEGRATION_ID).build(), AUDIT_INFO))
        );

        // Then
        assertThat(throwable).isInstanceOf(NotAllowedDomainException.class);
    }

    private void givenExistingGroup(List<Group> groups) {
        queryServiceInMemory.initWith(groups);
    }
}
