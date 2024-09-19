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

import fixtures.ApplicationModelFixtures;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.MembershipFixtures;
import fixtures.core.model.MetadataFixtures;
import fixtures.core.model.PageFixture;
import fixtures.core.model.PlanFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApiCategoryQueryServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IndexerInMemory;
import inmemory.IntegrationAgentInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.MetadataCrudServiceInMemory;
import inmemory.PageCrudServiceInMemory;
import inmemory.PageQueryServiceInMemory;
import inmemory.PageRevisionCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.documentation.domain_service.DeleteApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.UpdateApiDocumentationDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.DeleteMembershipDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.plan.domain_service.DeletePlanDomainService;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.DeleteSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.RejectSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DeleteIngestedApisUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String INTEGRATION_ID = "integration-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String USER_ID = "user-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String MY_API = "my-api";

    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    PlanQueryServiceInMemory planQueryService = new PlanQueryServiceInMemory(planCrudService);
    SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    SubscriptionQueryServiceInMemory subscriptionQueryService = new SubscriptionQueryServiceInMemory(subscriptionCrudService);
    ApplicationCrudServiceInMemory applicationService = new ApplicationCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    PageCrudServiceInMemory pageCrudService = new PageCrudServiceInMemory();
    PageQueryServiceInMemory pageQueryService = new PageQueryServiceInMemory(pageCrudService);
    PageRevisionCrudServiceInMemory pageRevisionCrudServiceInMemory = new PageRevisionCrudServiceInMemory();
    IndexerInMemory indexer = new IndexerInMemory();
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainServiceInMemory = new TriggerNotificationDomainServiceInMemory();
    ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory = new ApiKeyCrudServiceInMemory();
    ApiKeyQueryServiceInMemory apiKeyQueryServiceInMemory = new ApiKeyQueryServiceInMemory(apiKeyCrudServiceInMemory);
    ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    ApiQueryServiceInMemory apiQueryServiceInMemory = new ApiQueryServiceInMemory(apiCrudServiceInMemory);
    MetadataCrudServiceInMemory metadataCrudServiceInMemory = new MetadataCrudServiceInMemory();
    ApiMetadataQueryServiceInMemory apiMetadataQueryServiceInMemory = new ApiMetadataQueryServiceInMemory(metadataCrudServiceInMemory);
    MembershipCrudServiceInMemory membershipCrudServiceInMemory = new MembershipCrudServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryServiceInMemory = new MembershipQueryServiceInMemory(membershipCrudServiceInMemory);
    RoleQueryServiceInMemory roleQueryServiceInMemory = new RoleQueryServiceInMemory();
    GroupQueryServiceInMemory groupQueryServiceInMemory = new GroupQueryServiceInMemory();
    ApiCategoryQueryServiceInMemory apiCategoryQueryServiceInMemory = new ApiCategoryQueryServiceInMemory();

    private DeleteIngestedApisUseCase useCase;

    @BeforeEach
    public void setUp() {
        var auditDomainService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        var revokeApiKeyDomainService = new RevokeApiKeyDomainService(
            apiKeyCrudServiceInMemory,
            apiKeyQueryServiceInMemory,
            subscriptionCrudService,
            auditDomainService,
            triggerNotificationDomainServiceInMemory
        );
        var rejectSubscriptionDomainService = new RejectSubscriptionDomainService(
            subscriptionCrudService,
            auditDomainService,
            triggerNotificationDomainServiceInMemory,
            userCrudService
        );
        var closeSubscriptionDomainService = new CloseSubscriptionDomainService(
            subscriptionCrudService,
            applicationService,
            auditDomainService,
            triggerNotificationDomainServiceInMemory,
            rejectSubscriptionDomainService,
            revokeApiKeyDomainService,
            apiCrudServiceInMemory,
            new IntegrationAgentInMemory()
        );
        var deleteSubscriptionDomainService = new DeleteSubscriptionDomainService(subscriptionCrudService, auditDomainService);
        var deletePlanDomainService = new DeletePlanDomainService(planCrudService, subscriptionQueryService, auditDomainService);
        var updateApiDocumentationService = new UpdateApiDocumentationDomainService(
            pageCrudService,
            pageRevisionCrudServiceInMemory,
            auditDomainService,
            indexer
        );
        var deleteApiDocumentationDomainService = new DeleteApiDocumentationDomainService(
            pageCrudService,
            pageQueryService,
            auditDomainService,
            updateApiDocumentationService,
            planQueryService,
            indexer
        );
        var apiMetadataDomainService = new ApiMetadataDomainService(
            metadataCrudServiceInMemory,
            apiMetadataQueryServiceInMemory,
            auditDomainService
        );
        var deleteMembershipDomainService = new DeleteMembershipDomainService(
            membershipQueryServiceInMemory,
            membershipCrudServiceInMemory,
            auditDomainService
        );
        var templateProcessor = new FreemarkerTemplateProcessor();
        var apiMetadataDecoderDomainService = new ApiMetadataDecoderDomainService(apiMetadataQueryServiceInMemory, templateProcessor);
        var apiPrimaryOwnerDomainService = new ApiPrimaryOwnerDomainService(
            auditDomainService,
            groupQueryServiceInMemory,
            membershipCrudServiceInMemory,
            membershipQueryServiceInMemory,
            roleQueryServiceInMemory,
            userCrudService
        );
        var apiIndexerDomainService = new ApiIndexerDomainService(
            apiMetadataDecoderDomainService,
            apiPrimaryOwnerDomainService,
            apiCategoryQueryServiceInMemory,
            indexer
        );

        useCase =
            new DeleteIngestedApisUseCase(
                apiQueryServiceInMemory,
                planQueryService,
                subscriptionQueryService,
                closeSubscriptionDomainService,
                deleteSubscriptionDomainService,
                deletePlanDomainService,
                pageQueryService,
                deleteApiDocumentationDomainService,
                auditDomainService,
                apiMetadataDomainService,
                deleteMembershipDomainService,
                apiCrudServiceInMemory,
                apiIndexerDomainService
            );
        initializePrimaryOwnerData();
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                planCrudService,
                planQueryService,
                subscriptionCrudService,
                subscriptionQueryService,
                applicationService,
                auditCrudService,
                userCrudService,
                pageCrudService,
                pageQueryService,
                pageRevisionCrudServiceInMemory,
                indexer,
                apiKeyCrudServiceInMemory,
                apiKeyQueryServiceInMemory,
                apiCrudServiceInMemory,
                apiQueryServiceInMemory,
                metadataCrudServiceInMemory,
                apiMetadataQueryServiceInMemory,
                membershipCrudServiceInMemory,
                membershipQueryServiceInMemory,
                roleQueryServiceInMemory,
                groupQueryServiceInMemory,
                apiCategoryQueryServiceInMemory
            )
            .forEach(InMemoryAlternative::reset);
    }

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @ParameterizedTest
    @EnumSource(value = Api.ApiLifecycleState.class, mode = EnumSource.Mode.EXCLUDE, names = { "PUBLISHED" })
    public void should_delete_all_apis_except_published_one(Api.ApiLifecycleState apiLifecycleState) {
        //given
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aFederatedApi().toBuilder().apiLifecycleState(apiLifecycleState).build()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var input = DeleteIngestedApisUseCase.Input.builder().integrationId(INTEGRATION_ID).auditInfo(auditInfo).build();

        //when
        var output = useCase.execute(input);

        //then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(apiCrudServiceInMemory.storage()).isNotNull().isEmpty();
            softly.assertThat(output.deleted()).isEqualTo(1);
            softly.assertThat(output.skipped()).isEqualTo(0);
            softly.assertThat(output.errors()).isEqualTo(0);
        });
    }

    @Test
    public void should_not_delete_published_api() {
        //given
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aFederatedApi()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var input = DeleteIngestedApisUseCase.Input.builder().integrationId(INTEGRATION_ID).auditInfo(auditInfo).build();

        //when
        var output = useCase.execute(input);

        //then
        SoftAssertions.assertSoftly(softly -> {
            softly
                .assertThat(apiCrudServiceInMemory.storage())
                .hasSize(1)
                .extracting(Api::getApiLifecycleState)
                .containsExactly(Api.ApiLifecycleState.PUBLISHED);
            softly.assertThat(output.deleted()).isEqualTo(0);
            softly.assertThat(output.skipped()).isEqualTo(1);
            softly.assertThat(output.errors()).isEqualTo(0);
        });
    }

    @Test
    void should_correctly_count_errors() {
        //given
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aFederatedApi().toBuilder().apiLifecycleState(null).build()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var input = DeleteIngestedApisUseCase.Input.builder().integrationId(INTEGRATION_ID).auditInfo(auditInfo).build();

        //when
        var output = useCase.execute(input);

        //then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(output.deleted()).isEqualTo(0);
            softly.assertThat(output.skipped()).isEqualTo(0);
            softly.assertThat(output.errors()).isEqualTo(1);
        });
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, mode = EnumSource.Mode.INCLUDE, names = { "ACCEPTED", "PAUSED" })
    public void should_close_active_subscriptions(SubscriptionEntity.Status subscriptionStatus) {
        var activeSubscription = SubscriptionFixtures
            .aSubscription()
            .toBuilder()
            .apiId(MY_API)
            .planId("federated")
            .status(subscriptionStatus)
            .build();
        subscriptionCrudService.initWith(List.of(activeSubscription));

        apiCrudServiceInMemory.initWith(
            List.of(ApiFixtures.aFederatedApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.UNPUBLISHED).build())
        );
        planCrudService.initWith(List.of(PlanFixtures.aFederatedPlan()));
        applicationService.initWith(List.of(ApplicationModelFixtures.anApplicationEntity().toBuilder().id("application-id").build()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var input = DeleteIngestedApisUseCase.Input.builder().integrationId(INTEGRATION_ID).auditInfo(auditInfo).build();

        //when
        useCase.execute(input);

        //then
        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch", "createdAt")
            .contains(
                AuditEntity
                    .builder()
                    .id("generated-id")
                    .organizationId(ORGANIZATION_ID)
                    .environmentId(ENVIRONMENT_ID)
                    .referenceType(AuditEntity.AuditReferenceType.API)
                    .referenceId(MY_API)
                    .user(USER_ID)
                    .event(SubscriptionAuditEvent.SUBSCRIPTION_CLOSED.name())
                    .properties(Map.of("APPLICATION", "application-id"))
                    .build()
            );
    }

    @Test
    public void should_delete_all_subscriptions() {
        var api = givenExistingApi(ApiFixtures.aFederatedApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.UNPUBLISHED).build());
        var allSubscriptions = Stream
            .of(SubscriptionEntity.Status.values())
            .map(value -> SubscriptionFixtures.aSubscription().toBuilder().apiId(api.getId()).planId("federated").status(value).build())
            .toList();
        subscriptionCrudService.initWith(allSubscriptions);
        planCrudService.initWith(List.of(PlanFixtures.aFederatedPlan()));
        applicationService.initWith(List.of(ApplicationModelFixtures.anApplicationEntity().toBuilder().id("application-id").build()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var input = DeleteIngestedApisUseCase.Input.builder().integrationId(INTEGRATION_ID).auditInfo(auditInfo).build();

        useCase.execute(input);

        assertThat(subscriptionCrudService.storage()).isNotNull().isEmpty();
    }

    @Test
    public void should_delete_plans() {
        apiCrudServiceInMemory.initWith(
            List.of(ApiFixtures.aFederatedApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.UNPUBLISHED).build())
        );
        planCrudService.initWith(List.of(PlanFixtures.aFederatedPlan()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var input = DeleteIngestedApisUseCase.Input.builder().integrationId(INTEGRATION_ID).auditInfo(auditInfo).build();

        //when
        useCase.execute(input);

        //then
        assertThat(planCrudService.storage()).isNotNull().isEmpty();
    }

    @Test
    public void should_delete_pages() {
        apiCrudServiceInMemory.initWith(
            List.of(ApiFixtures.aFederatedApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.UNPUBLISHED).build())
        );
        pageCrudService.initWith(List.of(PageFixture.aPage()));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var input = DeleteIngestedApisUseCase.Input.builder().integrationId(INTEGRATION_ID).auditInfo(auditInfo).build();

        //when
        useCase.execute(input);

        //then
        assertThat(pageCrudService.storage()).isNotNull().isEmpty();
    }

    @Test
    public void should_create_audit_log() {
        //given
        apiCrudServiceInMemory.initWith(
            List.of(ApiFixtures.aFederatedApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.UNPUBLISHED).build())
        );
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var input = DeleteIngestedApisUseCase.Input.builder().integrationId(INTEGRATION_ID).auditInfo(auditInfo).build();

        //when
        useCase.execute(input);

        //then
        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .contains(
                AuditEntity
                    .builder()
                    .id("generated-id")
                    .organizationId(ORGANIZATION_ID)
                    .environmentId(ENVIRONMENT_ID)
                    .referenceType(AuditEntity.AuditReferenceType.API)
                    .referenceId(MY_API)
                    .user(USER_ID)
                    .event(ApiAuditEvent.API_DELETED.name())
                    .properties(Map.of())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_delete_membership() {
        //given
        apiCrudServiceInMemory.initWith(
            List.of(ApiFixtures.aFederatedApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.UNPUBLISHED).build())
        );
        membershipCrudServiceInMemory.initWith(List.of(MembershipFixtures.anApiMembership(MY_API)));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var input = DeleteIngestedApisUseCase.Input.builder().integrationId(INTEGRATION_ID).auditInfo(auditInfo).build();

        //when
        useCase.execute(input);

        //then
        assertThat(membershipCrudServiceInMemory.storage()).isNotNull().isEmpty();
    }

    @Test
    void should_delete_api_metadata() {
        //given
        apiCrudServiceInMemory.initWith(
            List.of(ApiFixtures.aFederatedApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.UNPUBLISHED).build())
        );
        metadataCrudServiceInMemory.initWith(List.of(MetadataFixtures.anApiMetadata(MY_API)));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var input = DeleteIngestedApisUseCase.Input.builder().integrationId(INTEGRATION_ID).auditInfo(auditInfo).build();

        //when
        useCase.execute(input);

        //then
        assertThat(metadataCrudServiceInMemory.storage()).isNotNull().isEmpty();
    }

    @Test
    void should_delete_api_index() {
        var primaryOwner = PrimaryOwnerEntity.builder().id(USER_ID).type(PrimaryOwnerEntity.Type.USER).build();
        var apiToDelete = ApiFixtures.aFederatedApi().toBuilder().apiLifecycleState(Api.ApiLifecycleState.UNPUBLISHED).build();
        var apiToIndex = new IndexableApi(apiToDelete, primaryOwner, null, null);
        indexer.initWith(List.of(apiToIndex));
        apiCrudServiceInMemory.initWith(List.of(apiToDelete));
        var auditInfo = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
        var input = DeleteIngestedApisUseCase.Input.builder().integrationId(INTEGRATION_ID).auditInfo(auditInfo).build();

        useCase.execute(input);

        assertThat(indexer.storage()).isNotNull().isEmpty();
    }

    private void initializePrimaryOwnerData() {
        roleQueryServiceInMemory.initWith(
            List.of(
                Role
                    .builder()
                    .id("role-id")
                    .scope(Role.Scope.API)
                    .referenceType(Role.ReferenceType.ORGANIZATION)
                    .referenceId(ORGANIZATION_ID)
                    .name("PRIMARY_OWNER")
                    .build()
            )
        );
        membershipCrudServiceInMemory.initWith(
            List.of(
                Membership
                    .builder()
                    .id("member-id")
                    .memberId("my-member-id")
                    .memberType(Membership.Type.USER)
                    .referenceType(Membership.ReferenceType.API)
                    .referenceId(MY_API)
                    .roleId("role-id")
                    .build()
            )
        );
        userCrudService.initWith(List.of(BaseUserEntity.builder().id("my-member-id").email("one_valid@email.com").build()));
    }

    private Api givenExistingApi(Api api) {
        apiCrudServiceInMemory.initWith(List.of(api));
        return api;
    }
}
