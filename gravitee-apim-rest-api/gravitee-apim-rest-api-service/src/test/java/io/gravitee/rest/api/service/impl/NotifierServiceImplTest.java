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
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.EmailRecipientsService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.notification.ApiHook;
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

    @BeforeEach
    void setUp() {
        cut =
            new NotifierServiceImpl(
                notifierManager,
                portalNotificationConfigRepository,
                portalNotificationService,
                genericNotificationConfigRepository,
                emailNotifierService,
                webhookNotifierService,
                emailRecipientsService,
                parameterService
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
            )
                .thenReturn(
                    List.of(
                        GenericNotificationConfig
                            .builder()
                            .notifier("default-email")
                            .id(DEFAULT_EMAIL_NOTIFIER_ID)
                            .config("${(api.primaryOwner.email)!''};second.mail@gio.test")
                            .build()
                    )
                );
            when(emailRecipientsService.processTemplatedRecipients(List.of("${(api.primaryOwner.email)!''};second.mail@gio.test"), params))
                .thenReturn(Set.of("recipient@gio.test", "second.mail@gio.test"));
            when(parameterService.findAsBoolean(executionContext, Key.TRIAL_INSTANCE, ParameterReferenceType.SYSTEM)).thenReturn(false);

            cut.triggerGenericNotifications(
                executionContext,
                ApiHook.API_STARTED,
                NotificationReferenceType.API,
                "api-id",
                params,
                List.of()
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
            )
                .thenReturn(
                    List.of(
                        GenericNotificationConfig
                            .builder()
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
            )
                .thenReturn(Set.of("recipient@gio.test", "second.mail-not-opted-in@gio.test"));
            when(parameterService.findAsBoolean(executionContext, Key.TRIAL_INSTANCE, ParameterReferenceType.SYSTEM)).thenReturn(true);
            when(
                emailRecipientsService.filterRegisteredUser(
                    executionContext,
                    Set.of("recipient@gio.test", "second.mail-not-opted-in@gio.test")
                )
            )
                .thenReturn(Set.of("recipient@gio.test"));

            cut.triggerGenericNotifications(
                executionContext,
                ApiHook.API_STARTED,
                NotificationReferenceType.API,
                "api-id",
                params,
                List.of()
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
            )
                .thenReturn(
                    List.of(
                        GenericNotificationConfig
                            .builder()
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
            )
                .thenReturn(Set.of("additional@gio.test", "recipient@gio.test", "second.mail-not-opted-in@gio.test"));
            when(parameterService.findAsBoolean(executionContext, Key.TRIAL_INSTANCE, ParameterReferenceType.SYSTEM)).thenReturn(true);
            when(
                emailRecipientsService.filterRegisteredUser(
                    eq(executionContext),
                    argThat(a -> a.containsAll(List.of("recipient@gio.test", "additional@gio.test", "second.mail-not-opted-in@gio.test")))
                )
            )
                .thenReturn(Set.of("additional@gio.test", "recipient@gio.test"));

            cut.triggerGenericNotifications(
                executionContext,
                ApiHook.API_STARTED,
                NotificationReferenceType.API,
                "api-id",
                params,
                List.of(new Recipient("default-email", "additional@gio.test"))
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
            )
                .thenReturn(Collections.emptyList());

            cut.triggerGenericNotifications(
                executionContext,
                ApiHook.API_STARTED,
                NotificationReferenceType.API,
                "api-id",
                params,
                List.of()
            );

            verifyNoInteractions(emailNotifierService);
            verifyNoInteractions(webhookNotifierService);
        }
    }
}
