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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.ApiKeyFixtures;
import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.IntegrationAgentInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory.ApplicationNotification;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApplicationHookContext;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.domain_service.RejectSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.use_case.CloseSubscriptionUseCase.Input;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CloseSubscriptionUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String USER_ID = "user-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final String SUBSCRIPTION_ID = "subscription-id";
    private static final String APPLICATION_ID = "application-id";

    private final SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudServiceInMemory = new AuditCrudServiceInMemory();
    private final TriggerNotificationDomainServiceInMemory triggerNotificationService = new TriggerNotificationDomainServiceInMemory();
    private final ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ApiKeyCrudServiceInMemory apiKeyCrudService = new ApiKeyCrudServiceInMemory();
    private final PlanCrudServiceInMemory planCrudService = new PlanCrudServiceInMemory();
    private final IntegrationAgentInMemory integrationAgent = new IntegrationAgentInMemory();
    private CloseSubscriptionUseCase usecase;

    @BeforeEach
    void setUp() {
        UuidString.overrideGenerator(() -> "audit-id");

        var userCrudService = new UserCrudServiceInMemory();
        var auditDomainService = new AuditDomainService(auditCrudServiceInMemory, userCrudService, new JacksonJsonDiffProcessor());

        var rejectSubscriptionDomainService = new RejectSubscriptionDomainService(
            subscriptionCrudService,
            auditDomainService,
            new TriggerNotificationDomainServiceInMemory(),
            userCrudService
        );
        var revokeApiKeyDomainService = new RevokeApiKeyDomainService(
            apiKeyCrudService,
            new ApiKeyQueryServiceInMemory(apiKeyCrudService),
            subscriptionCrudService,
            auditDomainService,
            triggerNotificationService
        );

        usecase =
            new CloseSubscriptionUseCase(
                subscriptionCrudService,
                new CloseSubscriptionDomainService(
                    subscriptionCrudService,
                    applicationCrudService,
                    auditDomainService,
                    triggerNotificationService,
                    rejectSubscriptionDomainService,
                    revokeApiKeyDomainService,
                    apiCrudService,
                    integrationAgent
                )
            );
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(
                apiCrudService,
                subscriptionCrudService,
                auditCrudServiceInMemory,
                applicationCrudService,
                apiKeyCrudService,
                planCrudService
            )
            .forEach(InMemoryAlternative::reset);
        triggerNotificationService.reset();
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    @Test
    void should_throw_when_closing_subscription_not_matching_api() {
        // Given no subscription
        var subscription = givenExistingSubscription(SubscriptionFixtures.aSubscription());

        // When
        Throwable throwable = catchThrowable(() ->
            usecase.execute(Input.builder().subscriptionId(subscription.getId()).apiId("another-api").auditInfo(AUDIT_INFO).build())
        );

        // Then
        assertThat(throwable).isInstanceOf(SubscriptionNotFoundException.class).hasMessageContaining(subscription.getId());
    }

    @Test
    void should_throw_when_closing_subscription_not_matching_application() {
        // Given no subscription
        var subscription = givenExistingSubscription(SubscriptionFixtures.aSubscription());

        // When
        Throwable throwable = catchThrowable(() ->
            usecase.execute(
                Input.builder().subscriptionId(subscription.getId()).applicationId("another-application").auditInfo(AUDIT_INFO).build()
            )
        );

        // Then
        assertThat(throwable).isInstanceOf(SubscriptionNotFoundException.class).hasMessageContaining(subscription.getId());
    }

    @Test
    void should_throw_when_close_unknown_subscription() {
        // Given no subscription

        // When
        Throwable throwable = catchThrowable(() -> usecase.execute(new Input("unknown", AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(SubscriptionNotFoundException.class).hasMessageContaining("unknown");
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, names = { "CLOSED", "REJECTED" })
    void should_do_nothing_if_subscription_already_closed(SubscriptionEntity.Status status) {
        // Given
        var api = givenExistingApi(ApiFixtures.aProxyApiV4());
        var subscription = givenExistingSubscription(
            SubscriptionFixtures.aSubscription().toBuilder().id(SUBSCRIPTION_ID).apiId(api.getId()).status(status).build()
        );

        // When
        var result = usecase.execute(new Input(subscription.getId(), AUDIT_INFO));

        // Then
        assertThat(result.subscription()).isSameAs(subscription).extracting(SubscriptionEntity::getStatus).isEqualTo(status);
    }

    @Test
    void should_reject_pending_subscription() {
        // Given
        var api = givenExistingApi(ApiFixtures.aProxyApiV4());
        var plan = givenExistingPlan(PlanFixtures.aPlanHttpV4().toBuilder().id("plan-id").build().setPlanStatus(PlanStatus.PUBLISHED));
        givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID)
                .apiId(api.getId())
                .planId(plan.getId())
                .status(SubscriptionEntity.Status.PENDING)
                .build()
        );

        // When
        var result = usecase.execute(new Input(SUBSCRIPTION_ID, AUDIT_INFO));

        // Then
        assertThat(result.subscription())
            .extracting(SubscriptionEntity::getId, SubscriptionEntity::getStatus)
            .containsExactly(SUBSCRIPTION_ID, SubscriptionEntity.Status.REJECTED);
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, names = { "ACCEPTED", "PAUSED" })
    void should_close_accepted_or_paused_subscription(SubscriptionEntity.Status status) {
        // Given
        var api = givenExistingApi(ApiFixtures.aProxyApiV4());
        var application = givenExistingApplication(BaseApplicationEntity.builder().id(APPLICATION_ID).build());
        givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID)
                .apiId(api.getId())
                .applicationId(application.getId())
                .status(status)
                .build()
        );

        // When
        var result = usecase.execute(new Input(SUBSCRIPTION_ID, AUDIT_INFO));

        // Then
        assertThat(result.subscription())
            .extracting(SubscriptionEntity::getId, SubscriptionEntity::getStatus)
            .containsExactly(SUBSCRIPTION_ID, SubscriptionEntity.Status.CLOSED);
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, names = { "ACCEPTED", "PAUSED" })
    void should_trigger_api_and_application_notification(SubscriptionEntity.Status status) {
        // Given
        var api = givenExistingApi(ApiFixtures.aProxyApiV4());
        var application = givenExistingApplication(BaseApplicationEntity.builder().id(APPLICATION_ID).build());
        givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID)
                .applicationId(application.getId())
                .apiId(api.getId())
                .planId("plan-id")
                .status(status)
                .build()
        );

        // When
        usecase.execute(new Input(SUBSCRIPTION_ID, AUDIT_INFO));

        // Then
        assertThat(triggerNotificationService.getApiNotifications())
            .containsExactly(new SubscriptionClosedApiHookContext(api.getId(), APPLICATION_ID, "plan-id"));
        assertThat(triggerNotificationService.getApplicationNotifications())
            .containsExactly(
                new ApplicationNotification(new SubscriptionClosedApplicationHookContext(APPLICATION_ID, api.getId(), "plan-id"))
            );
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, names = { "ACCEPTED", "PAUSED" })
    void should_create_audit_for_api_and_application(SubscriptionEntity.Status status) {
        // Given
        var api = givenExistingApi(ApiFixtures.aProxyApiV4());
        var application = givenExistingApplication(BaseApplicationEntity.builder().id(APPLICATION_ID).build());
        givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID)
                .apiId(api.getId())
                .applicationId(application.getId())
                .status(status)
                .build()
        );

        // When
        usecase.execute(new Input(SUBSCRIPTION_ID, AUDIT_INFO));

        // Then
        assertThat(auditCrudServiceInMemory.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "patch")
            .containsExactly(
                new AuditEntity(
                    "audit-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    AuditEntity.AuditReferenceType.API,
                    api.getId(),
                    USER_ID,
                    Map.of("APPLICATION", APPLICATION_ID),
                    SubscriptionAuditEvent.SUBSCRIPTION_CLOSED.name(),
                    ZonedDateTime.now(),
                    ""
                ),
                new AuditEntity(
                    "audit-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    AuditEntity.AuditReferenceType.APPLICATION,
                    APPLICATION_ID,
                    USER_ID,
                    Map.of("API", api.getId()),
                    SubscriptionAuditEvent.SUBSCRIPTION_CLOSED.name(),
                    ZonedDateTime.now(),
                    ""
                )
            );
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, names = { "ACCEPTED", "PAUSED" })
    void should_not_revoke_keys_for_application_in_shared_api_key_mode(SubscriptionEntity.Status status) {
        // Given
        var api = givenExistingApi(ApiFixtures.aProxyApiV4());
        var application = givenExistingApplication(
            BaseApplicationEntity.builder().id(APPLICATION_ID).apiKeyMode(ApiKeyMode.SHARED).build()
        );
        var subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID)
                .apiId(api.getId())
                .applicationId(application.getId())
                .status(status)
                .build()
        );
        givenExistingApiKeysForSubscription(
            List.of(
                ApiKeyFixtures
                    .anApiKey()
                    .toBuilder()
                    .id("api-key-id")
                    .key("api-key")
                    .applicationId(application.getId())
                    .subscriptions(List.of(subscription.getId()))
                    .revoked(false)
                    .expireAt(Instant.now().plus(1, ChronoUnit.DAYS).atZone(ZoneId.systemDefault()))
                    .build()
            )
        );

        // When
        usecase.execute(new Input(SUBSCRIPTION_ID, AUDIT_INFO));

        // Then
        var revokedKeys = apiKeyCrudService.storage().stream().filter(ApiKeyEntity::isRevoked).toList();
        assertThat(revokedKeys).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = SubscriptionEntity.Status.class, names = { "ACCEPTED", "PAUSED" })
    void should_close_federated_subscription(SubscriptionEntity.Status status) {
        // Given
        var api = givenExistingApi(ApiFixtures.aFederatedApi());
        var application = givenExistingApplication(
            BaseApplicationEntity.builder().id(APPLICATION_ID).apiKeyMode(ApiKeyMode.EXCLUSIVE).build()
        );
        var subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID)
                .apiId(api.getId())
                .applicationId(application.getId())
                .status(status)
                .metadata(Map.of("api-key-provider-id", "value"))
                .build()
        );
        givenExistingApiKeysForSubscription(
            List.of(
                ApiKeyFixtures
                    .anApiKey()
                    .toBuilder()
                    .id("api-key-id")
                    .key("api-key")
                    .applicationId(application.getId())
                    .subscriptions(List.of(subscription.getId()))
                    .revoked(false)
                    .expireAt(Instant.now().plus(1, ChronoUnit.DAYS).atZone(ZoneId.systemDefault()))
                    .build()
            )
        );

        // When
        usecase.execute(new Input(SUBSCRIPTION_ID, AUDIT_INFO));

        // Then
        assertThat(integrationAgent.closedSubscriptions("integration-id")).containsExactly(subscription);
    }

    private Api givenExistingApi(Api api) {
        apiCrudService.initWith(List.of(api));
        return api;
    }

    private void givenExistingApiKeysForSubscription(List<ApiKeyEntity> apiKeys) {
        apiKeyCrudService.initWith(apiKeys);
    }

    private BaseApplicationEntity givenExistingApplication(BaseApplicationEntity application) {
        applicationCrudService.initWith(List.of(application));
        return application;
    }

    private SubscriptionEntity givenExistingSubscription(SubscriptionEntity subscription) {
        subscriptionCrudService.initWith(List.of(subscription));
        return subscription;
    }

    private Plan givenExistingPlan(Plan plan) {
        planCrudService.initWith(List.of(plan));
        return plan;
    }
}
