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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.notifier.NotifierPlugin;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.model.GenericNotificationConfig;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.service.EmailRecipientsService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PortalNotificationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notifiers.EmailNotifierService;
import io.gravitee.rest.api.service.notifiers.WebhookNotifierService;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class NotifierServiceImplAdditionalRecipientsTest {

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

    @Test
    @SneakyThrows
    void should_email_additional_recipients_along_with_application_email_configs() {
        final Map<String, Object> params = Map.of();
        final ExecutionContext executionContext = new ExecutionContext("org-1", "env-1");

        when(
            genericNotificationConfigRepository.findByReferenceAndHook(
                ApplicationHook.SUBSCRIPTION_ACCEPTED.name(),
                NotificationReferenceType.APPLICATION,
                "app-id"
            )
        ).thenReturn(
            List.of(GenericNotificationConfig.builder().notifier(DEFAULT_EMAIL_NOTIFIER_ID).id("cfg").config("creator@gio.test").build())
        );
        when(emailRecipientsService.processTemplatedRecipients(any(), eq(params))).thenAnswer(inv ->
            new HashSet<String>(inv.<Collection<String>>getArgument(0))
        );

        cut.trigger(
            executionContext,
            ApplicationHook.SUBSCRIPTION_ACCEPTED,
            "app-id",
            params,
            List.of(new Recipient("EMAIL", "subscriber@gio.test"))
        );

        final ArgumentCaptor<Collection<String>> recipientsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(emailNotifierService).trigger(
            eq(executionContext),
            eq(ApplicationHook.SUBSCRIPTION_ACCEPTED),
            eq(params),
            recipientsCaptor.capture()
        );
        assertThat(recipientsCaptor.getValue()).containsExactlyInAnyOrder("creator@gio.test", "subscriber@gio.test");
    }

    @Test
    @SneakyThrows
    void should_email_additional_recipients_when_application_has_no_notification_config() {
        final Map<String, Object> params = Map.of();
        final ExecutionContext executionContext = new ExecutionContext("org-1", "env-1");

        when(
            genericNotificationConfigRepository.findByReferenceAndHook(
                ApplicationHook.SUBSCRIPTION_REJECTED.name(),
                NotificationReferenceType.APPLICATION,
                "app-id"
            )
        ).thenReturn(Collections.emptyList());
        when(emailRecipientsService.processTemplatedRecipients(List.of("subscriber@gio.test"), params)).thenReturn(
            Set.of("subscriber@gio.test")
        );

        cut.trigger(
            executionContext,
            ApplicationHook.SUBSCRIPTION_REJECTED,
            "app-id",
            params,
            List.of(new Recipient("EMAIL", "subscriber@gio.test"))
        );

        final ArgumentCaptor<Collection<String>> recipientsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(emailNotifierService).trigger(
            eq(executionContext),
            eq(ApplicationHook.SUBSCRIPTION_REJECTED),
            eq(params),
            recipientsCaptor.capture()
        );
        assertThat(recipientsCaptor.getValue()).containsExactly("subscriber@gio.test");
        verifyNoInteractions(webhookNotifierService);
    }
}
