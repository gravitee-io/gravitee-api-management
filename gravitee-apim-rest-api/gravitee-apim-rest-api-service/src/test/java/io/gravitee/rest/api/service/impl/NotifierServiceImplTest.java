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

import static io.gravitee.rest.api.service.impl.NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.notifier.NotifierPlugin;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PortalNotificationConfig;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.EmailRecipientsService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApiProductHook;
import io.gravitee.rest.api.service.notification.PortalHook;
import io.gravitee.rest.api.service.notifiers.EmailNotifierService;
import io.gravitee.rest.api.service.notifiers.WebhookNotifierService;
import java.util.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class NotifierServiceImplTest {

    private NotifierServiceImpl cut;

    @Mock
    private ConfigurablePluginManager<NotifierPlugin> notifierManager;

    @Mock
    private PortalNotificationConfigRepository portalNotificationConfigRepository;

    @Mock
    private PortalNotificationService portalNotificationService;

    @Mock
    private GenericNotificationConfigRepository genericNotificationConfigRepository;

    @Mock
    private EmailNotifierService emailNotifierService;

    @Mock
    private WebhookNotifierService webhookNotifierService;

    @Mock
    private EmailRecipientsService emailRecipientsService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private MembershipService membershipService;

    @BeforeEach
    void setUp() {
        cut = new NotifierServiceImpl(
            notifierManager,
            portalNotificationConfigRepository,
            portalNotificationService,
            genericNotificationConfigRepository,
            emailNotifierService,
            webhookNotifierService,
            emailRecipientsService,
            parameterService,
            membershipService
        );
    }

    @Nested
    class TriggerEmail {

        @SneakyThrows
        @Test
        void should_trigger_email_to_all_recipients() {
            final Map<String, Object> params = Map.of();
            final ExecutionContext executionContext = new ExecutionContext();

            when(
                genericNotificationConfigRepository.findByReferenceAndHook(
                    ApiHook.API_STARTED.name(),
                    NotificationReferenceType.API,
                    "api-id"
                )
            ).thenReturn(
                List.of(
                    GenericNotificationConfig.builder()
                        .notifier("default-email")
                        .id(DEFAULT_EMAIL_NOTIFIER_ID)
                        .config("${(api.primaryOwner.email)!''};second.mail@gio.test")
                        .build()
                )
            );
            when(
                emailRecipientsService.processTemplatedRecipients(List.of("${(api.primaryOwner.email)!''};second.mail@gio.test"), params)
            ).thenReturn(Set.of("recipient@gio.test", "second.mail@gio.test"));
            when(parameterService.findAsBoolean(executionContext, Key.TRIAL_INSTANCE, ParameterReferenceType.SYSTEM)).thenReturn(false);

            cut.triggerGenericNotifications(
                executionContext,
                NotifierServiceImpl.TriggerNotificationsData.builder()
                    .hook(ApiHook.API_STARTED)
                    .referenceType(NotificationReferenceType.API)
                    .referenceId("api-id")
                    .params(params)
                    .recipients(List.of())
                    .build()
            );

            verify(emailRecipientsService, never()).filterRegisteredUser(any(), any());
            final ArgumentCaptor<Collection<String>> recipientsCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(emailNotifierService).trigger(eq(executionContext), eq(ApiHook.API_STARTED), eq(params), recipientsCaptor.capture());
            assertThat(recipientsCaptor.getValue()).containsExactlyInAnyOrder("recipient@gio.test", "second.mail@gio.test");
        }

        @Test
        @SneakyThrows
        void should_trigger_email_to_opted_in_recipients() {
            final Map<String, Object> params = Map.of();
            final ExecutionContext executionContext = new ExecutionContext();

            when(
                genericNotificationConfigRepository.findByReferenceAndHook(
                    ApiHook.API_STARTED.name(),
                    NotificationReferenceType.API,
                    "api-id"
                )
            ).thenReturn(
                List.of(
                    GenericNotificationConfig.builder()
                        .notifier("default-email")
                        .id(DEFAULT_EMAIL_NOTIFIER_ID)
                        .config("${(api.primaryOwner.email)!''};second.mail-not-opted-in@gio.test")
                        .build()
                )
            );
            when(
                emailRecipientsService.processTemplatedRecipients(
                    List.of("${(api.primaryOwner.email)!''};second.mail-not-opted-in@gio.test"),
                    params
                )
            ).thenReturn(Set.of("recipient@gio.test", "second.mail-not-opted-in@gio.test"));
            when(parameterService.findAsBoolean(executionContext, Key.TRIAL_INSTANCE, ParameterReferenceType.SYSTEM)).thenReturn(true);
            when(
                emailRecipientsService.filterRegisteredUser(
                    executionContext,
                    Set.of("recipient@gio.test", "second.mail-not-opted-in@gio.test")
                )
            ).thenReturn(Set.of("recipient@gio.test"));

            cut.triggerGenericNotifications(
                executionContext,
                NotifierServiceImpl.TriggerNotificationsData.builder()
                    .hook(ApiHook.API_STARTED)
                    .referenceType(NotificationReferenceType.API)
                    .referenceId("api-id")
                    .params(params)
                    .recipients(List.of())
                    .build()
            );

            final ArgumentCaptor<Collection<String>> recipientsCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(emailNotifierService).trigger(eq(executionContext), eq(ApiHook.API_STARTED), eq(params), recipientsCaptor.capture());
            assertThat(recipientsCaptor.getValue()).containsExactly("recipient@gio.test");
        }

        @Test
        @SneakyThrows
        void should_trigger_email_to_additional_opted_in_recipients() {
            final Map<String, Object> params = Map.of();
            final ExecutionContext executionContext = new ExecutionContext();

            when(
                genericNotificationConfigRepository.findByReferenceAndHook(
                    ApiHook.API_STARTED.name(),
                    NotificationReferenceType.API,
                    "api-id"
                )
            ).thenReturn(
                List.of(
                    GenericNotificationConfig.builder()
                        .notifier("default-email")
                        .id(DEFAULT_EMAIL_NOTIFIER_ID)
                        .config("${(api.primaryOwner.email)!''};second.mail-not-opted-in@gio.test")
                        .build()
                )
            );
            when(
                emailRecipientsService.processTemplatedRecipients(
                    argThat(a ->
                        a.containsAll(List.of("${(api.primaryOwner.email)!''};second.mail-not-opted-in@gio.test", "additional@gio.test"))
                    ),
                    eq(params)
                )
            ).thenReturn(Set.of("additional@gio.test", "recipient@gio.test", "second.mail-not-opted-in@gio.test"));
            when(parameterService.findAsBoolean(executionContext, Key.TRIAL_INSTANCE, ParameterReferenceType.SYSTEM)).thenReturn(true);
            when(
                emailRecipientsService.filterRegisteredUser(
                    eq(executionContext),
                    argThat(a -> a.containsAll(List.of("recipient@gio.test", "additional@gio.test", "second.mail-not-opted-in@gio.test")))
                )
            ).thenReturn(Set.of("additional@gio.test", "recipient@gio.test"));

            cut.triggerGenericNotifications(
                executionContext,
                NotifierServiceImpl.TriggerNotificationsData.builder()
                    .hook(ApiHook.API_STARTED)
                    .referenceType(NotificationReferenceType.API)
                    .referenceId("api-id")
                    .params(params)
                    .recipients(List.of(new Recipient("default-email", "additional@gio.test")))
                    .build()
            );

            final ArgumentCaptor<Collection<String>> recipientsCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(emailNotifierService).trigger(eq(executionContext), eq(ApiHook.API_STARTED), eq(params), recipientsCaptor.capture());
            assertThat(recipientsCaptor.getValue()).containsExactlyInAnyOrder("additional@gio.test", "recipient@gio.test");
        }

        @Test
        @SneakyThrows
        void should_not_trigger_notification_if_hook_not_present() {
            final Map<String, Object> params = Map.of();
            final ExecutionContext executionContext = new ExecutionContext();

            when(
                genericNotificationConfigRepository.findByReferenceAndHook(
                    ApiHook.API_STARTED.name(),
                    NotificationReferenceType.API,
                    "api-id"
                )
            ).thenReturn(Collections.emptyList());

            cut.triggerGenericNotifications(
                executionContext,
                NotifierServiceImpl.TriggerNotificationsData.builder()
                    .hook(ApiHook.API_STARTED)
                    .referenceType(NotificationReferenceType.API)
                    .referenceId("api-id")
                    .params(params)
                    .recipients(List.of())
                    .build()
            );

            verifyNoInteractions(emailNotifierService);
            verifyNoInteractions(webhookNotifierService);
        }
    }

    @Nested
    class TriggerWebhook {

        @Test
        @SneakyThrows
        void should_trigger_webhook_notifier() {
            final Map<String, Object> params = Map.of("key", "value");
            final ExecutionContext executionContext = new ExecutionContext();

            GenericNotificationConfig webhookConfig = GenericNotificationConfig.builder()
                .notifier("default-webhook")
                .id("wh-1")
                .config("{\"url\":\"https://hooks.example.com/notify\"}")
                .build();

            when(
                genericNotificationConfigRepository.findByReferenceAndHook(
                    ApiHook.API_STARTED.name(),
                    NotificationReferenceType.API,
                    "api-id"
                )
            ).thenReturn(List.of(webhookConfig));

            when(emailRecipientsService.processTemplatedRecipients(eq(Collections.emptyList()), eq(params))).thenReturn(
                Collections.emptySet()
            );
            when(parameterService.findAsBoolean(executionContext, Key.TRIAL_INSTANCE, ParameterReferenceType.SYSTEM)).thenReturn(false);

            cut.triggerGenericNotifications(
                executionContext,
                NotifierServiceImpl.TriggerNotificationsData.builder()
                    .hook(ApiHook.API_STARTED)
                    .referenceType(NotificationReferenceType.API)
                    .referenceId("api-id")
                    .params(params)
                    .build()
            );

            verify(webhookNotifierService).trigger(eq(ApiHook.API_STARTED), eq(webhookConfig), eq(params));
            verify(emailNotifierService).trigger(
                eq(executionContext),
                eq(ApiHook.API_STARTED),
                eq(params),
                argThat(c -> c != null && c.isEmpty())
            );
        }
    }

    @Nested
    class TriggerPortal {

        @Test
        @SneakyThrows
        void should_deliver_portal_notifications_for_environment_scoped_hook() {
            final Map<String, Object> params = Map.of();
            final ExecutionContext executionContext = new ExecutionContext("org-1", "env-1");

            PortalNotificationConfig portalCfg = PortalNotificationConfig.builder()
                .user("user-direct")
                .referenceType(NotificationReferenceType.ENVIRONMENT)
                .referenceId("env-1")
                .build();

            when(
                portalNotificationConfigRepository.findByReferenceAndHook(
                    PortalHook.GROUP_INVITATION.name(),
                    NotificationReferenceType.ENVIRONMENT,
                    "env-1"
                )
            ).thenReturn(List.of(portalCfg));

            when(
                genericNotificationConfigRepository.findByReferenceAndHook(
                    PortalHook.GROUP_INVITATION.name(),
                    NotificationReferenceType.ENVIRONMENT,
                    "env-1"
                )
            ).thenReturn(Collections.emptyList());

            cut.trigger(executionContext, PortalHook.GROUP_INVITATION, params);

            verify(portalNotificationService).create(
                eq(executionContext),
                eq(PortalHook.GROUP_INVITATION),
                argThat(users -> users.equals(List.of("user-direct"))),
                eq(params)
            );
        }

        @Test
        @SneakyThrows
        void should_deliver_portal_notifications_for_organization_scoped_hook() {
            final Map<String, Object> params = Map.of();
            final ExecutionContext executionContext = new ExecutionContext("org-1");

            PortalNotificationConfig portalCfg = PortalNotificationConfig.builder().user("org-user").build();

            when(portalNotificationConfigRepository.findByHookAndOrganizationId(PortalHook.USER_REGISTERED.name(), "org-1")).thenReturn(
                List.of(portalCfg)
            );

            when(genericNotificationConfigRepository.findByHookAndOrganizationId(PortalHook.USER_REGISTERED.name(), "org-1")).thenReturn(
                Collections.emptyList()
            );

            cut.trigger(executionContext, PortalHook.USER_REGISTERED, params);

            verify(portalNotificationService).create(
                eq(executionContext),
                eq(PortalHook.USER_REGISTERED),
                argThat(users -> users.equals(List.of("org-user"))),
                eq(params)
            );
        }

        @Test
        @SneakyThrows
        void should_include_group_members_in_portal_notification_recipients() {
            final Map<String, Object> params = Map.of();
            final ExecutionContext executionContext = new ExecutionContext("org-1", "env-1");

            PortalNotificationConfig portalCfg = PortalNotificationConfig.builder()
                .user("user-direct")
                .groups(Set.of("group-a"))
                .referenceType(NotificationReferenceType.ENVIRONMENT)
                .referenceId("env-1")
                .build();

            when(
                portalNotificationConfigRepository.findByReferenceAndHook(
                    PortalHook.GROUP_INVITATION.name(),
                    NotificationReferenceType.ENVIRONMENT,
                    "env-1"
                )
            ).thenReturn(List.of(portalCfg));

            when(
                genericNotificationConfigRepository.findByReferenceAndHook(
                    PortalHook.GROUP_INVITATION.name(),
                    NotificationReferenceType.ENVIRONMENT,
                    "env-1"
                )
            ).thenReturn(Collections.emptyList());

            MemberEntity fromGroup = new MemberEntity();
            fromGroup.setId("user-from-group");
            when(
                membershipService.getMembersByReferencesAndRole(executionContext, MembershipReferenceType.GROUP, List.of("group-a"), null)
            ).thenReturn(Set.of(fromGroup));

            cut.trigger(executionContext, PortalHook.GROUP_INVITATION, params);

            verify(portalNotificationService).create(
                eq(executionContext),
                eq(PortalHook.GROUP_INVITATION),
                argThat(users -> new HashSet<>(users).equals(Set.of("user-direct", "user-from-group"))),
                eq(params)
            );
        }
    }

    @Nested
    class TriggerApiHookWithReference {

        @Test
        @SneakyThrows
        void should_lookup_configs_when_api_hook_targets_api_product_reference() {
            final Map<String, Object> params = Map.of();
            final ExecutionContext executionContext = new ExecutionContext("org-1", "env-1");

            when(
                portalNotificationConfigRepository.findByReferenceAndHook(
                    ApiHook.API_UPDATED.name(),
                    NotificationReferenceType.API_PRODUCT,
                    "product-1"
                )
            ).thenReturn(Collections.emptyList());

            when(
                genericNotificationConfigRepository.findByReferenceAndHook(
                    ApiHook.API_UPDATED.name(),
                    NotificationReferenceType.API_PRODUCT,
                    "product-1"
                )
            ).thenReturn(Collections.emptyList());

            cut.trigger(executionContext, ApiHook.API_UPDATED, NotificationReferenceType.API_PRODUCT, "product-1", params);

            verify(genericNotificationConfigRepository).findByReferenceAndHook(
                ApiHook.API_UPDATED.name(),
                NotificationReferenceType.API_PRODUCT,
                "product-1"
            );
            verify(portalNotificationConfigRepository).findByReferenceAndHook(
                ApiHook.API_UPDATED.name(),
                NotificationReferenceType.API_PRODUCT,
                "product-1"
            );
        }
    }

    @Nested
    class TriggerApiProductHook {

        @Test
        @SneakyThrows
        void should_lookup_configs_for_api_product_hook() {
            final Map<String, Object> params = Map.of();
            final ExecutionContext executionContext = new ExecutionContext("org-1", "env-1");

            when(
                portalNotificationConfigRepository.findByReferenceAndHook(
                    ApiProductHook.API_PRODUCT_UPDATED.name(),
                    NotificationReferenceType.API_PRODUCT,
                    "product-1"
                )
            ).thenReturn(Collections.emptyList());

            when(
                genericNotificationConfigRepository.findByReferenceAndHook(
                    ApiProductHook.API_PRODUCT_UPDATED.name(),
                    NotificationReferenceType.API_PRODUCT,
                    "product-1"
                )
            ).thenReturn(Collections.emptyList());

            cut.trigger(executionContext, ApiProductHook.API_PRODUCT_UPDATED, "product-1", params);

            verify(genericNotificationConfigRepository).findByReferenceAndHook(
                ApiProductHook.API_PRODUCT_UPDATED.name(),
                NotificationReferenceType.API_PRODUCT,
                "product-1"
            );
            verify(portalNotificationConfigRepository).findByReferenceAndHook(
                ApiProductHook.API_PRODUCT_UPDATED.name(),
                NotificationReferenceType.API_PRODUCT,
                "product-1"
            );
        }
    }
}
