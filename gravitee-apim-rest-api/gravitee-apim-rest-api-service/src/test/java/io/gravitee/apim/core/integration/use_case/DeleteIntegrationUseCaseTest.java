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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.IntegrationFixture;
import inmemory.ApiQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.IntegrationCrudServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.AssociatedApisFoundException;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.core.membership.domain_service.DeleteMembershipDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.rest.api.service.AuditService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeleteIntegrationUseCaseTest {

    private static final String INTEGRATION_ID = "integration-id";

    IntegrationCrudServiceInMemory integrationCrudServiceInMemory = new IntegrationCrudServiceInMemory();
    ApiQueryServiceInMemory apiQueryServiceInMemory = new ApiQueryServiceInMemory();
    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    DeleteIntegrationUseCase usecase;

    @BeforeEach
    void setUp() {
        IntegrationCrudService integrationCrudService = integrationCrudServiceInMemory;
        ApiQueryService apiQueryService = apiQueryServiceInMemory;
        AuditDomainService auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        DeleteMembershipDomainService deleteMembershipDomainService = new DeleteMembershipDomainService(
            membershipQueryService,
            membershipCrudService,
            auditDomainService
        );
        usecase = new DeleteIntegrationUseCase(integrationCrudService, apiQueryService, deleteMembershipDomainService);
    }

    @AfterEach
    void tearDown() {
        integrationCrudServiceInMemory.reset();
        apiQueryServiceInMemory.reset();
    }

    @Test
    void should_delete_integration() {
        integrationCrudServiceInMemory.initWith(List.of(IntegrationFixture.anIntegration()));
        var input = DeleteIntegrationUseCase.Input.builder().integrationId(INTEGRATION_ID).build();

        usecase.execute(input);

        assertThat(integrationCrudServiceInMemory.storage()).isEmpty();
    }

    @Test
    void should_delete_integration_memberships() {
        var integrationMember = Membership
            .builder()
            .id("membership-id")
            .roleId("role-id")
            .referenceId(INTEGRATION_ID)
            .referenceType(Membership.ReferenceType.INTEGRATION)
            .memberId("user-id")
            .memberType(Membership.Type.USER)
            .build();
        givenExistingMemberships(List.of(integrationMember));
        integrationCrudServiceInMemory.initWith(List.of(IntegrationFixture.anIntegration()));
        var input = DeleteIntegrationUseCase.Input.builder().integrationId(INTEGRATION_ID).build();

        usecase.execute(input);

        assertThat(membershipCrudService.storage()).hasSize(0);
    }

    @Test
    void should_throw_error_when_associated_federated_apis_found() {
        integrationCrudServiceInMemory.initWith(List.of(IntegrationFixture.anIntegration()));
        apiQueryServiceInMemory.initWith(List.of(ApiFixtures.aFederatedApi()));
        var input = DeleteIntegrationUseCase.Input.builder().integrationId(INTEGRATION_ID).build();

        assertThatThrownBy(() -> usecase.execute(input))
            .isInstanceOf(AssociatedApisFoundException.class)
            .hasMessage("Associated APIs found for federation with id: integration-id");
    }

    @Test
    void should_throw_error_when_integration_not_found() {
        var input = DeleteIntegrationUseCase.Input.builder().integrationId(INTEGRATION_ID).build();
        assertThatThrownBy(() -> usecase.execute(input))
            .isInstanceOf(IntegrationNotFoundException.class)
            .hasMessage("Integration not found.");
    }

    private void givenExistingMemberships(List<Membership> memberships) {
        membershipCrudService.initWith(memberships);
    }
}
