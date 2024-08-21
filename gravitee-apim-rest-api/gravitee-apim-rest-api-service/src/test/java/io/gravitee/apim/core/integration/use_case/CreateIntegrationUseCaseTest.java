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

import static fixtures.core.model.RoleFixtures.integrationPrimaryOwnerRoleId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.IntegrationFixture;
import fixtures.core.model.LicenseFixtures;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IntegrationCrudServiceInMemory;
import inmemory.LicenseCrudServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.use_case.CreateIntegrationUseCase.Input;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.membership.domain_service.IntegrationPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.IntegrationPrimaryOwnerFactory;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
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
    private static final String USER_ID = "user-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENV_ID, USER_ID);

    IntegrationCrudServiceInMemory integrationCrudServiceInMemory = new IntegrationCrudServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudServiceInMemory = new MembershipCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryServiceInMemory = new MembershipQueryServiceInMemory(membershipCrudServiceInMemory);
    ParametersQueryServiceInMemory parametersQueryServiceInMemory = new ParametersQueryServiceInMemory();
    RoleQueryServiceInMemory roleQueryServiceInMemory = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudServiceInMemory = new UserCrudServiceInMemory();
    GroupQueryServiceInMemory groupQueryServiceInMemory = new GroupQueryServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

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
        parametersQueryServiceInMemory.initWith(
            List.of(
                new Parameter(
                    Key.API_PRIMARY_OWNER_MODE.key(),
                    ENVIRONMENT_ID,
                    ParameterReferenceType.ENVIRONMENT,
                    ApiPrimaryOwnerMode.USER.name()
                )
            )
        );

        IntegrationPrimaryOwnerFactory integrationPrimaryOwnerFactory = new IntegrationPrimaryOwnerFactory(
            membershipQueryServiceInMemory,
            parametersQueryServiceInMemory,
            roleQueryServiceInMemory,
            userCrudServiceInMemory,
            groupQueryServiceInMemory
        );
        IntegrationPrimaryOwnerDomainService integrationPrimaryOwnerDomainService = new IntegrationPrimaryOwnerDomainService(
            membershipCrudServiceInMemory,
            roleQueryServiceInMemory,
            membershipQueryService,
            userCrudService
        );

        IntegrationCrudService integrationCrudService = integrationCrudServiceInMemory;
        usecase =
            new CreateIntegrationUseCase(
                integrationCrudService,
                new LicenseDomainService(new LicenseCrudServiceInMemory(), licenseManager),
                integrationPrimaryOwnerFactory,
                integrationPrimaryOwnerDomainService
            );

        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anEnterpriseLicense());
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                integrationCrudServiceInMemory,
                membershipCrudServiceInMemory,
                membershipQueryServiceInMemory,
                parametersQueryServiceInMemory,
                roleQueryServiceInMemory,
                userCrudServiceInMemory,
                groupQueryServiceInMemory
            )
            .forEach(InMemoryAlternative::reset);
        reset(licenseManager);
    }

    @Test
    void should_create_new_integration() {
        //Given
        givenExistingUsers(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );
        var input = new Input(IntegrationFixture.anIntegration(), AUDIT_INFO);

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

    @Test
    void should_create_new_primary_owner_membership() {
        //Given
        givenExistingUsers(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );
        roleQueryServiceInMemory.resetSystemRoles(ORGANIZATION_ID);
        var input = new Input(IntegrationFixture.anIntegration(), AUDIT_INFO);

        //When
        usecase.execute(input);

        //Then
        assertThat(membershipCrudServiceInMemory.storage())
            .containsExactly(
                Membership
                    .builder()
                    .id(INTEGRATION_ID)
                    .memberId(USER_ID)
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.INTEGRATION)
                    .referenceId(INTEGRATION_ID)
                    .roleId(integrationPrimaryOwnerRoleId(ORGANIZATION_ID))
                    .createdAt(ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()))
                    .updatedAt(ZonedDateTime.ofInstant(INSTANT_NOW, ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_throw_when_no_enterprise_license_found() {
        // Given
        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anOssLicense());

        // When
        var throwable = Assertions.catchThrowable(() -> usecase.execute(new Input(IntegrationFixture.anIntegration(), AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(NotAllowedDomainException.class);
    }

    private void givenExistingUsers(List<BaseUserEntity> users) {
        userCrudServiceInMemory.initWith(users);
    }
}
