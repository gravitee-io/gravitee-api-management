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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.PortalNotificationRepository;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotification;
import io.gravitee.rest.api.model.ApiModel;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.notification.PortalHook;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortalNotificationServiceImplTest {

    @Mock
    private PortalNotificationRepository portalNotificationRepository;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @InjectMocks
    private PortalNotificationServiceImpl cut;

    @Captor
    private ArgumentCaptor<List<PortalNotification>> created;

    @BeforeEach
    void setUp() {
        when(notificationTemplateService.resolveTemplateWithParam(eq("org-id"), anyString(), any())).thenReturn("resolved");
    }

    @Test
    @SneakyThrows
    void should_store_hook_api_application_and_environment_of_an_application_hook() {
        var api = new ApiModel();
        api.setId("api-id");
        var application = new ApplicationEntity();
        application.setId("application-id");
        var params = new NotificationParamsBuilder().api(api).application(application).build();

        cut.create(
            new ExecutionContext("org-id", "env-id"),
            ApplicationHook.SUBSCRIPTION_ACCEPTED,
            List.of("user-1", "user-2"),
            params,
            NotificationReferenceType.APPLICATION,
            "application-id"
        );

        verify(portalNotificationRepository).create(created.capture());
        assertThat(created.getValue())
            .hasSize(2)
            .allSatisfy(notification -> {
                assertThat(notification.getHook()).isEqualTo("SUBSCRIPTION_ACCEPTED");
                assertThat(notification.getApiId()).isEqualTo("api-id");
                assertThat(notification.getApplicationId()).isEqualTo("application-id");
                assertThat(notification.getEnvironmentId()).isEqualTo("env-id");
                assertThat(notification.getReadAt()).isNull();
                assertThat(notification.getTitle()).isEqualTo("resolved");
                assertThat(notification.getMessage()).isEqualTo("resolved");
            })
            .extracting(PortalNotification::getUser)
            .containsExactly("user-1", "user-2");
    }

    @Test
    @SneakyThrows
    void should_store_api_of_an_api_hook_from_its_reference() {
        cut.create(
            new ExecutionContext("org-id", "env-id"),
            ApiHook.API_STOPPED,
            List.of("user-1"),
            Map.of(),
            NotificationReferenceType.API,
            "api-id"
        );

        verify(portalNotificationRepository).create(created.capture());
        var notification = created.getValue().getFirst();
        assertThat(notification.getHook()).isEqualTo("API_STOPPED");
        assertThat(notification.getApiId()).isEqualTo("api-id");
        assertThat(notification.getApplicationId()).isNull();
    }

    @Test
    @SneakyThrows
    void should_store_no_reference_for_a_portal_hook() {
        cut.create(
            new ExecutionContext("org-id", "env-id"),
            PortalHook.GROUP_INVITATION,
            List.of("user-1"),
            Map.of(),
            NotificationReferenceType.ENVIRONMENT,
            "env-id"
        );

        verify(portalNotificationRepository).create(created.capture());
        var notification = created.getValue().getFirst();
        assertThat(notification.getHook()).isEqualTo("GROUP_INVITATION");
        assertThat(notification.getApiId()).isNull();
        assertThat(notification.getApplicationId()).isNull();
        assertThat(notification.getEnvironmentId()).isEqualTo("env-id");
    }

    @Test
    @SneakyThrows
    void should_store_no_environment_for_an_organization_level_trigger() {
        cut.create(new ExecutionContext("org-id"), PortalHook.USER_REGISTERED, List.of("user-1"), Map.of(), null, null);

        verify(portalNotificationRepository).create(created.capture());
        assertThat(created.getValue().getFirst().getEnvironmentId()).isNull();
    }

    @Test
    @SneakyThrows
    void should_read_references_from_params_without_trigger_reference() {
        var api = new ApiModel();
        api.setId("api-id");

        cut.create(
            new ExecutionContext("org-id", "env-id"),
            ApiHook.NEW_SPEC_GENERATED,
            List.of("user-1"),
            new NotificationParamsBuilder().api(api).build()
        );

        verify(portalNotificationRepository).create(created.capture());
        var notification = created.getValue().getFirst();
        assertThat(notification.getHook()).isEqualTo("NEW_SPEC_GENERATED");
        assertThat(notification.getApiId()).isEqualTo("api-id");
        assertThat(notification.getEnvironmentId()).isEqualTo("env-id");
    }
}
