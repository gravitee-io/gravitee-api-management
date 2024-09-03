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
import static java.util.Optional.of;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.IntegrationFixture;
import fixtures.core.model.LicenseFixtures;
import inmemory.AsyncJobQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IntegrationAgentInMemory;
import inmemory.IntegrationQueryServiceInMemory;
import inmemory.LicenseCrudServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.integration.model.IntegrationView;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.membership.domain_service.IntegrationPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.data.domain.Page;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GetIntegrationsUseCaseTest {

    private static final String ENV_ID = "my-env";
    private static final String ORGANIZATION_ID = "my-org";
    private static final String INTEGRATION_ID = "integration-id";
    private static final String USER_ID = "user-id";
    private static final int PAGE_NUMBER = 1;
    private static final int PAGE_SIZE = 5;
    private static final Pageable pageable = new PageableImpl(PAGE_NUMBER, PAGE_SIZE);
    IntegrationAgentInMemory integrationAgent = new IntegrationAgentInMemory();
    AsyncJobQueryServiceInMemory asyncJobQueryService = new AsyncJobQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudServiceInMemory = new MembershipCrudServiceInMemory();
    RoleQueryServiceInMemory roleQueryServiceInMemory = new RoleQueryServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudServiceInMemory);
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    IntegrationQueryServiceInMemory integrationQueryServiceInMemory = new IntegrationQueryServiceInMemory();
    LicenseManager licenseManager = mock(LicenseManager.class);

    GetIntegrationsUseCase usecase;

    @BeforeEach
    void setUp() {
        var integrationPrimaryOwnerDomainService = new IntegrationPrimaryOwnerDomainService(
            membershipCrudServiceInMemory,
            roleQueryServiceInMemory,
            membershipQueryService,
            userCrudService
        );
        usecase =
            new GetIntegrationsUseCase(
                integrationQueryServiceInMemory,
                new LicenseDomainService(new LicenseCrudServiceInMemory(), licenseManager),
                integrationAgent,
                integrationPrimaryOwnerDomainService,
                asyncJobQueryService
            );

        roleQueryServiceInMemory.resetSystemRoles(ORGANIZATION_ID);
        givenExistingUsers(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );
        givenExistingMemberships(
            List.of(
                Membership
                    .builder()
                    .referenceType(Membership.ReferenceType.INTEGRATION)
                    .referenceId(INTEGRATION_ID)
                    .memberType(Membership.Type.USER)
                    .memberId(USER_ID)
                    .roleId(integrationPrimaryOwnerRoleId(ORGANIZATION_ID))
                    .build()
            )
        );

        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anEnterpriseLicense());
    }

    @AfterEach
    void tearDown() {
        Stream.of(integrationQueryServiceInMemory, integrationQueryServiceInMemory).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_integrations_with_specific_env_id() {
        //Given
        var expected = IntegrationFixture.anIntegration();
        integrationAgent.configureAgentFor(expected.getId(), IntegrationAgent.Status.CONNECTED);
        integrationQueryServiceInMemory.initWith(
            List.of(expected, IntegrationFixture.anIntegration("falseEnvID"), IntegrationFixture.anIntegration("anotherFalseEnvID"))
        );
        var input = GetIntegrationsUseCase.Input
            .builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(ENV_ID)
            .pageable(of(pageable))
            .build();

        //When
        var output = usecase.execute(input);

        //Then
        assertThat(output).isNotNull();
        assertThat(output.integrations())
            .extracting(Page::getContent, Page::getPageNumber, Page::getPageElements, Page::getTotalElements)
            .containsExactly(
                List.of(
                    new IntegrationView(
                        expected,
                        IntegrationView.AgentStatus.CONNECTED,
                        null,
                        new IntegrationView.PrimaryOwner(USER_ID, "jane.doe@gravitee.io", "Jane Doe")
                    )
                ),
                PAGE_NUMBER,
                output.integrations().getPageElements(),
                (long) output.integrations().getContent().size()
            );
    }

    @Test
    void should_return_integrations_with_default_pageable() {
        //Given
        var expected = IntegrationFixture.anIntegration();
        integrationAgent.configureAgentFor(expected.getId(), IntegrationAgent.Status.CONNECTED);
        integrationQueryServiceInMemory.initWith(List.of(expected));
        var input = new GetIntegrationsUseCase.Input(ORGANIZATION_ID, ENV_ID);

        //When
        var output = usecase.execute(input);

        //Then
        assertThat(output).isNotNull();
        assertThat(output.integrations())
            .extracting(Page::getContent, Page::getPageNumber, Page::getPageElements, Page::getTotalElements)
            .containsExactly(
                List.of(
                    new IntegrationView(
                        expected,
                        IntegrationView.AgentStatus.CONNECTED,
                        null,
                        new IntegrationView.PrimaryOwner(USER_ID, "jane.doe@gravitee.io", "Jane Doe")
                    )
                ),
                PAGE_NUMBER,
                output.integrations().getPageElements(),
                (long) output.integrations().getContent().size()
            );
    }

    @Test
    void should_throw_when_no_enterprise_license_found() {
        // Given
        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anOssLicense());

        // When
        var throwable = Assertions.catchThrowable(() -> usecase.execute(new GetIntegrationsUseCase.Input(ORGANIZATION_ID, ENV_ID)));

        // Then
        assertThat(throwable).isInstanceOf(NotAllowedDomainException.class);
    }

    private void givenExistingUsers(List<BaseUserEntity> users) {
        userCrudService.initWith(users);
    }

    private void givenExistingMemberships(List<Membership> memberships) {
        membershipCrudServiceInMemory.initWith(memberships);
    }
}
