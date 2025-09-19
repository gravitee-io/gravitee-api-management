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

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notifiers.impl.EmailNotifierServiceImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class EmailNotifierServiceTest {

    @Mock
    private EmailService mockEmailService;

    private EmailNotifierServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmailNotifierServiceImpl(mockEmailService);
    }

    @Nested
    class Trigger {

        @Test
        void should_send_email_based_on_hook() {
            // Given
            var executionContext = new ExecutionContext(null, null);
            var templateData = Map.<String, Object>of();
            var recipientEmail = "recipient1@gravitee.io";

            // When
            service.trigger(executionContext, ApiHook.API_STARTED, templateData, List.of(recipientEmail));

            // Then
            verify(mockEmailService).sendAsyncEmailNotification(
                same(executionContext),
                eq(
                    new EmailNotificationBuilder()
                        .to(recipientEmail)
                        .template(EmailNotificationBuilder.EmailTemplate.API_API_STARTED)
                        .params(templateData)
                        .build()
                )
            );
        }

        @Test
        void should_do_nothing_when_no_hook_is_provided() {
            service.trigger(GraviteeContext.getExecutionContext(), null, Map.of(), List.of());
            verifyNoInteractions(mockEmailService);
        }
    }
}
