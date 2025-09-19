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
import io.gravitee.apim.core.api_key.use_case.RevokeApplicationApiKeyUseCase.Input;
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

class RevokeApplicationApiKeyUseCaseTest {

    private static final String USER_ID = "user-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final AuditInfo AUDIT_INFO = AuditInfoFixtures.anAuditInfo(ORGANIZATION_ID, ENVIRONMENT_ID, USER_ID);
    private static final String API_KEY_ID = "api-key-id";
    private static final String APPLICATION_ID = "application-id";
    private static final String SUBSCRIPTION_1 = "subscription-1";
    private static final String SUBSCRIPTION_2 = "subscription-2";
    private static final String PLAN_1 = "plan-1";
    private static final String PLAN_2 = "plan-2";
    private static final String API_1 = "api-1";
    private static final String API_2 = "api-2";

    ApplicationCrudServiceInMemory applicationCrudService = new ApplicationCrudServiceInMemory();
    ApiKeyCrudServiceInMemory apiKeyCrudService = new ApiKeyCrudServiceInMemory();
    ApiKeyQueryServiceInMemory apiKeyQueryService = new ApiKeyQueryServiceInMemory(apiKeyCrudService);
    SubscriptionCrudServiceInMemory subscriptionCrudService = new SubscriptionCrudServiceInMemory();
    AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();
    TriggerNotificationDomainServiceInMemory triggerNotificationDomainService = new TriggerNotificationDomainServiceInMemory();
    RevokeApplicationApiKeyUseCase usecase;

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
        usecase = new RevokeApplicationApiKeyUseCase(applicationCrudService, apiKeyQueryService, revokeApiKeyDomainService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiKeyCrudService, applicationCrudService, auditCrudService, subscriptionCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_throw_when_application_does_not_exists() {
        // Given no application

        // When
        var throwable = catchThrowable(() -> usecase.execute(new Input(API_KEY_ID, APPLICATION_ID, AUDIT_INFO)));

        // Then
        assertThat(throwable)
            .isInstanceOf(ApplicationNotFoundException.class)
            .hasMessage("Application [%s] cannot be found.", APPLICATION_ID);
    }

    @Test
    void should_throw_when_api_key_does_not_exists() {
        // Given
        givenAnApplication();

        // When
        var throwable = catchThrowable(() -> usecase.execute(new Input(API_KEY_ID, APPLICATION_ID, AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(ApiKeyNotFoundException.class).hasMessage("No API Key can be found.");
    }

    @Test
    void should_throw_when_api_key_does_not_belong_to_the_right_application() {
        // Given
        givenAnApplication();
        givenAnApiKey(anApiKey().toBuilder().applicationId("other-application-id").build());

        // When
        var throwable = catchThrowable(() -> usecase.execute(new Input(API_KEY_ID, APPLICATION_ID, AUDIT_INFO)));

        // Then
        assertThat(throwable).isInstanceOf(ApiKeyNotFoundException.class).hasMessage("No API Key can be found.");
    }

    @ParameterizedTest
    @EnumSource(value = ApiKeyMode.class, names = { "SHARED" }, mode = EnumSource.Mode.EXCLUDE)
    void should_throw_when_application_does_not_use_shared_api_key(ApiKeyMode apiKeyMode) {
        // Given
        var application = givenAnApplication(anApplicationEntity().toBuilder().id(APPLICATION_ID).apiKeyMode(apiKeyMode).build());
        givenAnApiKey(anApiKey().toBuilder().applicationId(application.getId()).build());

        // When
        var throwable = catchThrowable(() -> usecase.execute(new Input(API_KEY_ID, APPLICATION_ID, AUDIT_INFO)));

        // Then
        assertThat(throwable)
            .isInstanceOf(InvalidApplicationApiKeyModeException.class)
            .hasMessage(String.format("Invalid operation for API Key mode [%s] of application [%s]", apiKeyMode, application.getId()));
    }

    @Test
    void should_revoke_api_key() {
        // Given
        var now = ZonedDateTime.now();
        var application = givenAnApplication();
        givenAnApiKey(anApiKey().toBuilder().applicationId(application.getId()).subscriptions(List.of()).build());

        // When
        var result = usecase.execute(new Input(API_KEY_ID, APPLICATION_ID, AUDIT_INFO));

        // Then
        SoftAssertions.assertSoftly(soft -> {
            var revoked = result.apiKey();
            soft.assertThat(revoked.isRevoked()).describedAs("ApiKey is not revoked").isTrue();
            soft.assertThat(revoked.getRevokedAt()).isEqualTo(revoked.getUpdatedAt());
            soft.assertThat(revoked.getRevokedAt()).isAfterOrEqualTo(now);
        });
    }

    @Test
    void should_create_an_audit_log_for_each_subscriptions_associated_to_the_api_key() {
        // Given
        var application = givenAnApplication();
        var subscriptions = givenSubscriptions();
        var apiKey = givenAnApiKey(
            anApiKey()
                .toBuilder()
                .applicationId(application.getId())
                .subscriptions(subscriptions.stream().map(SubscriptionEntity::getId).toList())
                .build()
        );

        // When
        var result = usecase.execute(new Input(API_KEY_ID, APPLICATION_ID, AUDIT_INFO));

        // Then
        assertThat(auditCrudService.storage())
            .hasSize(2)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .contains(
                new AuditEntity(
                    "new-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    AuditEntity.AuditReferenceType.API,
                    API_2,
                    USER_ID,
                    Map.of("API_KEY", apiKey.getKey(), "API", API_2, "APPLICATION", APPLICATION_ID),
                    ApiKeyAuditEvent.APIKEY_REVOKED.name(),
                    result.apiKey().getRevokedAt(),
                    ""
                ),
                new AuditEntity(
                    "new-id",
                    ORGANIZATION_ID,
                    ENVIRONMENT_ID,
                    AuditEntity.AuditReferenceType.API,
                    API_1,
                    USER_ID,
                    Map.of("API_KEY", apiKey.getKey(), "API", API_1, "APPLICATION", APPLICATION_ID),
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
        var subscriptions = givenSubscriptions();
        var apiKey = givenAnApiKey(
            anApiKey()
                .toBuilder()
                .applicationId(application.getId())
                .subscriptions(subscriptions.stream().map(SubscriptionEntity::getId).toList())
                .build()
        );

        // When
        usecase.execute(new Input(API_KEY_ID, APPLICATION_ID, AUDIT_INFO));

        // Then
        assertThat(triggerNotificationDomainService.getApiNotifications()).containsExactly(
            new ApiKeyRevokedApiHookContext(API_1, APPLICATION_ID, PLAN_1, apiKey.getKey()),
            new ApiKeyRevokedApiHookContext(API_2, APPLICATION_ID, PLAN_2, apiKey.getKey())
        );
    }

    private BaseApplicationEntity givenAnApplication() {
        return givenAnApplication(anApplicationEntity().toBuilder().id(APPLICATION_ID).apiKeyMode(ApiKeyMode.SHARED).build());
    }

    private BaseApplicationEntity givenAnApplication(BaseApplicationEntity application) {
        applicationCrudService.initWith(List.of(application));
        return application;
    }

    private ApiKeyEntity givenAnApiKey(ApiKeyEntity apiKey) {
        apiKeyCrudService.initWith(List.of(apiKey));
        return apiKey;
    }

    private List<SubscriptionEntity> givenSubscriptions() {
        var subscriptions = List.of(
            SubscriptionFixtures.aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_1)
                .apiId(API_1)
                .applicationId(APPLICATION_ID)
                .planId(PLAN_1)
                .build(),
            SubscriptionFixtures.aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_2)
                .apiId(API_2)
                .applicationId(APPLICATION_ID)
                .planId(PLAN_2)
                .build()
        );
        subscriptionCrudService.initWith(subscriptions);
        return subscriptions;
    }
}
