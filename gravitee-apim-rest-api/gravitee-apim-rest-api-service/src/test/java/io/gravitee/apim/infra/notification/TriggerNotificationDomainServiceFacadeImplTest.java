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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.PrimaryOwnerDomainServiceInMemory;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.ApiNotificationTemplateData;
import io.gravitee.apim.core.notification.model.ApplicationNotificationTemplateData;
import io.gravitee.apim.core.notification.model.PlanNotificationTemplateData;
import io.gravitee.apim.core.notification.model.PrimaryOwnerNotificationTemplateData;
import io.gravitee.apim.core.notification.model.hook.ApiHookContext;
import io.gravitee.apim.core.notification.model.hook.ApplicationHookContext;
import io.gravitee.apim.core.notification.model.hook.HookContextEntry;
import io.gravitee.apim.infra.template.FreemarkerTemplateProcessor;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notifiers.EmailNotifierService;
import java.sql.Date;
import java.time.Instant;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TriggerNotificationDomainServiceFacadeImplTest {

    enum NotificationParametersMode {
        PASSED_AS_PARAMETER,
        COMPUTED_BY_METHOD,
    }

    @Mock
    NotifierService notifierService;

    @Mock
    ApiRepository apiRepository;

    @Mock
    ApplicationRepository applicationRepository;

    @Mock
    PlanRepository planRepository;

    @Mock
    GenericNotificationConfigRepository genericNotificationConfigRepository;

    PrimaryOwnerDomainServiceInMemory apiPrimaryOwnerDomainService = new PrimaryOwnerDomainServiceInMemory();

    ApiMetadataQueryServiceInMemory apiMetadataQueryService = new ApiMetadataQueryServiceInMemory();

    @Captor
    ArgumentCaptor<Map<String, Object>> paramsCaptor;

    TriggerNotificationDomainService service;

    @BeforeEach
    void setUp() {
        service =
            new TriggerNotificationDomainServiceFacadeImpl(
                notifierService,
                apiRepository,
                genericNotificationConfigRepository,
                applicationRepository,
                planRepository,
                apiPrimaryOwnerDomainService,
                apiMetadataQueryService,
                new FreemarkerTemplateProcessor()
            );
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Nested
    class TriggerApiNotification {

        @ParameterizedTest
        @EnumSource(value = NotificationParametersMode.class)
        public void should_fetch_api_notification_data(NotificationParametersMode notificationParametersMode) {
            // Given
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build()
            );

            // When
            final SimpleApiHookContextForTest apiHookContext = new SimpleApiHookContextForTest("api-id");
            switch (notificationParametersMode) {
                case PASSED_AS_PARAMETER -> {
                    final Map<String, Object> notificationParameters = service.prepareNotificationParameters(
                        GraviteeContext.getExecutionContext(),
                        apiHookContext
                    );
                    service.triggerApiNotification(GraviteeContext.getExecutionContext(), apiHookContext, notificationParameters);
                }
                case COMPUTED_BY_METHOD -> service.triggerApiNotification(GraviteeContext.getExecutionContext(), apiHookContext);
            }

            // Then
            verify(notifierService)
                .trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.SUBSCRIPTION_CLOSED), eq("api-id"), paramsCaptor.capture());
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

        @ParameterizedTest
        @EnumSource(value = NotificationParametersMode.class)
        public void should_fetch_api_notification_data_with_metadata(NotificationParametersMode notificationParametersMode) {
            // Given
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build(),
                List.of(
                    ApiMetadata.builder().key("key1").value("value1").format(MetadataFormat.STRING).build(),
                    //                    ApiMetadata.builder().key("email-support").value("${api.name}").format(MetadataFormat.STRING).build()
                    ApiMetadata.builder().key("email-support").value("${(api.primaryOwner.email)!''}").format(MetadataFormat.STRING).build()
                )
            );

            // When
            final SimpleApiHookContextForTest apiHookContext = new SimpleApiHookContextForTest("api-id");
            switch (notificationParametersMode) {
                case PASSED_AS_PARAMETER -> {
                    final Map<String, Object> notificationParameters = service.prepareNotificationParameters(
                        GraviteeContext.getExecutionContext(),
                        apiHookContext
                    );
                    service.triggerApiNotification(GraviteeContext.getExecutionContext(), apiHookContext, notificationParameters);
                }
                case COMPUTED_BY_METHOD -> service.triggerApiNotification(GraviteeContext.getExecutionContext(), apiHookContext);
            }

            // Then
            verify(notifierService)
                .trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.SUBSCRIPTION_CLOSED), eq("api-id"), paramsCaptor.capture());
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

        @ParameterizedTest
        @EnumSource(value = NotificationParametersMode.class)
        public void should_fetch_application_notification_data(NotificationParametersMode notificationParametersMode) {
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
                    .build(),
                PrimaryOwnerEntity.builder().id("user-2").displayName("Jen Doe").email("jen.doe@gravitee.io").type("USER").build()
            );

            // When
            final SimpleApiHookContextForTest apiHookContext = new SimpleApiHookContextForTest(
                "api-id",
                Map.of(HookContextEntry.APPLICATION_ID, "application-id")
            );
            switch (notificationParametersMode) {
                case PASSED_AS_PARAMETER -> {
                    final Map<String, Object> notificationParameters = service.prepareNotificationParameters(
                        GraviteeContext.getExecutionContext(),
                        apiHookContext
                    );
                    service.triggerApiNotification(GraviteeContext.getExecutionContext(), apiHookContext, notificationParameters);
                }
                case COMPUTED_BY_METHOD -> service.triggerApiNotification(GraviteeContext.getExecutionContext(), apiHookContext);
            }

            // Then
            verify(notifierService)
                .trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.SUBSCRIPTION_CLOSED), eq("api-id"), paramsCaptor.capture());
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
                                .id("user-2")
                                .email("jen.doe@gravitee.io")
                                .displayName("Jen Doe")
                                .type("USER")
                                .build()
                        )
                        .build()
                );
        }

        @ParameterizedTest
        @EnumSource(value = NotificationParametersMode.class)
        public void should_fetch_plan_notification_data(NotificationParametersMode notificationParametersMode) {
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
            switch (notificationParametersMode) {
                case PASSED_AS_PARAMETER -> {
                    final Map<String, Object> notificationParameters = service.prepareNotificationParameters(
                        GraviteeContext.getExecutionContext(),
                        apiHookContext
                    );
                    service.triggerApiNotification(GraviteeContext.getExecutionContext(), apiHookContext, notificationParameters);
                }
                case COMPUTED_BY_METHOD -> service.triggerApiNotification(GraviteeContext.getExecutionContext(), apiHookContext);
            }

            // Then
            verify(notifierService)
                .trigger(eq(GraviteeContext.getExecutionContext()), eq(ApiHook.SUBSCRIPTION_CLOSED), eq("api-id"), paramsCaptor.capture());
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "plan",
                    PlanNotificationTemplateData
                        .builder()
                        .name("plan")
                        .description("plan-description")
                        .order(1)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
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

        @ParameterizedTest
        @EnumSource(value = NotificationParametersMode.class)
        public void should_fetch_application_notification_data(NotificationParametersMode notificationParametersMode) {
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
                    .build(),
                PrimaryOwnerEntity.builder().id("user-2").displayName("Jen Doe").email("jen.doe@gravitee.io").type("USER").build()
            );

            // When
            final SimpleApplicationHookContextForTest applicationHookContext = new SimpleApplicationHookContextForTest("application-id");
            switch (notificationParametersMode) {
                case PASSED_AS_PARAMETER -> {
                    final Map<String, Object> notificationParameters = service.prepareNotificationParameters(
                        GraviteeContext.getExecutionContext(),
                        applicationHookContext
                    );
                    service.triggerApplicationNotification(
                        GraviteeContext.getExecutionContext(),
                        applicationHookContext,
                        notificationParameters
                    );
                }
                case COMPUTED_BY_METHOD -> service.triggerApplicationNotification(
                    GraviteeContext.getExecutionContext(),
                    applicationHookContext
                );
            }

            // Then
            verify(notifierService)
                .trigger(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq("application-id"),
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
                                .id("user-2")
                                .email("jen.doe@gravitee.io")
                                .displayName("Jen Doe")
                                .type("USER")
                                .build()
                        )
                        .build()
                );
        }

        @ParameterizedTest
        @EnumSource(value = NotificationParametersMode.class)
        public void should_fetch_api_notification_data(NotificationParametersMode notificationParametersMode) {
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
            switch (notificationParametersMode) {
                case PASSED_AS_PARAMETER -> {
                    final Map<String, Object> notificationParameters = service.prepareNotificationParameters(
                        GraviteeContext.getExecutionContext(),
                        applicationHookContext
                    );
                    service.triggerApplicationNotification(
                        GraviteeContext.getExecutionContext(),
                        applicationHookContext,
                        notificationParameters
                    );
                }
                case COMPUTED_BY_METHOD -> service.triggerApplicationNotification(
                    GraviteeContext.getExecutionContext(),
                    applicationHookContext
                );
            }

            // Then
            verify(notifierService)
                .trigger(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq("application-id"),
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

        @ParameterizedTest
        @EnumSource(value = NotificationParametersMode.class)
        public void should_fetch_api_notification_data_with_metadata(NotificationParametersMode notificationParametersMode) {
            // Given
            givenExistingApi(
                anApi().withId("api-id"),
                PrimaryOwnerEntity.builder().id("user-id").displayName("Jane Doe").email("jane.doe@gravitee.io").type("USER").build(),
                List.of(
                    ApiMetadata.builder().key("key1").value("value1").format(MetadataFormat.STRING).build(),
                    //                    ApiMetadata.builder().key("email-support").value("${api.name}").format(MetadataFormat.STRING).build()
                    ApiMetadata.builder().key("email-support").value("${(api.primaryOwner.email)!''}").format(MetadataFormat.STRING).build()
                )
            );

            // When
            final SimpleApplicationHookContextForTest applicationHookContext = new SimpleApplicationHookContextForTest(
                "application-id",
                Map.of(HookContextEntry.API_ID, "api-id")
            );
            switch (notificationParametersMode) {
                case PASSED_AS_PARAMETER -> {
                    final Map<String, Object> notificationParameters = service.prepareNotificationParameters(
                        GraviteeContext.getExecutionContext(),
                        applicationHookContext
                    );
                    service.triggerApplicationNotification(
                        GraviteeContext.getExecutionContext(),
                        applicationHookContext,
                        notificationParameters
                    );
                }
                case COMPUTED_BY_METHOD -> service.triggerApplicationNotification(
                    GraviteeContext.getExecutionContext(),
                    applicationHookContext
                );
            }

            // Then
            verify(notifierService)
                .trigger(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq("application-id"),
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

        @ParameterizedTest
        @EnumSource(value = NotificationParametersMode.class)
        public void should_fetch_plan_notification_data(NotificationParametersMode notificationParametersMode) {
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
                    .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                    .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                    .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                    .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
                    .build()
            );
            // When
            final SimpleApplicationHookContextForTest applicationHookContext = new SimpleApplicationHookContextForTest(
                "application-id",
                Map.of(HookContextEntry.PLAN_ID, "plan-id")
            );
            switch (notificationParametersMode) {
                case PASSED_AS_PARAMETER -> {
                    final Map<String, Object> notificationParameters = service.prepareNotificationParameters(
                        GraviteeContext.getExecutionContext(),
                        applicationHookContext
                    );
                    service.triggerApplicationNotification(
                        GraviteeContext.getExecutionContext(),
                        applicationHookContext,
                        notificationParameters
                    );
                }
                case COMPUTED_BY_METHOD -> service.triggerApplicationNotification(
                    GraviteeContext.getExecutionContext(),
                    applicationHookContext
                );
            }

            // Then
            verify(notifierService)
                .trigger(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(ApplicationHook.SUBSCRIPTION_CLOSED),
                    eq("application-id"),
                    paramsCaptor.capture()
                );
            var params = paramsCaptor.getValue();
            assertThat(params)
                .containsEntry(
                    "plan",
                    PlanNotificationTemplateData
                        .builder()
                        .name("plan")
                        .description("plan-description")
                        .order(1)
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
                        .publishedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                        .closedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
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
    class TriggerApplicationEmailNotification {

        @Test
        public void should_trigger_application_email_notification_() {
            // Given
            final TriggerApplicationNotification.SimpleApplicationHookContextForTest applicationHookContext =
                new TriggerApplicationNotification.SimpleApplicationHookContextForTest("application-id");
            when(
                notifierService.hasEmailNotificationFor(
                    GraviteeContext.getExecutionContext(),
                    applicationHookContext.getHook(),
                    applicationHookContext.getApplicationId(),
                    Map.of(),
                    "existing@mail.fake"
                )
            )
                .thenReturn(true);

            // When
            service.triggerApplicationEmailNotification(
                GraviteeContext.getExecutionContext(),
                applicationHookContext,
                Map.of(),
                "existing@mail.fake"
            );

            // Then
            verify(notifierService)
                .hasEmailNotificationFor(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(applicationHookContext.getHook()),
                    eq(applicationHookContext.getApplicationId()),
                    eq(Map.of()),
                    eq("existing@mail.fake")
                );
            verify(notifierService)
                .triggerEmail(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(applicationHookContext.getHook()),
                    eq(applicationHookContext.getApplicationId()),
                    eq(Map.of()),
                    eq("existing@mail.fake")
                );
        }

        @Test
        public void should_not_trigger_application_email_notification_() {
            // Given
            final TriggerApplicationNotification.SimpleApplicationHookContextForTest applicationHookContext =
                new TriggerApplicationNotification.SimpleApplicationHookContextForTest("application-id");
            when(
                notifierService.hasEmailNotificationFor(
                    GraviteeContext.getExecutionContext(),
                    applicationHookContext.getHook(),
                    applicationHookContext.getApplicationId(),
                    Map.of(),
                    "not-existing@mail.fake"
                )
            )
                .thenReturn(false);

            // When
            service.triggerApplicationEmailNotification(
                GraviteeContext.getExecutionContext(),
                applicationHookContext,
                Map.of(),
                "not-existing@mail.fake"
            );

            // Then
            verify(notifierService)
                .hasEmailNotificationFor(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(applicationHookContext.getHook()),
                    eq(applicationHookContext.getApplicationId()),
                    eq(Map.of()),
                    eq("not-existing@mail.fake")
                );
            verify(notifierService, never())
                .triggerEmail(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(applicationHookContext.getHook()),
                    eq(applicationHookContext.getApplicationId()),
                    eq(Map.of()),
                    eq("not-existing@mail.fake")
                );
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

        apiPrimaryOwnerDomainService.initWith(List.of(Map.entry(api.getId(), primaryOwnerEntity)));

        apiMetadataQueryService.initWith(List.of(Map.entry(api.getId(), metadata)));
    }

    @SneakyThrows
    private void givenExistingApplication(Application application, PrimaryOwnerEntity primaryOwnerEntity) {
        lenient().when(applicationRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(applicationRepository.findById(eq(application.getId()))).thenReturn(Optional.of(application));

        apiPrimaryOwnerDomainService.initWith(List.of(Map.entry(application.getId(), primaryOwnerEntity)));
    }

    @SneakyThrows
    private void givenExistingPlan(Plan plan) {
        lenient().when(planRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(planRepository.findById(eq(plan.getId()))).thenReturn(Optional.of(plan));
    }
}
