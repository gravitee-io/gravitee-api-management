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
package io.gravitee.apim.core.subscription.use_case;

import static fixtures.ApplicationModelFixtures.anApplicationEntity;
import static fixtures.core.model.MembershipFixtures.anApplicationPrimaryOwnerUserMembership;
import static io.gravitee.apim.core.subscription.domain_service.AcceptSubscriptionDomainService.REJECT_BY_TECHNICAL_ERROR_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import fixtures.ApplicationModelFixtures;
import fixtures.core.model.ApiFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IntegrationAgentInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_key.domain_service.GenerateApiKeyDomainService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.integration.exception.IntegrationSubscriptionException;
import io.gravitee.apim.core.integration.model.IntegrationSubscription;
import io.gravitee.apim.core.membership.domain_service.ApplicationPrimaryOwnerDomainService;
import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.apim.core.notification.model.hook.SubscriptionAcceptedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionAcceptedApplicationHookContext;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.domain_service.AcceptSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import io.reactivex.rxjava3.core.Single;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class AcceptSubscriptionUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String API_ID = "api-id";
    private static final String USER_ID = "user-id";
    private static final String APPLICATION_ID = "my-application";

    private static final ZonedDateTime STARTING_AT = Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault());
    private static final ZonedDateTime ENDING_AT = Instant.parse("2024-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault());
    private static final String REASON = "Subscription accepted";

    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);

    SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();

    AuditCrudServiceInMemory auditCrudServiceInMemory = new AuditCrudServiceInMemory();
    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainService = new TriggerNotificationDomainServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    ApiKeyCrudServiceInMemory apiKeyCrudService = new ApiKeyCrudServiceInMemory();
    ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    IntegrationAgentInMemory integrationAgent = spy(new IntegrationAgentInMemory());
    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    AcceptSubscriptionUseCase useCase;

    @BeforeAll
    static void init() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(auditCrudServiceInMemory, userCrudService, new JacksonJsonDiffProcessor());

        var generateApiKeyDomainService = new GenerateApiKeyDomainService(
            apiKeyCrudService,
            new ApiKeyQueryServiceInMemory(apiKeyCrudService),
            applicationCrudService,
            auditDomainService
        );

        ApplicationPrimaryOwnerDomainService applicationPrimaryOwnerDomainService = new ApplicationPrimaryOwnerDomainService(
            groupQueryService,
            membershipQueryService,
            roleQueryService,
            userCrudService
        );

        var acceptSubscriptionDomainService = new AcceptSubscriptionDomainService(
            subscriptionCrudService,
            auditDomainService,
            apiCrudService,
            applicationCrudService,
            planCrudService,
            generateApiKeyDomainService,
            integrationAgent,
            triggerNotificationDomainService,
            userCrudService,
            applicationPrimaryOwnerDomainService
        );

        useCase = new AcceptSubscriptionUseCase(subscriptionCrudService, planCrudService, acceptSubscriptionDomainService);

        membershipQueryService.initWith(List.of(anApplicationPrimaryOwnerUserMembership(APPLICATION_ID, USER_ID, ORGANIZATION_ID)));
        applicationCrudService.initWith(
            List.of(
                ApplicationModelFixtures
                    .anApplicationEntity()
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
        Stream
            .of(
                apiCrudService,
                apiKeyCrudService,
                applicationCrudService,
                auditCrudServiceInMemory,
                integrationAgent,
                planCrudService,
                subscriptionCrudService,
                userCrudService
            )
            .forEach(InMemoryAlternative::reset);
        triggerNotificationDomainService.reset();
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_accept_subscription() {
        // Given
        var api = givenExistingApi(ApiFixtures.aProxyApiV4().setId(API_ID));
        var plan = givenExistingPlan(PlanFixtures.anApiKeyV4().setPlanStatus(PlanStatus.PUBLISHED));
        var application = givenExistingApplication();
        var subscription = givenExistingPendingSubscriptionFor(api, plan, application);

        // When
        var result = accept(subscription.getId());

        // Then
        assertThat(result.subscription())
            .extracting(
                SubscriptionEntity::getId,
                SubscriptionEntity::getStatus,
                SubscriptionEntity::getReasonMessage,
                SubscriptionEntity::getStartingAt,
                SubscriptionEntity::getEndingAt
            )
            .contains(subscription.getId(), SubscriptionEntity.Status.ACCEPTED, REASON, STARTING_AT, ENDING_AT);
    }

    @Test
    void should_accept_subscription_without_validity_period() {
        // Given
        var api = givenExistingApi(ApiFixtures.aProxyApiV4().setId(API_ID));
        var plan = givenExistingPlan(PlanFixtures.anApiKeyV4().setPlanStatus(PlanStatus.PUBLISHED));
        var application = givenExistingApplication();
        var subscription = givenExistingPendingSubscriptionFor(api, plan, application);

        // When
        var result = accept(subscription.getId(), null, null);

        // Then
        assertThat(result.subscription())
            .extracting(
                SubscriptionEntity::getId,
                SubscriptionEntity::getStatus,
                SubscriptionEntity::getReasonMessage,
                SubscriptionEntity::getStartingAt,
                SubscriptionEntity::getEndingAt
            )
            .contains(subscription.getId(), SubscriptionEntity.Status.ACCEPTED, REASON, INSTANT_NOW.atZone(ZoneId.systemDefault()), null);
    }

    @Test
    void should_accept_subscription_with_custom_key() {
        // Given
        var api = givenExistingApi(ApiFixtures.aProxyApiV4().setId(API_ID));
        var plan = givenExistingPlan(PlanFixtures.anApiKeyV4().setPlanStatus(PlanStatus.PUBLISHED));
        var application = givenExistingApplication();
        var subscription = givenExistingPendingSubscriptionFor(api, plan, application);

        // When
        accept(subscription.getId(), STARTING_AT, ENDING_AT, "custom_key");

        // Then
        assertThat(apiKeyCrudService.storage())
            .containsOnly(
                ApiKeyEntity
                    .builder()
                    .id("generated-id")
                    .applicationId(subscription.getApplicationId())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .key("custom_key")
                    .subscriptions(List.of(subscription.getId()))
                    .expireAt(ENDING_AT)
                    .build()
            );
    }

    @Test
    void should_keep_integration_metadata_when_accepting_federated_subscription() {
        // Given
        var api = givenExistingApi(ApiFixtures.aFederatedApi().setId(API_ID));
        var plan = givenExistingPlan(PlanFixtures.aFederatedPlan().setPlanStatus(PlanStatus.PUBLISHED));
        var application = givenExistingApplication();
        var subscription = givenExistingPendingSubscriptionFor(api, plan, application);

        // When
        var result = accept(subscription.getId());

        // Then
        assertThat(result.subscription())
            .extracting(
                SubscriptionEntity::getId,
                SubscriptionEntity::getStatus,
                SubscriptionEntity::getReasonMessage,
                SubscriptionEntity::getStartingAt,
                SubscriptionEntity::getEndingAt,
                SubscriptionEntity::getMetadata
            )
            .contains(
                subscription.getId(),
                SubscriptionEntity.Status.ACCEPTED,
                null,
                INSTANT_NOW.atZone(ZoneId.systemDefault()),
                null,
                Map.of("key", "value")
            );
    }

    @Test
    void should_reject_subscription_when_integration_processing_fails_on_federated_api() {
        // Given
        var api = givenExistingApi(ApiFixtures.aFederatedApi().setId(API_ID));
        var plan = givenExistingPlan(PlanFixtures.aFederatedPlan().setPlanStatus(PlanStatus.PUBLISHED));
        var application = givenExistingApplication();
        var subscription = givenExistingPendingSubscriptionFor(api, plan, application);

        doReturn(Single.error(new IntegrationSubscriptionException("fail to subscribe")))
            .when(integrationAgent)
            .subscribe(any(), any(), any(), any(), any());

        // When
        accept(subscription.getId());

        // Then
        assertThat(subscriptionCrudService.storage())
            .extracting(
                SubscriptionEntity::getId,
                SubscriptionEntity::getStatus,
                SubscriptionEntity::getReasonMessage,
                SubscriptionEntity::getClosedAt
            )
            .contains(
                tuple(
                    subscription.getId(),
                    SubscriptionEntity.Status.REJECTED,
                    REJECT_BY_TECHNICAL_ERROR_MESSAGE,
                    INSTANT_NOW.atZone(ZoneId.systemDefault())
                )
            );
    }

    @ParameterizedTest
    @CsvSource({ "V4, PROXY", "V4, MESSAGE", "V2, PROXY", "FEDERATED, PROXY" })
    void should_create_audits(DefinitionVersion definitionVersion, ApiType apiType) {
        // Given
        var api = givenExistingApiOf(definitionVersion, apiType);
        var plan = givenExistingPublishedPlanFor(api);
        var application = givenExistingApplication();
        var subscription = givenExistingPendingSubscriptionFor(api, plan, application);

        // When
        accept(subscription.getId());

        // Then
        assertThat(auditCrudServiceInMemory.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "patch")
            .contains(
                new AuditEntity(
                    "generated-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    AuditEntity.AuditReferenceType.API,
                    "api-id",
                    USER_ID,
                    Map.of("APPLICATION", application.getId()),
                    SubscriptionAuditEvent.SUBSCRIPTION_UPDATED.name(),
                    ZonedDateTime.now(),
                    ""
                ),
                new AuditEntity(
                    "generated-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    AuditEntity.AuditReferenceType.APPLICATION,
                    application.getId(),
                    USER_ID,
                    Map.of("API", "api-id"),
                    SubscriptionAuditEvent.SUBSCRIPTION_UPDATED.name(),
                    ZonedDateTime.now(),
                    ""
                )
            );
    }

    @Test
    void should_generated_key_for_API_Key_plan() {
        // Given
        var api = givenExistingApi(ApiFixtures.aProxyApiV4().setId(API_ID));
        var plan = givenExistingPlan(PlanFixtures.anApiKeyV4().setPlanStatus(PlanStatus.PUBLISHED));
        var application = givenExistingApplication();
        var subscription = givenExistingPendingSubscriptionFor(api, plan, application);

        // When
        accept(subscription.getId());

        // Then
        assertThat(apiKeyCrudService.storage())
            .containsOnly(
                ApiKeyEntity
                    .builder()
                    .id("generated-id")
                    .applicationId(subscription.getApplicationId())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .key("generated-id")
                    .subscriptions(List.of(subscription.getId()))
                    .expireAt(ENDING_AT)
                    .build()
            );
    }

    @Test
    void should_create_API_key_from_integration_when_federated_api() {
        // Given
        var api = givenExistingApi(ApiFixtures.aFederatedApi().setId(API_ID));
        var plan = givenExistingPlan(PlanFixtures.aFederatedPlan().setPlanStatus(PlanStatus.PUBLISHED));
        var application = givenExistingApplication();
        var subscription = givenExistingPendingSubscriptionFor(api, plan, application);

        // When
        accept(subscription.getId());

        // Then
        assertThat(apiKeyCrudService.storage())
            .containsOnly(
                ApiKeyEntity
                    .builder()
                    .id("generated-id")
                    .applicationId(subscription.getApplicationId())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .key(
                        String.join(
                            "-",
                            IntegrationSubscription.Type.API_KEY.name(),
                            subscription.getId(),
                            application.getId(),
                            application.getName()
                        )
                    )
                    .subscriptions(List.of(subscription.getId()))
                    .expireAt(null)
                    .federated(true)
                    .build()
            );
    }

    @Test
    void should_not_generated_key_for_not_API_Key_plan() {
        // Given
        var api = givenExistingApi(ApiFixtures.aProxyApiV4().setId(API_ID));
        var plan = givenExistingPlan(PlanFixtures.aPushPlan().setPlanStatus(PlanStatus.PUBLISHED));
        var application = givenExistingApplication();
        var subscription = givenExistingPendingSubscriptionFor(api, plan, application);

        // When
        accept(subscription.getId());

        // Then
        assertThat(apiKeyCrudService.storage()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "V4, PROXY", "V4, MESSAGE", "V2, PROXY", "FEDERATED, PROXY" })
    void should_trigger_notifications_for_API_and_Application_owners(DefinitionVersion definitionVersion, ApiType apiType) {
        // Given
        var application = givenExistingApplication();
        var api = givenExistingApiOf(definitionVersion, apiType);
        var plan = givenExistingPublishedPlanFor(api);
        var subscription = givenExistingPendingSubscriptionFor(api, plan, application);

        // When
        accept(subscription.getId());

        // Then
        assertThat(triggerNotificationDomainService.getApiNotifications())
            .containsExactly(
                new SubscriptionAcceptedApiHookContext("api-id", application.getId(), plan.getId(), subscription.getId(), null)
            );

        assertThat(triggerNotificationDomainService.getApplicationNotifications())
            .containsExactly(
                new TriggerNotificationDomainServiceInMemory.ApplicationNotification(
                    new SubscriptionAcceptedApplicationHookContext(
                        application.getId(),
                        "api-id",
                        plan.getId(),
                        subscription.getId(),
                        USER_ID
                    )
                )
            );
    }

    @ParameterizedTest
    @CsvSource({ "V4, PROXY", "V4, MESSAGE", "V2, PROXY", "FEDERATED, PROXY" })
    void should_trigger_notifications_for_subscriber_when_it_has_email(DefinitionVersion definitionVersion, ApiType apiType) {
        // Given
        var api = givenExistingApiOf(definitionVersion, apiType);
        var plan = givenExistingPublishedPlanFor(api);
        var application = givenExistingApplication();
        var subscriber = givenExistingUser(BaseUserEntity.builder().id("subscriber").email("subscriber@mail.fake").build());
        var subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .subscribedBy(subscriber.getId())
                .apiId(api.getId())
                .planId(plan.getId())
                .applicationId(application.getId())
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );

        // When
        accept(subscription.getId());

        // Then
        assertThat(triggerNotificationDomainService.getApplicationNotifications())
            .contains(
                new TriggerNotificationDomainServiceInMemory.ApplicationNotification(
                    new Recipient("EMAIL", "subscriber@mail.fake"),
                    new SubscriptionAcceptedApplicationHookContext(
                        application.getId(),
                        "api-id",
                        plan.getId(),
                        subscription.getId(),
                        USER_ID
                    )
                )
            );
    }

    @Test
    void should_throw_when_subscription_does_not_exists() {
        // When
        var throwable = catchThrowable(() -> accept("unknown"));

        // Then
        assertThat(throwable).isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void should_throw_when_subscription_does_not_belong_to_API() {
        // Given
        var subscription = givenExistingSubscription(SubscriptionFixtures.aSubscription().toBuilder().apiId("other-id").build());

        // When
        var throwable = catchThrowable(() -> accept(subscription.getId()));

        // Then
        assertThat(throwable).isInstanceOf(SubscriptionNotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING")
    void should_throw_when_status_not_pending(SubscriptionEntity.Status status) {
        // Given
        var application = givenExistingApplication();
        var plan = givenExistingPlan(PlanFixtures.anApiKeyV4().setPlanStatus(PlanStatus.PUBLISHED));
        var subscription = givenExistingSubscription(
            SubscriptionFixtures.aSubscription().toBuilder().planId(plan.getId()).applicationId(application.getId()).status(status).build()
        );

        // When
        var throwable = catchThrowable(() -> accept(subscription.getId()));

        // Then
        assertThat(throwable).isInstanceOf(IllegalStateException.class).hasMessage("Cannot accept subscription");
    }

    @Test
    void should_throw_when_plan_is_closed() {
        // Given
        var application = givenExistingApplication();
        var plan = givenExistingPlan(PlanFixtures.anApiKeyV4().setPlanStatus(PlanStatus.CLOSED));
        var subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .planId(plan.getId())
                .applicationId(application.getId())
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );

        // When
        var throwable = catchThrowable(() -> accept(subscription.getId()));

        // Then
        assertThat(throwable).isInstanceOf(PlanAlreadyClosedException.class);
    }

    private BaseApplicationEntity givenExistingApplication() {
        var application = anApplicationEntity();
        applicationCrudService.initWith(List.of(application));
        return application;
    }

    private Api givenExistingApi(Api api) {
        apiCrudService.initWith(List.of(api));
        return api;
    }

    private Api givenExistingApiOf(DefinitionVersion definitionVersion, ApiType apiType) {
        var api = anApi(definitionVersion, apiType);
        apiCrudService.initWith(List.of(api));
        return api;
    }

    private Api anApi(DefinitionVersion definitionVersion, ApiType apiType) {
        return switch (definitionVersion) {
            case V1, V2 -> ApiFixtures.aProxyApiV2().setId(API_ID);
            case V4 -> switch (apiType) {
                case PROXY -> ApiFixtures.aProxyApiV4().setId(API_ID);
                case MESSAGE -> ApiFixtures.aMessageApiV4().setId(API_ID);
            };
            case FEDERATED -> ApiFixtures.aFederatedApi().setId(API_ID);
        };
    }

    private Plan givenExistingPlan(Plan plan) {
        planCrudService.initWith(List.of(plan));
        return plan;
    }

    private Plan givenExistingPublishedPlanFor(Api api) {
        var plan = aPublishedPlan(api);
        planCrudService.initWith(List.of(plan));
        return plan;
    }

    private Plan aPublishedPlan(Api api) {
        return switch (api.getDefinitionVersion()) {
            case V1, V2 -> PlanFixtures.aPlanV2().setPlanStatus(PlanStatus.PUBLISHED);
            case V4 -> switch (api.getType()) {
                case PROXY -> PlanFixtures.anApiKeyV4().setPlanStatus(PlanStatus.PUBLISHED);
                case MESSAGE -> PlanFixtures.aPushPlan().setPlanStatus(PlanStatus.PUBLISHED);
            };
            case FEDERATED -> PlanFixtures.aFederatedPlan().setPlanStatus(PlanStatus.PUBLISHED);
        };
    }

    private BaseUserEntity givenExistingUser(BaseUserEntity user) {
        userCrudService.initWith(List.of(user));
        return user;
    }

    private SubscriptionEntity givenExistingPendingSubscriptionFor(Api api, Plan plan, BaseApplicationEntity application) {
        var subscription = SubscriptionFixtures
            .aSubscription()
            .toBuilder()
            .subscribedBy("subscriber")
            .apiId(api.getId())
            .planId(plan.getId())
            .applicationId(application.getId())
            .status(SubscriptionEntity.Status.PENDING)
            .build();
        subscriptionCrudService.initWith(List.of(subscription));
        return subscription;
    }

    private SubscriptionEntity givenExistingSubscription(SubscriptionEntity subscription) {
        subscriptionCrudService.initWith(List.of(subscription));
        return subscription;
    }

    private AcceptSubscriptionUseCase.Output accept(String subscriptionId) {
        return accept(subscriptionId, STARTING_AT, ENDING_AT);
    }

    private AcceptSubscriptionUseCase.Output accept(String subscriptionId, ZonedDateTime startingAt, ZonedDateTime endingAt) {
        return accept(subscriptionId, startingAt, endingAt, null);
    }

    private AcceptSubscriptionUseCase.Output accept(
        String subscriptionId,
        ZonedDateTime startingAt,
        ZonedDateTime endingAt,
        String customKey
    ) {
        return useCase.execute(
            new AcceptSubscriptionUseCase.Input(API_ID, subscriptionId, startingAt, endingAt, REASON, customKey, AUDIT_INFO)
        );
    }
}
