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

import static fixtures.core.model.MembershipFixtures.anApiPrimaryOwnerUserMembership;
import static fixtures.core.model.MembershipFixtures.anApplicationPrimaryOwnerUserMembership;
import static fixtures.repository.ApiFixtures.anApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import fixtures.repository.IntegrationFixture;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.AuditCrudServiceInMemory;
import inmemory.GroupQueryServiceInMemory;
import inmemory.MembershipCrudServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApplicationPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.ApiNotificationTemplateData;
import io.gravitee.apim.core.notification.model.ApplicationNotificationTemplateData;
import io.gravitee.apim.core.notification.model.IntegrationNotificationTemplateData;
import io.gravitee.apim.core.notification.model.PlanNotificationTemplateData;
import io.gravitee.apim.core.notification.model.PrimaryOwnerNotificationTemplateData;
import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.apim.core.notification.model.SubscriptionNotificationTemplateData;
import io.gravitee.apim.core.notification.model.hook.ApiHookContext;
import io.gravitee.apim.core.notification.model.hook.ApplicationHookContext;
import io.gravitee.apim.core.notification.model.hook.HookContextEntry;
import io.gravitee.apim.core.notification.model.hook.portal.PortalHookContext;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.notification.internal.TemplateDataFetcher;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.repository.management.model.Integration;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.PortalHook;
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
    public static final String USER_ID = "user-id";
    public static final String USER_ID_2 = "user-id-2";
    public static final String API_ID = "api-id";
    public static final String APPLICATION_ID = "application-id";
    public static final String INTEGRATION_ID = "integration-id";

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

    @Mock
    IntegrationRepository integrationRepository;

    MembershipCrudServiceInMemory membershipCrudService = new MembershipCrudServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    ApiMetadataQueryServiceInMemory apiMetadataQueryService = new ApiMetadataQueryServiceInMemory();

    @Captor
    ArgumentCaptor<Map<String, Object>> paramsCaptor;

    TriggerNotificationDomainService service;

    @BeforeEach
    void setUp() {
        var membershipQueryService = new MembershipQueryServiceInMemory(membershipCrudService);

        service =
            new TriggerNotificationDomainServiceFacadeImpl(
                notifierService,
                new TemplateDataFetcher(
                    apiRepository,
                    applicationRepository,
                    planRepository,
                    subscriptionRepository,
                    integrationRepository,
                    new ApiPrimaryOwnerDomainService(
                        new AuditDomainService(new AuditCrudServiceInMemory(), userCrudService, new JacksonJsonDiffProcessor()),
                        new GroupQueryServiceInMemory(),
                        membershipCrudService,
                        membershipQueryService,
                        roleQueryService,
                        userCrudService
                    ),
                    new ApplicationPrimaryOwnerDomainService(
                        new GroupQueryServiceInMemory(),
                        membershipQueryService,
                        roleQueryService,
                        userCrudService
                    ),
                    new ApiMetadataDecoderDomainService(apiMetadataQueryService, new FreemarkerTemplateProcessor())
                )
            );

        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
        userCrudService.initWith(
            List.of(
                BaseUserEntity.builder().id(USER_ID).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build(),
                BaseUserEntity.builder().id(USER_ID_2).firstname("Jen").lastname("Doe").email("jen.doe@gravitee.io").build()
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
            givenExistingApi(anApi().withId(API_ID), PrimaryOwnerEntity.builder().id(USER_ID).build());

            // When
            final SimpleApiHookContextForTest apiHookContext = new SimpleApiHookContextForTest(API_ID);
            service.triggerApiNotification(ORGANIZATION_ID, apiHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApiHook.SUBSCRIPTION_CLOSED),
                    eq(API_ID),
                    paramsCaptor.capture()
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "api",
                    ApiNotificationTemplateData
                        .builder()
                        .id(API_ID)
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
                                .id(USER_ID)
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
                anApi().withId(API_ID),
                PrimaryOwnerEntity.builder().id(USER_ID).build(),
                List.of(
                    ApiMetadata.builder().apiId(API_ID).key("key1").value("value1").format(Metadata.MetadataFormat.STRING).build(),
                    ApiMetadata.builder().apiId(API_ID).key("null_key").value(null).format(Metadata.MetadataFormat.STRING).build(),
                    ApiMetadata
                        .builder()
                        .apiId(API_ID)
                        .key("email-support")
                        .value("${(api.primaryOwner.email)!''}")
                        .format(Metadata.MetadataFormat.STRING)
                        .build()
                )
            );

            // When
            final SimpleApiHookContextForTest apiHookContext = new SimpleApiHookContextForTest(API_ID);
            service.triggerApiNotification(ORGANIZATION_ID, apiHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApiHook.SUBSCRIPTION_CLOSED),
                    eq(API_ID),
                    paramsCaptor.capture()
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "api",
                    ApiNotificationTemplateData
                        .builder()
                        .id(API_ID)
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
                                .id(USER_ID)
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
            givenExistingApi(anApi().withId(API_ID), PrimaryOwnerEntity.builder().id(USER_ID).build());
            givenExistingApplication(
                Application
                    .builder()
                    .id(APPLICATION_ID)
                    .name("application-name")
                    .type(ApplicationType.SIMPLE)
                    .status(ApplicationStatus.ACTIVE)
                    .description("application-description")
                    .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                    .apiKeyMode(ApiKeyMode.SHARED)
                    .build(),
                PrimaryOwnerEntity.builder().id(USER_ID_2).build()
            );

            // When
            final SimpleApiHookContextForTest apiHookContext = new SimpleApiHookContextForTest(
                API_ID,
                Map.of(HookContextEntry.APPLICATION_ID, APPLICATION_ID)
            );
            service.triggerApiNotification(ORGANIZATION_ID, apiHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApiHook.SUBSCRIPTION_CLOSED),
                    eq(API_ID),
                    paramsCaptor.capture()
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "application",
                    ApplicationNotificationTemplateData
                        .builder()
                        .name("application-name")
                        .type("SIMPLE")
                        .status("ACTIVE")
                        .description("application-description")
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .primaryOwner(
                            PrimaryOwnerNotificationTemplateData
                                .builder()
                                .id(USER_ID_2)
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
            givenExistingApi(anApi().withId(API_ID), PrimaryOwnerEntity.builder().id(USER_ID).build());

            givenExistingPlan(
                Plan
                    .builder()
                    .id("plan-id")
                    .api(API_ID)
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
                API_ID,
                Map.of(HookContextEntry.PLAN_ID, "plan-id")
            );
            service.triggerApiNotification(ORGANIZATION_ID, apiHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApiHook.SUBSCRIPTION_CLOSED),
                    eq(API_ID),
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
            givenExistingApi(anApi().withId(API_ID), PrimaryOwnerEntity.builder().id(USER_ID).build());

            givenExistingPlan(
                Plan
                    .builder()
                    .id("plan-id")
                    .api(API_ID)
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
                API_ID,
                Map.of(HookContextEntry.PLAN_ID, "plan-id")
            );
            service.triggerApiNotification(ORGANIZATION_ID, apiHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApiHook.SUBSCRIPTION_CLOSED),
                    eq(API_ID),
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
            givenExistingApi(anApi().withId(API_ID), PrimaryOwnerEntity.builder().id(USER_ID).build());

            givenExistingSubscription(Subscription.builder().id("subscription-id").request("my-request").reason("my-reason").build());
            // When
            final SimpleApiHookContextForTest apiHookContext = new SimpleApiHookContextForTest(
                API_ID,
                Map.of(HookContextEntry.SUBSCRIPTION_ID, "subscription-id")
            );
            service.triggerApiNotification(ORGANIZATION_ID, apiHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApiHook.SUBSCRIPTION_CLOSED),
                    eq(API_ID),
                    paramsCaptor.capture()
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "subscription",
                    SubscriptionNotificationTemplateData.builder().id("subscription-id").request("my-request").reason("my-reason").build()
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
                    .id(APPLICATION_ID)
                    .name("application-name")
                    .type(ApplicationType.SIMPLE)
                    .status(ApplicationStatus.ACTIVE)
                    .description("application-description")
                    .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                    .apiKeyMode(ApiKeyMode.SHARED)
                    .build(),
                PrimaryOwnerEntity.builder().id(USER_ID_2).build()
            );

            // When
            final SimpleApplicationHookContextForTest applicationHookContext = new SimpleApplicationHookContextForTest(APPLICATION_ID);
            service.triggerApplicationNotification(ORGANIZATION_ID, applicationHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq(APPLICATION_ID),
                    paramsCaptor.capture(),
                    eq(Collections.emptyList())
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "application",
                    ApplicationNotificationTemplateData
                        .builder()
                        .name("application-name")
                        .type("SIMPLE")
                        .status("ACTIVE")
                        .description("application-description")
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .primaryOwner(
                            PrimaryOwnerNotificationTemplateData
                                .builder()
                                .id(USER_ID_2)
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
                    .id(APPLICATION_ID)
                    .name("application-name")
                    .description("application-description")
                    .type(ApplicationType.SIMPLE)
                    .status(ApplicationStatus.ACTIVE)
                    .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                    .apiKeyMode(ApiKeyMode.SHARED)
                    .build(),
                PrimaryOwnerEntity.builder().id(USER_ID_2).build()
            );
            givenExistingApi(anApi().withId(API_ID), PrimaryOwnerEntity.builder().id(USER_ID).build());

            // When
            final SimpleApplicationHookContextForTest applicationHookContext = new SimpleApplicationHookContextForTest(
                APPLICATION_ID,
                Map.of(HookContextEntry.API_ID, API_ID)
            );
            service.triggerApplicationNotification(ORGANIZATION_ID, applicationHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq(APPLICATION_ID),
                    paramsCaptor.capture(),
                    eq(Collections.emptyList())
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "api",
                    ApiNotificationTemplateData
                        .builder()
                        .id(API_ID)
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
                                .id(USER_ID)
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
                anApi().withId(API_ID),
                PrimaryOwnerEntity.builder().id(USER_ID).build(),
                List.of(
                    ApiMetadata.builder().apiId(API_ID).key("key1").value("value1").format(Metadata.MetadataFormat.STRING).build(),
                    ApiMetadata.builder().apiId(API_ID).key("null_key").value(null).format(Metadata.MetadataFormat.STRING).build(),
                    ApiMetadata
                        .builder()
                        .apiId(API_ID)
                        .key("email-support")
                        .value("${(api.primaryOwner.email)!''}")
                        .format(Metadata.MetadataFormat.STRING)
                        .build()
                )
            );

            // When
            final SimpleApplicationHookContextForTest applicationHookContext = new SimpleApplicationHookContextForTest(
                APPLICATION_ID,
                Map.of(HookContextEntry.API_ID, API_ID)
            );
            service.triggerApplicationNotification(ORGANIZATION_ID, applicationHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq(APPLICATION_ID),
                    paramsCaptor.capture(),
                    eq(Collections.emptyList())
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "api",
                    ApiNotificationTemplateData
                        .builder()
                        .id(API_ID)
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
                                .id(USER_ID)
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
            givenExistingApi(anApi().withId(API_ID), PrimaryOwnerEntity.builder().id(USER_ID).build());

            givenExistingPlan(
                Plan
                    .builder()
                    .id("plan-id")
                    .api(API_ID)
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
                APPLICATION_ID,
                Map.of(HookContextEntry.PLAN_ID, "plan-id")
            );
            service.triggerApplicationNotification(ORGANIZATION_ID, applicationHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq(APPLICATION_ID),
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
            givenExistingApi(anApi().withId(API_ID), PrimaryOwnerEntity.builder().id(USER_ID).build());

            givenExistingPlan(
                Plan
                    .builder()
                    .id("plan-id")
                    .api(API_ID)
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
                APPLICATION_ID,
                Map.of(HookContextEntry.PLAN_ID, "plan-id")
            );
            service.triggerApplicationNotification(ORGANIZATION_ID, applicationHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq(APPLICATION_ID),
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
            givenExistingApi(anApi().withId(API_ID), PrimaryOwnerEntity.builder().id(USER_ID).build());

            givenExistingSubscription(Subscription.builder().id("subscription-id").request("my-request").reason("my-reason").build());
            // When
            var hook = new SimpleApplicationHookContextForTest(APPLICATION_ID, Map.of(HookContextEntry.SUBSCRIPTION_ID, "subscription-id"));
            service.triggerApplicationNotification(ORGANIZATION_ID, hook);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq(APPLICATION_ID),
                    paramsCaptor.capture(),
                    eq(Collections.emptyList())
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "subscription",
                    SubscriptionNotificationTemplateData.builder().id("subscription-id").request("my-request").reason("my-reason").build()
                );
        }

        @Test
        public void should_send_notification_to_additional_recipient() {
            // Given
            givenExistingApplication(
                Application
                    .builder()
                    .id(APPLICATION_ID)
                    .name("application-name")
                    .type(ApplicationType.SIMPLE)
                    .status(ApplicationStatus.ACTIVE)
                    .description("application-description")
                    .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                    .apiKeyMode(ApiKeyMode.SHARED)
                    .build(),
                PrimaryOwnerEntity.builder().id(USER_ID_2).build()
            );

            // When
            final SimpleApplicationHookContextForTest applicationHookContext = new SimpleApplicationHookContextForTest(APPLICATION_ID);
            List<Recipient> additionalRecipients = List.of(new Recipient("EMAIL", "user@gravitee.io"));
            service.triggerApplicationNotification(ORGANIZATION_ID, applicationHookContext, additionalRecipients);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq(APPLICATION_ID),
                    paramsCaptor.capture(),
                    same(additionalRecipients)
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "application",
                    ApplicationNotificationTemplateData
                        .builder()
                        .name("application-name")
                        .type("SIMPLE")
                        .status("ACTIVE")
                        .description("application-description")
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .primaryOwner(
                            PrimaryOwnerNotificationTemplateData
                                .builder()
                                .id(USER_ID_2)
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

    @Nested
    class TriggerPortalNotification {

        @Test
        public void should_fetch_portal_notification_data() {
            // Given
            var integration = givenExistingIntegration(IntegrationFixture.anIntegration().withId(INTEGRATION_ID));

            // When
            var portalHookContext = new SimplePortalHookContextForTest(Map.of(HookContextEntry.INTEGRATION_ID, INTEGRATION_ID));
            service.triggerPortalNotification(ORGANIZATION_ID, portalHookContext);

            // Then
            verify(notifierService)
                .trigger(
                    eq(new ExecutionContext(ORGANIZATION_ID, null)),
                    eq(PortalHook.FEDERATED_APIS_INGESTION_COMPLETE),
                    paramsCaptor.capture()
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "integration",
                    IntegrationNotificationTemplateData
                        .builder()
                        .id(INTEGRATION_ID)
                        .name(integration.getName())
                        .provider(integration.getProvider())
                        .build()
                );
        }

        static class SimplePortalHookContextForTest extends PortalHookContext {

            private final Map<HookContextEntry, String> properties;

            public SimplePortalHookContextForTest(Map<HookContextEntry, String> properties) {
                super(PortalHook.FEDERATED_APIS_INGESTION_COMPLETE);
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

        membershipCrudService.initWith(List.of(anApiPrimaryOwnerUserMembership(API_ID, primaryOwnerEntity.id(), ORGANIZATION_ID)));

        apiMetadataQueryService.initWithApiMetadata(metadata);
    }

    @SneakyThrows
    private void givenExistingApplication(Application application, PrimaryOwnerEntity primaryOwnerEntity) {
        lenient().when(applicationRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(applicationRepository.findById(eq(application.getId()))).thenReturn(Optional.of(application));

        membershipCrudService.initWith(
            List.of(anApplicationPrimaryOwnerUserMembership(APPLICATION_ID, primaryOwnerEntity.id(), ORGANIZATION_ID))
        );
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

    @SneakyThrows
    private Integration givenExistingIntegration(Integration integration) {
        lenient().when(integrationRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(integrationRepository.findById(eq(integration.getId()))).thenReturn(Optional.of(integration));

        return integration;
    }
}
