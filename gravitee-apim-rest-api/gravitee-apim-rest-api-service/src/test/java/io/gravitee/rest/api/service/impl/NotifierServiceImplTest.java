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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notifiers.EmailNotifierService;
import io.gravitee.rest.api.service.notifiers.WebhookNotifierService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class NotifierServiceImplTest {

    @Mock
    private GenericNotificationConfigRepository genericNotificationConfigRepository;

    @Mock
    private EmailNotifierService emailNotifierService;

    @Mock
    private WebhookNotifierService webhookNotifierService;

    @InjectMocks
    private NotifierServiceImpl notifierService;

    @Nested
    class TriggerEmail {

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

            notifierService.triggerGenericNotifications(
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
