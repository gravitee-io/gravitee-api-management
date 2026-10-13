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
package io.gravitee.apim.rest.api.mcp.infra;

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.CALLER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.rest.api.mcp.core.model.Notification;
import io.gravitee.apim.rest.api.mcp.core.port.Messaging;
import io.gravitee.rest.api.model.MessageChannel;
import io.gravitee.rest.api.model.MessageEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.MessageService;
import io.gravitee.rest.api.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MessageServiceMessagingTest {

    private final MessageService messageService = mock(MessageService.class);
    private final UserService userService = mock(UserService.class);
    private final MessageServiceMessaging messaging = new MessageServiceMessaging(messageService, userService);

    @Test
    void should_ask_the_messaging_service_for_the_owners_of_the_subscribed_applications_and_name_them() {
        when(messageService.getRecipientsId(any(), any(io.gravitee.repository.management.model.Api.class), any())).thenReturn(
            Set.of("jane-id")
        );
        var jane = new UserEntity();
        jane.setId("jane-id");
        jane.setFirstname("Jane");
        jane.setLastname("Owner");
        jane.setEmail("jane@acme.example");
        when(userService.findByIds(any(), eq(Set.of("jane-id")))).thenReturn(Set.of(jane));
        var notification = new Notification(Notification.Audience.CONSUMER_OWNERS, Notification.Channel.MAIL, "Subject", "Body", null);

        var recipients = messaging.recipients(CALLER, "orders-id", notification);

        assertThat(recipients).containsExactly(new Messaging.Recipient("jane-id", "Jane Owner", "jane@acme.example"));
        var entity = ArgumentCaptor.forClass(MessageEntity.class);
        verify(messageService).getRecipientsId(any(), any(io.gravitee.repository.management.model.Api.class), entity.capture());
        assertThat(entity.getValue().getChannel()).isEqualTo(MessageChannel.MAIL);
        assertThat(entity.getValue().getRecipient().getRoleScope()).isEqualTo("APPLICATION");
        assertThat(entity.getValue().getRecipient().getRoleValues()).containsExactly("PRIMARY_OWNER");
        assertThat(entity.getValue().getTitle()).isEqualTo("Subject");
        assertThat(entity.getValue().getText()).isEqualTo("Body");
    }

    @Test
    void should_send_a_slack_shaped_json_body_to_a_webhook() {
        when(messageService.create(any(), eq("orders-id"), any())).thenReturn(1);
        var notification = new Notification(
            Notification.Audience.WEBHOOK,
            Notification.Channel.WEBHOOK,
            "Orders is rejecting Mobile app",
            "Since 14:02, every call answers 401: the key was revoked.",
            "https://hooks.slack.example/T0/B0/x"
        );

        assertThat(messaging.send(CALLER, "orders-id", notification)).isEqualTo(1);

        var entity = ArgumentCaptor.forClass(MessageEntity.class);
        verify(messageService).create(any(), eq("orders-id"), entity.capture());
        assertThat(entity.getValue().getChannel()).isEqualTo(MessageChannel.HTTP);
        assertThat(entity.getValue().getRecipient().getUrl()).isEqualTo("https://hooks.slack.example/T0/B0/x");
        assertThat(entity.getValue().getParams()).isEqualTo(Map.of("Content-Type", "application/json"));
        assertThat(entity.getValue().getText()).isEqualTo(
            "{\"text\": \"*Orders is rejecting Mobile app*\\nSince 14:02, every call answers 401: the key was revoked.\"}"
        );
        assertThat(messaging.recipients(CALLER, "orders-id", notification)).isEmpty();
    }

    @Test
    void should_escape_quotes_in_the_webhook_body() {
        var body = MessageServiceMessaging.slackBody(
            new Notification(Notification.Audience.WEBHOOK, Notification.Channel.WEBHOOK, null, "say \"hi\"", "u")
        );

        assertThat(body).isEqualTo("{\"text\": \"say \\\"hi\\\"\"}");
    }
}
