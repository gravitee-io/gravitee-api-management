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

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.mcp.core.model.Notification;
import io.gravitee.apim.rest.api.mcp.core.port.Messaging;
import io.gravitee.rest.api.model.MessageChannel;
import io.gravitee.rest.api.model.MessageEntity;
import io.gravitee.rest.api.model.MessageRecipientEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MessageService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * APIM's messaging service: it resolves an API's consumers from subscriptions and memberships, sends over mail,
 * portal notifications or a webhook, and audits {@code MESSAGE_SENT} under the authenticated user.
 */
public class MessageServiceMessaging implements Messaging {

    private final MessageService messageService;
    private final UserService userService;

    public MessageServiceMessaging(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    @Override
    public List<Recipient> recipients(AuditInfo caller, String apiId, Notification notification) {
        if (notification.channel() == Notification.Channel.WEBHOOK) {
            return List.of();
        }
        var context = context(caller);
        return ApimContext.run(caller, () -> {
            var ids = messageService.getRecipientsId(context, apiOf(apiId), toEntity(notification));
            return userService
                .findByIds(context, ids)
                .stream()
                .map(user -> new Recipient(user.getId(), user.getDisplayName(), user.getEmail()))
                .sorted(Comparator.comparing(Recipient::name, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        });
    }

    @Override
    public int send(AuditInfo caller, String apiId, Notification notification) {
        return ApimContext.run(caller, () -> messageService.create(context(caller), apiId, toEntity(notification)));
    }

    /** The messaging service wants the API as a repository model and reads only its id. */
    private static io.gravitee.repository.management.model.Api apiOf(String apiId) {
        var api = new io.gravitee.repository.management.model.Api();
        api.setId(apiId);
        return api;
    }

    private static MessageEntity toEntity(Notification notification) {
        var entity = new MessageEntity();
        entity.setTitle(notification.subject());
        var recipient = new MessageRecipientEntity();
        switch (notification.channel()) {
            case PORTAL -> {
                entity.setChannel(MessageChannel.PORTAL);
                entity.setText(notification.text());
                recipient.setRoleScope(RoleScope.APPLICATION.name());
                recipient.setRoleValues(notification.audience().applicationRoles());
            }
            case MAIL -> {
                entity.setChannel(MessageChannel.MAIL);
                entity.setText(notification.text());
                recipient.setRoleScope(RoleScope.APPLICATION.name());
                recipient.setRoleValues(notification.audience().applicationRoles());
            }
            case WEBHOOK -> {
                // The service posts the text as the body: a Slack-style {"text": …} document, declared as JSON.
                entity.setChannel(MessageChannel.HTTP);
                entity.setText(slackBody(notification));
                entity.setParams(Map.of("Content-Type", "application/json"));
                recipient.setUrl(notification.webhookUrl());
            }
        }
        entity.setRecipient(recipient);
        return entity;
    }

    static String slackBody(Notification notification) {
        var text = notification.subject() == null ? notification.text() : "*" + notification.subject() + "*\n" + notification.text();
        return "{\"text\": \"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
    }

    private static ExecutionContext context(AuditInfo caller) {
        return new ExecutionContext(caller.organizationId(), caller.environmentId());
    }
}
