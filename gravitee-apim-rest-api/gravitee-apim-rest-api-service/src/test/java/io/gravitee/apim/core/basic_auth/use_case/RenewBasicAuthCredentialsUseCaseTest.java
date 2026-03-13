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
package io.gravitee.apim.core.basic_auth.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.BasicAuthCredentialsCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.basic_auth.domain_service.GenerateBasicAuthCredentialsDomainService;
import io.gravitee.apim.core.basic_auth.model.BasicAuthCredentialsEntity;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RenewBasicAuthCredentialsUseCaseTest {

    private static final String API_ID = "api-id";
    private static final String ORGANIZATION_ID = "org-id";
    private static final String ENVIRONMENT_ID = "env-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    private final SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    private final BasicAuthCredentialsCrudServiceInMemory basicAuthCredentialsCrudService = new BasicAuthCredentialsCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    private RenewBasicAuthCredentialsUseCase useCase;

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(
            auditCrudService,
            userCrudService,
            new io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor()
        );
        var generateService = new GenerateBasicAuthCredentialsDomainService(basicAuthCredentialsCrudService, auditDomainService);
        useCase = new RenewBasicAuthCredentialsUseCase(subscriptionCrudService, planCrudService, generateService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(subscriptionCrudService, planCrudService, basicAuthCredentialsCrudService, auditCrudService, userCrudService).forEach(
            InMemoryAlternative::reset
        );
    }

    @Test
    void should_renew_credentials_for_accepted_subscription() {
        var subscription = givenAcceptedBasicAuthSubscription();
        var plan = givenBasicAuthPlan();

        var existingCredentials = BasicAuthCredentialsEntity.builder()
            .id("old-creds-id")
            .username("old-user")
            .password("hashed-old-password")
            .applicationId(subscription.getApplicationId())
            .subscriptions(List.of(subscription.getId()))
            .environmentId(subscription.getEnvironmentId())
            .build();
        basicAuthCredentialsCrudService.initWith(List.of(existingCredentials));

        var result = useCase.execute(new RenewBasicAuthCredentialsUseCase.Input(API_ID, subscription.getId(), AUDIT_INFO));

        assertThat(result.credentials()).isNotNull();
        assertThat(result.credentials().getUsername()).isNotEmpty();
        assertThat(result.credentials().getPassword()).isNotEmpty();
        assertThat(result.credentials().getUsername()).isNotEqualTo("old-user");

        var stored = basicAuthCredentialsCrudService.storage();
        assertThat(stored).hasSize(2);
        var oldCreds = stored
            .stream()
            .filter(c -> c.getId().equals("old-creds-id"))
            .findFirst()
            .orElseThrow();
        assertThat(oldCreds.isRevoked()).isTrue();
    }

    @Test
    void should_reject_for_pending_subscription() {
        var subscription = SubscriptionFixtures.aSubscription()
            .toBuilder()
            .id("sub-pending")
            .apiId(API_ID)
            .planId("plan-basic-auth")
            .status(SubscriptionEntity.Status.PENDING)
            .build();
        subscriptionCrudService.initWith(List.of(subscription));
        givenBasicAuthPlan();

        assertThatThrownBy(() ->
            useCase.execute(new RenewBasicAuthCredentialsUseCase.Input(API_ID, "sub-pending", AUDIT_INFO))
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_reject_for_non_basic_auth_plan() {
        var subscription = SubscriptionFixtures.aSubscription()
            .toBuilder()
            .id("sub-apikey")
            .apiId(API_ID)
            .planId("plan-apikey")
            .status(SubscriptionEntity.Status.ACCEPTED)
            .build();
        subscriptionCrudService.initWith(List.of(subscription));

        var plan = PlanFixtures.HttpV4.anApiKey().toBuilder().id("plan-apikey").apiId(API_ID).build();
        planCrudService.initWith(List.of(plan));

        assertThatThrownBy(() ->
            useCase.execute(new RenewBasicAuthCredentialsUseCase.Input(API_ID, "sub-apikey", AUDIT_INFO))
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_reject_for_wrong_api() {
        var subscription = givenAcceptedBasicAuthSubscription();
        givenBasicAuthPlan();

        assertThatThrownBy(() ->
            useCase.execute(new RenewBasicAuthCredentialsUseCase.Input("other-api", subscription.getId(), AUDIT_INFO))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    private SubscriptionEntity givenAcceptedBasicAuthSubscription() {
        var subscription = SubscriptionFixtures.aSubscription()
            .toBuilder()
            .id("sub-basic-auth")
            .apiId(API_ID)
            .planId("plan-basic-auth")
            .status(SubscriptionEntity.Status.ACCEPTED)
            .environmentId(ENVIRONMENT_ID)
            .endingAt(null)
            .build();
        subscriptionCrudService.initWith(List.of(subscription));
        return subscription;
    }

    private Plan givenBasicAuthPlan() {
        var plan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id("plan-basic-auth")
            .apiId(API_ID)
            .definitionVersion(DefinitionVersion.V4)
            .apiType(ApiType.PROXY)
            .planDefinitionHttpV4(
                io.gravitee.definition.model.v4.plan.Plan.builder()
                    .id("plan-basic-auth")
                    .security(PlanSecurity.builder().type("BASIC_AUTH").build())
                    .build()
            )
            .build();
        planCrudService.initWith(List.of(plan));
        return plan;
    }
}
