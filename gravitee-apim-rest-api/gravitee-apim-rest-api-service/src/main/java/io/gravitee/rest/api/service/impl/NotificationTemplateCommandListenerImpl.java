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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventManager;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.model.notification.NotificationTemplateCommandEntity;
import io.gravitee.rest.api.service.NotificationTemplateCommandListener;
import io.gravitee.rest.api.service.event.CommandEvent;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.v4.mapper.NotificationTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationTemplateCommandListenerImpl implements NotificationTemplateCommandListener {

    private final ObjectMapper objectMapper;
    private final NotificationTemplateService notificationTemplateService;
    private final NotificationTemplateMapper notificationTemplateMapper;

    public NotificationTemplateCommandListenerImpl(
        EventManager eventManager,
        ObjectMapper objectMapper,
        NotificationTemplateService notificationTemplateService,
        NotificationTemplateMapper notificationTemplateMapper
    ) {
        this.objectMapper = objectMapper;
        this.notificationTemplateService = notificationTemplateService;
        this.notificationTemplateMapper = notificationTemplateMapper;
        eventManager.subscribeForEvents(this, CommandEvent.class);
    }

    @Override
    public void onEvent(Event<CommandEvent, CommandEntity> event) {
        if (
            event != null &&
            CommandEvent.TO_PROCESS.equals(event.type()) &&
            event.content() != null &&
            event.content().getTags().contains(CommandTags.EMAIL_TEMPLATE_UPDATE)
        ) {
            log.debug("Command event: {}", event.content().getContent());
            var notificationTemplate = getNotificationTemplate(event.content().getContent());
            var organizationId = event.content().getOrganizationId();
            if (notificationTemplate != null && organizationId != null) {
                notificationTemplateService.updateFreemarkerCache(
                    notificationTemplateMapper.toNotificationTemplateEntity(notificationTemplate),
                    organizationId
                );
            }
        }
    }

    private NotificationTemplateCommandEntity getNotificationTemplate(String content) {
        try {
            return objectMapper.readValue(content, NotificationTemplateCommandEntity.class);
        } catch (JsonProcessingException e) {
            log.error("Error processing NotificationTemplateCommand", e);
            return null;
        }
    }
}
