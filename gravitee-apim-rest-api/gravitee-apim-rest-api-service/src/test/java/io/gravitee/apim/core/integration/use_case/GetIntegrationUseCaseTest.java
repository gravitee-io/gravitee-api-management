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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fixtures.core.model.AsyncJobFixture;
import fixtures.core.model.IntegrationFixture;
import fixtures.core.model.LicenseFixtures;
import inmemory.AsyncJobQueryServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IntegrationAgentInMemory;
import inmemory.IntegrationCrudServiceInMemory;
import inmemory.LicenseCrudServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.exception.NotAllowedDomainException;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.integration.model.AsyncJob;
import io.gravitee.apim.core.integration.model.IntegrationView;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.integration.use_case.GetIntegrationUseCase.Input;
import io.gravitee.apim.core.license.domain_service.LicenseDomainService;
import io.gravitee.apim.core.membership.domain_service.IntegrationPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.node.api.license.LicenseManager;
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
    private static final String USER_ID = "user-id";

    IntegrationCrudServiceInMemory integrationCrudServiceInMemory = new IntegrationCrudServiceInMemory();
    AsyncJobQueryServiceInMemory asyncJobQueryService = new AsyncJobQueryServiceInMemory();
    LicenseCrudServiceInMemory licenseCrudService = new LicenseCrudServiceInMemory();
    LicenseManager licenseManager = mock(LicenseManager.class);
    IntegrationAgentInMemory integrationAgent = new IntegrationAgentInMemory();
    MembershipCrudServiceInMemory membershipCrudServiceInMemory = new MembershipCrudServiceInMemory();
    RoleQueryServiceInMemory roleQueryServiceInMemory = new RoleQueryServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudServiceInMemory);
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    GetIntegrationUseCase usecase;

    @BeforeEach
    void setUp() {
        var integrationPrimaryOwnerDomainService = new IntegrationPrimaryOwnerDomainService(
            membershipCrudServiceInMemory,
            roleQueryServiceInMemory,
            membershipQueryService,
            userCrudService
        );
        usecase =
            new GetIntegrationUseCase(
                integrationCrudServiceInMemory,
                asyncJobQueryService,
                new LicenseDomainService(licenseCrudService, licenseManager),
                integrationAgent,
                integrationPrimaryOwnerDomainService
            );
        var integration = List.of(IntegrationFixture.anIntegration().withId(INTEGRATION_ID));
        integrationCrudServiceInMemory.initWith(integration);

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
        Stream
            .of(
                integrationCrudServiceInMemory,
                asyncJobQueryService,
                licenseCrudService,
                integrationAgent,
                membershipCrudServiceInMemory,
                roleQueryServiceInMemory,
                membershipQueryService,
                groupQueryService,
                userCrudService
            )
            .forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_get_integration() {
        //Given
        integrationAgent.configureAgentFor(INTEGRATION_ID, IntegrationAgent.Status.DISCONNECTED);
        var input = new Input(INTEGRATION_ID, ORGANIZATION_ID);

        //When
        GetIntegrationUseCase.Output output = usecase.execute(input);

        //Then
        assertThat(output).isNotNull();
        assertThat(output.integration().getId()).isEqualTo(INTEGRATION_ID);
        assertThat(output.integration())
            .extracting(
                IntegrationView::getName,
                IntegrationView::getDescription,
                IntegrationView::getProvider,
                IntegrationView::getEnvironmentId,
                IntegrationView::getCreatedAt,
                IntegrationView::getUpdatedAt,
                IntegrationView::getAgentStatus,
                IntegrationView::getPrimaryOwner
            )
            .containsExactly(
                NAME,
                DESCRIPTION,
                PROVIDER,
                ENV_ID,
                CREATED_AT,
                UPDATED_AT,
                IntegrationView.AgentStatus.DISCONNECTED,
                new IntegrationView.PrimaryOwner(USER_ID, "jane.doe@gravitee.io", "Jane Doe")
            );
    }

    @Test
    void should_return_integration_with_a_pending_job() {
        // Given
        integrationAgent.configureAgentFor(INTEGRATION_ID, IntegrationAgent.Status.DISCONNECTED);
        var job = givenAnAsyncJob(AsyncJobFixture.aPendingFederatedApiIngestionJob().withSourceId(INTEGRATION_ID));

        // When
        var output = usecase.execute(new Input(INTEGRATION_ID, ORGANIZATION_ID));

        // Then
        assertThat(output.integration().getPendingJob()).isEqualTo(job);
    }

    @Test
    void should_throw_error_when_integration_not_found() {
        var input = new Input("not-existing-integration-id", ORGANIZATION_ID);

        assertThatExceptionOfType(IntegrationNotFoundException.class)
            .isThrownBy(() -> usecase.execute(input))
            .withMessage("Integration not found.");
    }

    @Test
    void should_throw_when_no_enterprise_license_found() {
        // Given
        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION_ID)).thenReturn(LicenseFixtures.anOssLicense());

        // When
        var throwable = Assertions.catchThrowable(() -> usecase.execute(new Input(INTEGRATION_ID, ORGANIZATION_ID)));

        // Then
        assertThat(throwable).isInstanceOf(NotAllowedDomainException.class);
    }

    private AsyncJob givenAnAsyncJob(AsyncJob job) {
        asyncJobQueryService.initWith(List.of(job));
        return job;
    }

    private void givenExistingUsers(List<BaseUserEntity> users) {
        userCrudService.initWith(users);
    }

    private void givenExistingMemberships(List<Membership> memberships) {
        membershipCrudServiceInMemory.initWith(memberships);
    }
}
