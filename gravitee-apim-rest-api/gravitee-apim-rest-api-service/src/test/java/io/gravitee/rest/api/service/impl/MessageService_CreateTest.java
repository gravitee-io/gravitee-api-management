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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.MessageChannel;
import io.gravitee.rest.api.model.MessageEntity;
import io.gravitee.rest.api.model.MessageRecipientEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.HttpClientService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.AbstractManagementException;
import io.gravitee.rest.api.service.exceptions.MessageDeliveryFailedException;
import io.gravitee.rest.api.service.exceptions.MessageUrlForbiddenException;
import io.gravitee.rest.api.service.exceptions.NotifierDisabledException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import io.vertx.core.buffer.Buffer;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MessageService_CreateTest {

    private static final String API_ID = "api-id";
    private static final String TARGET_URL = "http://internal.example:8080/admin";
    private static final String TARGET_BODY = "{\"secret\":\"internal-only\"}";

    @InjectMocks
    private MessageServiceImpl messageService = new MessageServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private HttpClientService httpClientService;

    @Mock
    private AuditService auditService;

    @Mock
    private ApiTemplateService apiTemplateService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private Environment environment;

    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        GraviteeContext.setCurrentOrganization("DEFAULT");
        GraviteeContext.setCurrentEnvironment("DEFAULT");
        executionContext = GraviteeContext.getExecutionContext();
        ReflectionTestUtils.setField(messageService, "httpEnabled", true);
        ReflectionTestUtils.setField(messageService, "httpWhitelist", List.of());
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    void should_throw_message_delivery_failed_when_http_target_answers_error() {
        when(httpClientService.request(anyString(), eq(HttpMethod.POST), eq(TARGET_URL), anyMap(), any(), any())).thenThrow(
            new TechnicalManagementException(
                new TechnicalManagementException(
                    " Error on url '" + TARGET_URL + "'. Status code: 401. Message: " + TARGET_BODY,
                    null
                ).toString()
            )
        );

        assertThatThrownBy(() -> messageService.create(executionContext, httpMessage()))
            .isInstanceOf(MessageDeliveryFailedException.class)
            .satisfies(MessageService_CreateTest::assertCleanDeliveryFailure);
    }

    @Test
    void should_throw_message_delivery_failed_when_http_target_unreachable() {
        when(httpClientService.request(anyString(), eq(HttpMethod.POST), eq(TARGET_URL), anyMap(), any(), any())).thenThrow(
            new TechnicalManagementException(
                "io.netty.channel.AbstractChannel$AnnotatedConnectException: Connection refused: internal.example/10.0.0.1:8080",
                new ConnectException("Connection refused")
            )
        );

        assertThatThrownBy(() -> messageService.create(executionContext, httpMessage()))
            .isInstanceOf(MessageDeliveryFailedException.class)
            .satisfies(MessageService_CreateTest::assertCleanDeliveryFailure);
    }

    @Test
    void should_throw_message_delivery_failed_when_api_message_http_target_fails() throws Exception {
        var api = new Api();
        api.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(httpClientService.request(anyString(), eq(HttpMethod.POST), eq(TARGET_URL), anyMap(), any(), any())).thenThrow(
            new TechnicalManagementException(" Error on url '" + TARGET_URL + "'. Status code: 500. Message: " + TARGET_BODY, null)
        );

        assertThatThrownBy(() -> messageService.create(executionContext, API_ID, httpMessage()))
            .isInstanceOf(MessageDeliveryFailedException.class)
            .satisfies(MessageService_CreateTest::assertCleanDeliveryFailure);
        verifyNoInteractions(auditService);
    }

    @Test
    void should_send_http_message_and_count_one_recipient_when_target_answers_success() {
        when(httpClientService.request(anyString(), eq(HttpMethod.POST), eq(TARGET_URL), anyMap(), any(), any())).thenReturn(
            Buffer.buffer("ok")
        );

        assertThat(messageService.create(executionContext, httpMessage())).isEqualTo(1);
        verify(auditService).createAuditLog(eq(executionContext), any());
    }

    @Test
    void should_keep_forbidden_error_when_url_not_whitelisted() {
        ReflectionTestUtils.setField(messageService, "httpWhitelist", List.of("https://allowed.example"));

        assertThatThrownBy(() -> messageService.create(executionContext, httpMessage())).isInstanceOf(MessageUrlForbiddenException.class);
        verifyNoInteractions(httpClientService);
    }

    @Test
    void should_keep_notifier_disabled_error_when_http_notifier_disabled() {
        ReflectionTestUtils.setField(messageService, "httpEnabled", false);

        assertThatThrownBy(() -> messageService.create(executionContext, httpMessage())).isInstanceOf(NotifierDisabledException.class);
        verifyNoInteractions(httpClientService);
    }

    private static MessageEntity httpMessage() {
        var recipient = new MessageRecipientEntity();
        recipient.setUrl(TARGET_URL);
        var message = new MessageEntity();
        message.setChannel(MessageChannel.HTTP);
        message.setText("hello");
        message.setParams(Map.of());
        message.setRecipient(recipient);
        return message;
    }

    private static void assertCleanDeliveryFailure(Throwable thrown) {
        var exception = (AbstractManagementException) thrown;
        assertThat(exception.getHttpStatusCode()).isEqualTo(400);
        assertThat(exception.getTechnicalCode()).isEqualTo("message.delivery.failed");
        assertThat(exception.getMessage())
            .isEqualTo("The target URL could not be reached or did not accept the message.")
            .doesNotContain(TARGET_URL, "Status code", TARGET_BODY, "Exception");
    }
}
