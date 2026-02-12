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
package io.gravitee.apim.infra.domain_service.subscription;

import static fixtures.core.model.MembershipFixtures.anApplicationPrimaryOwnerUserMembership;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.ApplicationModelFixtures;
import fixtures.core.model.AuditInfoFixtures;
import inmemory.*;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_key.domain_service.GenerateApiKeyDomainService;
import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApplicationPrimaryOwnerDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.domain_service.AcceptSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.RejectSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.SubscriptionCRDSpecDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.adapter.SubscriptionAdapterImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SubscriptionCRDSpecDomainServiceImplTest {

    private static final Instant THEN = Instant.parse("2023-10-22T10:15:30Z");

    private static final String ORGANIZATION_ID = "TEST";
    private static final String ENVIRONMENT_ID = "TEST";

    private static final String USER_ID = "user-id";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID);

    private static final String API_ID = "api-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String PLAN_ID = "plan-id";
    private static final String SUBSCRIPTION_ID = "subscription-id";

    private static final SubscriptionCRDSpec SPEC = SubscriptionCRDSpec.builder()
        .id(SUBSCRIPTION_ID)
        .referenceId(API_ID)
        .referenceType(SubscriptionReferenceType.API)
        .applicationId(APPLICATION_ID)
        .planId(PLAN_ID)
        .build();

    private final SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    private final TriggerNotificationDomainServiceInMemory notificationService = new TriggerNotificationDomainServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final ApiKeyCrudServiceInMemory apiKeyCrudService = new ApiKeyCrudServiceInMemory();
    private final ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    private final IntegrationAgentInMemory integrationAgent = new IntegrationAgentInMemory();
    private final ApiKeyQueryServiceInMemory apiKeyQueryService = new ApiKeyQueryServiceInMemory();
    private final SubscriptionAdapterImpl subscriptionAdapter = new SubscriptionAdapterImpl();
    private final GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    private final ApplicationPrimaryOwnerDomainService applicationPrimaryOwnerDomainService = new ApplicationPrimaryOwnerDomainService(
        groupQueryService,
        membershipQueryService,
        roleQueryService,
        userCrudService
    );
    private final MetadataCrudServiceInMemory metadataCrudService = new MetadataCrudServiceInMemory();
    private final IntegrationCrudServiceInMemory integrationCrudService = new IntegrationCrudServiceInMemory();

    private final SubscriptionService subscriptionService = Mockito.mock(SubscriptionService.class);

    private SubscriptionCRDSpecDomainService cut;

    @BeforeAll
    static void init() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(THEN, ZoneId.systemDefault()));
    }

    @BeforeEach
    void setUp() {
        when(subscriptionService.create(eq(EXECUTION_CONTEXT), any(), isNull(), eq(SUBSCRIPTION_ID))).thenReturn(
            subscriptionAdapter.map(subscriptionAdapter.fromSpec(SPEC))
        );

        subscriptionCrudService.initWith(
            List.of(subscriptionAdapter.fromSpec(SPEC).toBuilder().status(SubscriptionEntity.Status.PENDING).subscribedBy(USER_ID).build())
        );

        apiCrudService.initWith(List.of(Api.builder().id(API_ID).build()));
        cut = new SubscriptionCRDSpecDomainServiceImpl(
            subscriptionService,
            subscriptionAdapter,
            acceptSubscriptionDomainService(),
            closeSubscriptionDomainService()
        );

        membershipQueryService.initWith(List.of(anApplicationPrimaryOwnerUserMembership(APPLICATION_ID, USER_ID, ORGANIZATION_ID)));
        applicationCrudService.initWith(
            List.of(
                ApplicationModelFixtures.anApplicationEntity()
                    .toBuilder()
                    .id(APPLICATION_ID)
                    .primaryOwner(PrimaryOwnerEntity.builder().id(USER_ID).displayName("Jane").build())
                    .build()
            )
        );
        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        userCrudService.initWith(
            List.of(BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );
    }

    @AfterEach
    void tearDown() {
        auditCrudService.reset();
        subscriptionCrudService.reset();
        apiCrudService.reset();
        planCrudService.reset();
        apiKeyCrudService.reset();
        notificationService.reset();
        userCrudService.reset();
        integrationAgent.reset();
        applicationCrudService.reset();
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_create() {
        givenExistingJWTPlan();

        cut.createOrUpdate(AUDIT_INFO, SPEC);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(subscriptionCrudService.get(SUBSCRIPTION_ID)).isNotNull();
            soft.assertThat(subscriptionCrudService.get(SUBSCRIPTION_ID).getStatus()).isEqualTo(SubscriptionEntity.Status.ACCEPTED);
            soft.assertThat(subscriptionCrudService.get(SUBSCRIPTION_ID).getOrigin()).isEqualTo(OriginContext.Origin.KUBERNETES);
        });
    }

    @Test
    void should_update() {
        givenExistingJWTPlan();

        cut.createOrUpdate(AUDIT_INFO, SPEC.toBuilder().endingAt(ZonedDateTime.now()).build());

        verify(subscriptionService).update(EXECUTION_CONTEXT, subscriptionAdapter.fromCoreForUpdate(subscriptionAdapter.fromSpec(SPEC)));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(subscriptionCrudService.get(SUBSCRIPTION_ID)).isNotNull();
            soft.assertThat(subscriptionCrudService.get(SUBSCRIPTION_ID).getStatus()).isEqualTo(SubscriptionEntity.Status.ACCEPTED);
        });
    }

    @Test
    void should_close_on_delete() {
        cut.delete(AUDIT_INFO, SPEC.getId());

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(subscriptionCrudService.get(SUBSCRIPTION_ID)).isNotNull();
            soft.assertThat(subscriptionCrudService.get(SUBSCRIPTION_ID).getStatus()).isEqualTo(SubscriptionEntity.Status.REJECTED);
        });
    }

    private void givenExistingJWTPlan() {
        givenExistingPlan(
            Plan.builder()
                .id(PLAN_ID)
                .apiId(API_ID)
                .definitionVersion(DefinitionVersion.V2)
                .planDefinitionV2(io.gravitee.definition.model.Plan.builder().security("JWT").build())
                .build()
        );
    }

    private void givenExistingPlan(Plan plan) {
        planCrudService.initWith(List.of(plan));
    }

    private void givenExistingSubscription(SubscriptionEntity subscription) {
        subscriptionCrudService.initWith(List.of(subscription));
    }

    private CloseSubscriptionDomainService closeSubscriptionDomainService() {
        return new CloseSubscriptionDomainService(
            subscriptionCrudService,
            applicationCrudService,
            auditDomainService(),
            notificationService,
            rejectSubscriptionDomainService(),
            revokeApiKeyDomainService(),
            apiCrudService,
            integrationAgent
        );
    }

    private RevokeApiKeyDomainService revokeApiKeyDomainService() {
        return new RevokeApiKeyDomainService(
            apiKeyCrudService,
            apiKeyQueryService,
            subscriptionCrudService,
            auditDomainService(),
            notificationService
        );
    }

    private RejectSubscriptionDomainService rejectSubscriptionDomainService() {
        return new RejectSubscriptionDomainService(
            subscriptionCrudService,
            auditDomainService(),
            notificationService,
            userCrudService,
            applicationPrimaryOwnerDomainService
        );
    }

    private AcceptSubscriptionDomainService acceptSubscriptionDomainService() {
        return new AcceptSubscriptionDomainService(
            subscriptionCrudService,
            auditDomainService(),
            apiCrudService,
            applicationCrudService,
            planCrudService,
            generateApiKeyDomainService(),
            integrationAgent,
            notificationService,
            userCrudService,
            applicationPrimaryOwnerDomainService,
            metadataCrudService,
            integrationCrudService
        );
    }

    private AuditDomainService auditDomainService() {
        return new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
    }

    private GenerateApiKeyDomainService generateApiKeyDomainService() {
        return new GenerateApiKeyDomainService(
            apiKeyCrudService,
            new ApiKeyQueryServiceInMemory(apiKeyCrudService),
            applicationCrudService,
            auditDomainService()
        );
    }
}
