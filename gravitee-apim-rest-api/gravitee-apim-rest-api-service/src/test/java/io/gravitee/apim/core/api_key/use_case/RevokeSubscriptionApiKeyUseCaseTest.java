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
package io.gravitee.apim.core.api_key.use_case;

import static fixtures.ApplicationModelFixtures.anApplicationEntity;
import static fixtures.core.model.ApiKeyFixtures.anApiKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApiKeyQueryServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.SubscriptionCrudServiceInMemory;
import inmemory.TriggerNotificationDomainServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.use_case.RevokeSubscriptionApiKeyUseCase.Input;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiKeyAuditEvent;
import io.gravitee.apim.core.notification.model.hook.ApiKeyRevokedApiHookContext;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiKeyNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationApiKeyModeException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RevokeSubscriptionApiKeyUseCaseTest {

    private static final String USER_ID = "user-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final String KEY = "my-key";
    private static final String APPLICATION_ID = "application-id";
    private static final String SUBSCRIPTION_ID = "subscription-id";
    private static final String PLAN_1 = "plan-1";
    private static final String API_ID = "api-id";

    ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    ApiKeyCrudServiceInMemory apiKeyCrudService = new ApiKeyCrudServiceInMemory();
    SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    ApiKeyQueryServiceInMemory apiKeyQueryService = new ApiKeyQueryServiceInMemory(apiKeyCrudService, subscriptionCrudService);
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainService = new TriggerNotificationDomainServiceInMemory();
    RevokeSubscriptionApiKeyUseCase usecase;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "new-id");
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
    }

    @BeforeEach
    void setUp() {
        var auditDomainService = new AuditDomainService(auditCrudService, new UserCrudServiceInMemory(), new JacksonJsonDiffProcessor());
        var revokeApiKeyDomainService = new RevokeApiKeyDomainService(
            apiKeyCrudService,
            apiKeyQueryService,
            subscriptionCrudService,
            auditDomainService,
            triggerNotificationDomainService
        );
        usecase = new RevokeSubscriptionApiKeyUseCase(
            subscriptionCrudService,
            applicationCrudService,
            apiKeyQueryService,
            revokeApiKeyDomainService
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiKeyCrudService, applicationCrudService, auditCrudService, subscriptionCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_throw_when_subscription_does_not_exists() {
        // Given no subscription

        // When
        var throwable = catchThrowable(() -> usecase.execute(new Input(SUBSCRIPTION_ID, KEY, API_ID, "API", AUDIT_INFO)));

        // Then
        assertThat(throwable)
            .isInstanceOf(SubscriptionNotFoundException.class)
            .hasMessage("Subscription [%s] cannot be found.", SUBSCRIPTION_ID);
    }

    @Test
    void should_throw_when_api_key_does_not_exists() {
        // Given
        givenASubscription();

        // When
        var throwable = catchThrowable(() -> usecase.execute(new Input(SUBSCRIPTION_ID, KEY, API_ID, "API", AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(ApiKeyNotFoundException.class).hasMessage("No API Key can be found.");
    }

    @Test
    void should_throw_when_api_key_does_not_belong_to_the_right_subscription() {
        // Given
        givenASubscription();
        givenAnApiKey(anApiKey().toBuilder().key(KEY).subscriptions(List.of("another-subscription")).build());

        // When
        var throwable = catchThrowable(() -> usecase.execute(new Input(SUBSCRIPTION_ID, KEY, API_ID, "API", AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(ApiKeyNotFoundException.class).hasMessage("No API Key can be found.");
    }

    @Test
    void should_throw_when_application_does_not_exists() {
        // Given
        var subscription = givenASubscription();
        givenAnApiKey(anApiKey().toBuilder().key(KEY).subscriptions(List.of(subscription.getId())).build());

        // When
        var throwable = catchThrowable(() -> usecase.execute(new Input(SUBSCRIPTION_ID, KEY, API_ID, "API", AUDIT_INFO)));

        // Then
        assertThat(throwable)
            .isInstanceOf(ApplicationNotFoundException.class)
            .hasMessage("Application [%s] cannot be found.", APPLICATION_ID);
    }

    @Test
    void should_throw_when_application_uses_shared_api_key() {
        // Given
        var application = givenAnApplication(anApplicationEntity().toBuilder().id(APPLICATION_ID).apiKeyMode(ApiKeyMode.SHARED).build());
        var subscription = givenASubscription();
        givenAnApiKey(
            anApiKey().toBuilder().key(KEY).applicationId(application.getId()).subscriptions(List.of(subscription.getId())).build()
        );

        // When
        var throwable = catchThrowable(() -> usecase.execute(new Input(SUBSCRIPTION_ID, KEY, API_ID, "API", AUDIT_INFO)));

        // Then
        assertThat(throwable)
            .isInstanceOf(InvalidApplicationApiKeyModeException.class)
            .hasMessage(String.format("Invalid operation for API Key mode [SHARED] of application [%s]", application.getId()));
    }

    @ParameterizedTest
    @EnumSource(value = ApiKeyMode.class, names = { "EXCLUSIVE", "UNSPECIFIED" }, mode = EnumSource.Mode.INCLUDE)
    void should_revoke_api_key(ApiKeyMode apiKeyMode) {
        // Given
        var now = ZonedDateTime.now();
        var application = givenAnApplication(anApplicationEntity().toBuilder().id(APPLICATION_ID).apiKeyMode(apiKeyMode).build());
        var subscription = givenASubscription();
        givenAnApiKey(
            anApiKey().toBuilder().key(KEY).applicationId(application.getId()).subscriptions(List.of(subscription.getId())).build()
        );

        // When
        var result = usecase.execute(new Input(SUBSCRIPTION_ID, KEY, API_ID, "API", AUDIT_INFO));

        // Then
        SoftAssertions.assertSoftly(soft -> {
            var revoked = result.apiKey();
            soft.assertThat(revoked.isRevoked()).describedAs("ApiKey is not revoked").isTrue();
            soft.assertThat(revoked.getRevokedAt()).isEqualTo(revoked.getUpdatedAt());
            soft.assertThat(revoked.getRevokedAt()).isAfterOrEqualTo(now);
        });
    }

    @Test
    void should_create_an_audit_log() {
        // Given
        var application = givenAnApplication();
        var subscription = givenASubscription();
        var apiKey = givenAnApiKey(
            anApiKey().toBuilder().key(KEY).applicationId(application.getId()).subscriptions(List.of(subscription.getId())).build()
        );

        // When
        var result = usecase.execute(new Input(SUBSCRIPTION_ID, KEY, API_ID, "API", AUDIT_INFO));

        // Then
        assertThat(auditCrudService.storage())
            .hasSize(1)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .contains(
                new AuditEntity(
                    "new-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    AuditEntity.AuditReferenceType.API,
                    API_ID,
                    USER_ID,
                    Map.of("API_KEY", apiKey.getKey(), "API", API_ID, "APPLICATION", APPLICATION_ID),
                    ApiKeyAuditEvent.APIKEY_REVOKED.name(),
                    result.apiKey().getRevokedAt(),
                    ""
                )
            );
    }

    @Test
    void should_trigger_an_api_notification() {
        // Given
        var application = givenAnApplication();
        var subscription = givenASubscription();
        var apiKey = givenAnApiKey(
            anApiKey().toBuilder().key(KEY).applicationId(application.getId()).subscriptions(List.of(subscription.getId())).build()
        );

        // When
        usecase.execute(new Input(SUBSCRIPTION_ID, KEY, API_ID, "API", AUDIT_INFO));

        // Then
        assertThat(triggerNotificationDomainService.getApiNotifications()).containsExactly(
            new ApiKeyRevokedApiHookContext(API_ID, APPLICATION_ID, PLAN_1, apiKey.getKey())
        );
    }

    private SubscriptionEntity givenASubscription() {
        return givenASubscription(
            SubscriptionFixtures.aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID)
                .apiId(API_ID)
                .applicationId(APPLICATION_ID)
                .planId(PLAN_1)
                .build()
        );
    }

    private SubscriptionEntity givenASubscription(SubscriptionEntity subscription) {
        subscriptionCrudService.initWith(List.of(subscription));
        return subscription;
    }

    private BaseApplicationEntity givenAnApplication() {
        return givenAnApplication(anApplicationEntity().toBuilder().id(APPLICATION_ID).apiKeyMode(ApiKeyMode.EXCLUSIVE).build());
    }

    private BaseApplicationEntity givenAnApplication(BaseApplicationEntity application) {
        applicationCrudService.initWith(List.of(application));
        return application;
    }

    private ApiKeyEntity givenAnApiKey(ApiKeyEntity apiKey) {
        apiKeyCrudService.initWith(List.of(apiKey));
        return apiKey;
    }
}
