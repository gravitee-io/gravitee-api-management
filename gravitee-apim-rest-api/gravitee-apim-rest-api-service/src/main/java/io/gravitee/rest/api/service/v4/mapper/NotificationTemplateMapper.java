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
package io.gravitee.rest.api.service.v4.mapper;

import io.gravitee.rest.api.model.notification.NotificationTemplateCommandEntity;
import io.gravitee.rest.api.model.notification.NotificationTemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateMapper {

    public NotificationTemplateCommandEntity toNotificationTemplateCommandEntity(NotificationTemplateEntity notificationTemplate) {
        return NotificationTemplateCommandEntity.builder()
            .id(notificationTemplate.getId())
            .hook(notificationTemplate.getHook())
            .scope(notificationTemplate.getScope())
            .name(notificationTemplate.getName())
            .description(notificationTemplate.getDescription())
            .title(notificationTemplate.getTitle())
            .content(notificationTemplate.getContent())
            .type(notificationTemplate.getType())
            .createdAt(notificationTemplate.getCreatedAt())
            .updatedAt(notificationTemplate.getUpdatedAt())
            .enabled(notificationTemplate.isEnabled())
            .templateName(notificationTemplate.getTemplateName())
            .build();
    }

    public NotificationTemplateEntity toNotificationTemplateEntity(NotificationTemplateCommandEntity notificationTemplate) {
        var notificationTemplateEntity = new NotificationTemplateEntity();
        notificationTemplateEntity.setId(notificationTemplate.getId());
        notificationTemplateEntity.setHook(notificationTemplate.getHook());
        notificationTemplateEntity.setScope(notificationTemplate.getScope());
        notificationTemplateEntity.setName(notificationTemplate.getName());
        notificationTemplateEntity.setDescription(notificationTemplate.getDescription());
        notificationTemplateEntity.setTitle(notificationTemplate.getTitle());
        notificationTemplateEntity.setContent(notificationTemplate.getContent());
        notificationTemplateEntity.setType(notificationTemplate.getType());
        notificationTemplateEntity.setCreatedAt(notificationTemplate.getCreatedAt());
        notificationTemplateEntity.setUpdatedAt(notificationTemplate.getUpdatedAt());
        notificationTemplateEntity.setEnabled(notificationTemplate.getEnabled());
        notificationTemplateEntity.setTemplateName(notificationTemplate.getTemplateName());
        return notificationTemplateEntity;
    }
}
