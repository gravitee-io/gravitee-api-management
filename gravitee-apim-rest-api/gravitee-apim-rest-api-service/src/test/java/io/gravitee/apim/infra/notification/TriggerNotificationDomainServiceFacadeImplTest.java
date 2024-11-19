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
package io.gravitee.apim.infra.notification;

import static fixtures.repository.ApiFixtures.anApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.PrimaryOwnerDomainServiceInMemory;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.ApiNotificationTemplateData;
import io.gravitee.apim.core.notification.model.ApplicationNotificationTemplateData;
import io.gravitee.apim.core.notification.model.PlanNotificationTemplateData;
import io.gravitee.apim.core.notification.model.PrimaryOwnerNotificationTemplateData;
import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.apim.core.notification.model.SubscriptionNotificationTemplateData;
import io.gravitee.apim.core.notification.model.hook.ApiHookContext;
import io.gravitee.apim.core.notification.model.hook.ApplicationHookContext;
import io.gravitee.apim.core.notification.model.hook.HookContextEntry;
import io.gravitee.apim.infra.notification.internal.TemplateDataFetcher;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import java.sql.Date;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TriggerNotificationDomainServiceFacadeImplTest {

    public static final String ORGANIZATION_ID = "DEFAULT";

    @Mock
    NotifierService notifierService;

    @Mock
    ApiRepository apiRepository;

    @Mock
    ApplicationRepository applicationRepository;

    @Mock
    PlanRepository planRepository;

    @Mock
    SubscriptionRepository subscriptionRepository;

    PrimaryOwnerDomainServiceInMemory primaryOwnerDomainService = new PrimaryOwnerDomainServiceInMemory();

    ApiMetadataQueryServiceInMemory apiMetadataQueryService = new ApiMetadataQueryServiceInMemory();

    @Captor
    ArgumentCaptor<Map<String, Object>> paramsCaptor;

    TriggerNotificationDomainService service;

    @BeforeEach
    void setUp() {
        service =
            new TriggerNotificationDomainServiceFacadeImpl(
                notifierService,
                new TemplateDataFetcher(
                    apiRepository,
                    applicationRepository,
                    planRepository,
                    subscriptionRepository,
                    primaryOwnerDomainService,
                    primaryOwnerDomainService,
                    apiMetadataQueryService,
                    new FreemarkerTemplateProcessor()
                )
            );
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Nested
    class TriggerApiNotification {

        @Test
        public void should_fetch_api_notification_data() {
            // Given
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build()
            );

            // When
            final SimpleApiHookContextForTest apiHookContext = new SimpleApiHookContextForTest("api-id");
            service.triggerApiNotification(ORGANIZATION_ID, apiHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApiHook.SUBSCRIPTION_CLOSED),
                    eq("api-id"),
                    paramsCaptor.capture()
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "api",
                    ApiNotificationTemplateData
                        .builder()
                        .id("api-id")
                        .name("api-name")
                        .description("api-description")
                        .apiVersion("api-version")
                        .definitionVersion(DefinitionVersion.V4)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .deployedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .primaryOwner(
                            PrimaryOwnerNotificationTemplateData
                                .builder()
                                .id("user-id")
                                .email("jane.doe@gravitee.io")
                                .displayName("Jane Doe")
                                .type("USER")
                                .build()
                        )
                        .metadata(Map.of())
                        .build()
                );
        }

        @Test
        public void should_fetch_api_notification_data_with_metadata() {
            // Given
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build(),
                List.of(
                    ApiMetadata.builder().key("key1").value("value1").format(MetadataFormat.STRING).build(),
                    ApiMetadata.builder().key("null_key").value(null).format(MetadataFormat.STRING).build(),
                    ApiMetadata.builder().key("email-support").value("${(api.primaryOwner.email)!''}").format(MetadataFormat.STRING).build()
                )
            );

            // When
            final SimpleApiHookContextForTest apiHookContext = new SimpleApiHookContextForTest("api-id");
            service.triggerApiNotification(ORGANIZATION_ID, apiHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApiHook.SUBSCRIPTION_CLOSED),
                    eq("api-id"),
                    paramsCaptor.capture()
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "api",
                    ApiNotificationTemplateData
                        .builder()
                        .id("api-id")
                        .name("api-name")
                        .description("api-description")
                        .apiVersion("api-version")
                        .definitionVersion(DefinitionVersion.V4)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .deployedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .primaryOwner(
                            PrimaryOwnerNotificationTemplateData
                                .builder()
                                .id("user-id")
                                .email("jane.doe@gravitee.io")
                                .displayName("Jane Doe")
                                .type("USER")
                                .build()
                        )
                        .metadata(Map.of("key1", "value1", "email-support", "jane.doe@gravitee.io"))
                        .build()
                );
        }

        @Test
        public void should_fetch_application_notification_data() {
            // Given
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build()
            );
            givenExistingApplication(
                Application
                    .builder()
                    .id("application-id")
                    .name("application-name")
                    .type(ApplicationType.SIMPLE)
                    .status(ApplicationStatus.ACTIVE)
                    .description("application-description")
                    .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                    .apiKeyMode(ApiKeyMode.SHARED)
                    .build(),
                PrimaryOwnerEntity.builder().id("user-2").displayName("Jen Doe").email("jen.doe@gravitee.io").type("USER").build()
            );

            // When
            final SimpleApiHookContextForTest apiHookContext = new SimpleApiHookContextForTest(
                "api-id",
                Map.of(HookContextEntry.APPLICATION_ID, "application-id")
            );
            service.triggerApiNotification(ORGANIZATION_ID, apiHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApiHook.SUBSCRIPTION_CLOSED),
                    eq("api-id"),
                    paramsCaptor.capture()
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "application",
                    ApplicationNotificationTemplateData
                        .builder()
                        .id("application-id")
                        .name("application-name")
                        .type("SIMPLE")
                        .status("ACTIVE")
                        .description("application-description")
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .primaryOwner(
                            PrimaryOwnerNotificationTemplateData
                                .builder()
                                .id("user-2")
                                .email("jen.doe@gravitee.io")
                                .displayName("Jen Doe")
                                .type("USER")
                                .build()
                        )
                        .apiKeyMode("SHARED")
                        .build()
                );
        }

        @Test
        public void should_fetch_plan_notification_data() {
            // Given
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build()
            );

            givenExistingPlan(
                Plan
                    .builder()
                    .id("plan-id")
                    .api("api-id")
                    .name("plan")
                    .description("plan-description")
                    .order(1)
                    .mode(Plan.PlanMode.STANDARD)
                    .security(Plan.PlanSecurityType.API_KEY)
                    .validation(Plan.PlanValidationType.MANUAL)
                    .commentMessage("my-comment-message")
                    .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                    .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                    .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                    .build()
            );
            // When
            final SimpleApiHookContextForTest apiHookContext = new SimpleApiHookContextForTest(
                "api-id",
                Map.of(HookContextEntry.PLAN_ID, "plan-id")
            );
            service.triggerApiNotification(ORGANIZATION_ID, apiHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApiHook.SUBSCRIPTION_CLOSED),
                    eq("api-id"),
                    paramsCaptor.capture()
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "plan",
                    PlanNotificationTemplateData
                        .builder()
                        .id("plan-id")
                        .name("plan")
                        .description("plan-description")
                        .order(1)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                        .security("API_KEY")
                        .validation("MANUAL")
                        .commentMessage("my-comment-message")
                        .build()
                );
        }

        @Test
        public void should_fetch_push_plan_notification_data() {
            // Given
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build()
            );

            givenExistingPlan(
                Plan
                    .builder()
                    .id("plan-id")
                    .api("api-id")
                    .name("plan")
                    .description("plan-description")
                    .order(1)
                    .mode(Plan.PlanMode.PUSH)
                    .validation(Plan.PlanValidationType.MANUAL)
                    .commentMessage("my-comment-message")
                    .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                    .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                    .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                    .build()
            );
            // When
            final SimpleApiHookContextForTest apiHookContext = new SimpleApiHookContextForTest(
                "api-id",
                Map.of(HookContextEntry.PLAN_ID, "plan-id")
            );
            service.triggerApiNotification(ORGANIZATION_ID, apiHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApiHook.SUBSCRIPTION_CLOSED),
                    eq("api-id"),
                    paramsCaptor.capture()
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "plan",
                    PlanNotificationTemplateData
                        .builder()
                        .id("plan-id")
                        .name("plan")
                        .description("plan-description")
                        .order(1)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                        .validation("MANUAL")
                        .commentMessage("my-comment-message")
                        .build()
                );
        }

        @Test
        public void should_fetch_subscription_notification_data() {
            // Given
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build()
            );

            givenExistingSubscription(
                Subscription
                    .builder()
                    .id("subscription-id")
                    .request("my-request")
                    .reason("my-reason")
                    .status(Subscription.Status.ACCEPTED)
                    .build()
            );
            // When
            final SimpleApiHookContextForTest apiHookContext = new SimpleApiHookContextForTest(
                "api-id",
                Map.of(HookContextEntry.SUBSCRIPTION_ID, "subscription-id")
            );
            service.triggerApiNotification(ORGANIZATION_ID, apiHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApiHook.SUBSCRIPTION_CLOSED),
                    eq("api-id"),
                    paramsCaptor.capture()
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "subscription",
                    SubscriptionNotificationTemplateData
                        .builder()
                        .id("subscription-id")
                        .request("my-request")
                        .reason("my-reason")
                        .status("ACCEPTED")
                        .build()
                );
        }

        static class SimpleApiHookContextForTest extends ApiHookContext {

            private final Map<HookContextEntry, String> properties;

            public SimpleApiHookContextForTest(String apiId) {
                this(apiId, new HashMap<>());
            }

            public SimpleApiHookContextForTest(String apiId, Map<HookContextEntry, String> properties) {
                super(ApiHook.SUBSCRIPTION_CLOSED, apiId);
                this.properties = properties;
            }

            @Override
            protected Map<HookContextEntry, String> getChildProperties() {
                return properties;
            }
        }
    }

    @Nested
    class TriggerApplicationNotification {

        @Test
        public void should_fetch_application_notification_data() {
            // Given
            givenExistingApplication(
                Application
                    .builder()
                    .id("application-id")
                    .name("application-name")
                    .type(ApplicationType.SIMPLE)
                    .status(ApplicationStatus.ACTIVE)
                    .description("application-description")
                    .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                    .apiKeyMode(ApiKeyMode.SHARED)
                    .build(),
                PrimaryOwnerEntity.builder().id("user-2").displayName("Jen Doe").email("jen.doe@gravitee.io").type("USER").build()
            );

            // When
            final SimpleApplicationHookContextForTest applicationHookContext = new SimpleApplicationHookContextForTest("application-id");
            service.triggerApplicationNotification(ORGANIZATION_ID, applicationHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq("application-id"),
                    paramsCaptor.capture(),
                    eq(Collections.emptyList())
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "application",
                    ApplicationNotificationTemplateData
                        .builder()
                        .id("application-id")
                        .name("application-name")
                        .type("SIMPLE")
                        .status("ACTIVE")
                        .description("application-description")
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .primaryOwner(
                            PrimaryOwnerNotificationTemplateData
                                .builder()
                                .id("user-2")
                                .email("jen.doe@gravitee.io")
                                .displayName("Jen Doe")
                                .type("USER")
                                .build()
                        )
                        .apiKeyMode("SHARED")
                        .build()
                );
        }

        @Test
        public void should_fetch_api_notification_data() {
            // Given
            givenExistingApplication(
                Application
                    .builder()
                    .id("application-id")
                    .name("application-name")
                    .description("application-description")
                    .type(ApplicationType.SIMPLE)
                    .status(ApplicationStatus.ACTIVE)
                    .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                    .apiKeyMode(ApiKeyMode.SHARED)
                    .build(),
                PrimaryOwnerEntity.builder().id("user-2").displayName("Jen Doe").email("jen.doe@gravitee.io").type("USER").build()
            );
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build()
            );

            // When
            final SimpleApplicationHookContextForTest applicationHookContext = new SimpleApplicationHookContextForTest(
                "application-id",
                Map.of(HookContextEntry.API_ID, "api-id")
            );
            service.triggerApplicationNotification(ORGANIZATION_ID, applicationHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq("application-id"),
                    paramsCaptor.capture(),
                    eq(Collections.emptyList())
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "api",
                    ApiNotificationTemplateData
                        .builder()
                        .id("api-id")
                        .name("api-name")
                        .description("api-description")
                        .apiVersion("api-version")
                        .definitionVersion(DefinitionVersion.V4)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .deployedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .primaryOwner(
                            PrimaryOwnerNotificationTemplateData
                                .builder()
                                .id("user-id")
                                .email("jane.doe@gravitee.io")
                                .displayName("Jane Doe")
                                .type("USER")
                                .build()
                        )
                        .metadata(Map.of())
                        .build()
                );
        }

        @Test
        public void should_fetch_api_notification_data_with_metadata() {
            // Given
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build(),
                List.of(
                    ApiMetadata.builder().key("key1").value("value1").format(MetadataFormat.STRING).build(),
                    ApiMetadata.builder().key("null_key").value(null).format(MetadataFormat.STRING).build(),
                    ApiMetadata.builder().key("email-support").value("${(api.primaryOwner.email)!''}").format(MetadataFormat.STRING).build()
                )
            );

            // When
            final SimpleApplicationHookContextForTest applicationHookContext = new SimpleApplicationHookContextForTest(
                "application-id",
                Map.of(HookContextEntry.API_ID, "api-id")
            );
            service.triggerApplicationNotification(ORGANIZATION_ID, applicationHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq("application-id"),
                    paramsCaptor.capture(),
                    eq(Collections.emptyList())
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "api",
                    ApiNotificationTemplateData
                        .builder()
                        .id("api-id")
                        .name("api-name")
                        .description("api-description")
                        .apiVersion("api-version")
                        .definitionVersion(DefinitionVersion.V4)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .deployedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .primaryOwner(
                            PrimaryOwnerNotificationTemplateData
                                .builder()
                                .id("user-id")
                                .email("jane.doe@gravitee.io")
                                .displayName("Jane Doe")
                                .type("USER")
                                .build()
                        )
                        .metadata(Map.of("key1", "value1", "email-support", "jane.doe@gravitee.io"))
                        .build()
                );
        }

        @Test
        public void should_fetch_plan_notification_data() {
            // Given
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build()
            );

            givenExistingPlan(
                Plan
                    .builder()
                    .id("plan-id")
                    .api("api-id")
                    .name("plan")
                    .description("plan-description")
                    .order(1)
                    .mode(Plan.PlanMode.STANDARD)
                    .security(Plan.PlanSecurityType.API_KEY)
                    .validation(Plan.PlanValidationType.MANUAL)
                    .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                    .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                    .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                    .commentMessage("my-comment-message")
                    .build()
            );
            // When
            final SimpleApplicationHookContextForTest applicationHookContext = new SimpleApplicationHookContextForTest(
                "application-id",
                Map.of(HookContextEntry.PLAN_ID, "plan-id")
            );
            service.triggerApplicationNotification(ORGANIZATION_ID, applicationHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq("application-id"),
                    paramsCaptor.capture(),
                    eq(Collections.emptyList())
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "plan",
                    PlanNotificationTemplateData
                        .builder()
                        .id("plan-id")
                        .name("plan")
                        .description("plan-description")
                        .order(1)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                        .security("API_KEY")
                        .validation("MANUAL")
                        .commentMessage("my-comment-message")
                        .build()
                );
        }

        @Test
        public void should_fetch_push_plan_notification_data() {
            // Given
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build()
            );

            givenExistingPlan(
                Plan
                    .builder()
                    .id("plan-id")
                    .api("api-id")
                    .name("plan")
                    .description("plan-description")
                    .order(1)
                    .mode(Plan.PlanMode.PUSH)
                    .validation(Plan.PlanValidationType.MANUAL)
                    .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                    .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                    .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                    .commentMessage("my-comment-message")
                    .build()
            );
            // When
            final SimpleApplicationHookContextForTest applicationHookContext = new SimpleApplicationHookContextForTest(
                "application-id",
                Map.of(HookContextEntry.PLAN_ID, "plan-id")
            );
            service.triggerApplicationNotification(ORGANIZATION_ID, applicationHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq("application-id"),
                    paramsCaptor.capture(),
                    eq(Collections.emptyList())
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "plan",
                    PlanNotificationTemplateData
                        .builder()
                        .id("plan-id")
                        .name("plan")
                        .description("plan-description")
                        .order(1)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                        .validation("MANUAL")
                        .commentMessage("my-comment-message")
                        .build()
                );
        }

        @Test
        public void should_fetch_subscription_notification_data() {
            // Given
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build()
            );

            givenExistingSubscription(
                Subscription
                    .builder()
                    .id("subscription-id")
                    .request("my-request")
                    .reason("my-reason")
                    .status(Subscription.Status.ACCEPTED)
                    .build()
            );
            // When
            var hook = new SimpleApplicationHookContextForTest(
                "application-id",
                Map.of(HookContextEntry.SUBSCRIPTION_ID, "subscription-id")
            );
            service.triggerApplicationNotification(ORGANIZATION_ID, hook);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq("application-id"),
                    paramsCaptor.capture(),
                    eq(Collections.emptyList())
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "subscription",
                    SubscriptionNotificationTemplateData
                        .builder()
                        .id("subscription-id")
                        .request("my-request")
                        .reason("my-reason")
                        .status("ACCEPTED")
                        .build()
                );
        }

        @Test
        public void should_send_notification_to_additional_recipient() {
            // Given
            givenExistingApplication(
                Application
                    .builder()
                    .id("application-id")
                    .name("application-name")
                    .type(ApplicationType.SIMPLE)
                    .status(ApplicationStatus.ACTIVE)
                    .description("application-description")
                    .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                    .apiKeyMode(ApiKeyMode.SHARED)
                    .build(),
                PrimaryOwnerEntity.builder().id("user-2").displayName("Jen Doe").email("jen.doe@gravitee.io").type("USER").build()
            );

            // When
            final SimpleApplicationHookContextForTest applicationHookContext = new SimpleApplicationHookContextForTest("application-id");
            List<Recipient> additionalRecipients = List.of(new Recipient("EMAIL", "user@gravitee.io"));
            service.triggerApplicationNotification(ORGANIZATION_ID, applicationHookContext, additionalRecipients);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq("application-id"),
                    paramsCaptor.capture(),
                    same(additionalRecipients)
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "application",
                    ApplicationNotificationTemplateData
                        .builder()
                        .id("application-id")
                        .name("application-name")
                        .type("SIMPLE")
                        .status("ACTIVE")
                        .description("application-description")
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .primaryOwner(
                            PrimaryOwnerNotificationTemplateData
                                .builder()
                                .id("user-2")
                                .email("jen.doe@gravitee.io")
                                .displayName("Jen Doe")
                                .type("USER")
                                .build()
                        )
                        .apiKeyMode("SHARED")
                        .build()
                );
        }

        static class SimpleApplicationHookContextForTest extends ApplicationHookContext {

            private final Map<HookContextEntry, String> properties;

            public SimpleApplicationHookContextForTest(String applicationId) {
                this(applicationId, new HashMap<>());
            }

            public SimpleApplicationHookContextForTest(String applicationId, Map<HookContextEntry, String> properties) {
                super(ApplicationHook.SUBSCRIPTION_CLOSED, applicationId);
                this.properties = properties;
            }

            @Override
            protected Map<HookContextEntry, String> getChildProperties() {
                return properties;
            }
        }
    }

    @SneakyThrows
    private void givenExistingApi(Api api, PrimaryOwnerEntity primaryOwnerEntity) {
        givenExistingApi(api, primaryOwnerEntity, List.of());
    }

    @SneakyThrows
    private void givenExistingApi(Api api, PrimaryOwnerEntity primaryOwnerEntity, List<ApiMetadata> metadata) {
        lenient().when(apiRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(apiRepository.findById(api.getId())).thenReturn(Optional.of(api));

        primaryOwnerDomainService.add(api.getId(), primaryOwnerEntity);

        apiMetadataQueryService.initWith(List.of(Map.entry(api.getId(), metadata)));
    }

    @SneakyThrows
    private void givenExistingApplication(Application application, PrimaryOwnerEntity primaryOwnerEntity) {
        lenient().when(applicationRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(applicationRepository.findById(eq(application.getId()))).thenReturn(Optional.of(application));

        primaryOwnerDomainService.add(application.getId(), primaryOwnerEntity);
    }

    @SneakyThrows
    private void givenExistingPlan(Plan plan) {
        lenient().when(planRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(planRepository.findById(eq(plan.getId()))).thenReturn(Optional.of(plan));
    }

    @SneakyThrows
    private void givenExistingSubscription(Subscription subscription) {
        lenient().when(subscriptionRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(subscriptionRepository.findById(eq(subscription.getId()))).thenReturn(Optional.of(subscription));
    }
}
