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

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.rest.api.model.notification.NotificationTemplateCommandEntity;
import io.gravitee.rest.api.model.notification.NotificationTemplateEntity;
import io.gravitee.rest.api.model.notification.NotificationTemplateType;
import java.time.Instant;
import java.util.Date;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

public class NotificationTemplateMapperTest {

    private final NotificationTemplateMapper cut = new NotificationTemplateMapper();

    @Test
    void should_map_entity_to_command() {
        var now = Instant.now();

        var entity = aNotificationTemplateEntity(now);

        var command = cut.toNotificationTemplateCommandEntity(entity);

        assertEquals(entity.getId(), command.getId());
        assertEquals(entity.getHook(), command.getHook());
        assertEquals(entity.getScope(), command.getScope());
        assertEquals(entity.getName(), command.getName());
        assertEquals(entity.getDescription(), command.getDescription());
        assertEquals(entity.getTitle(), command.getTitle());
        assertEquals(entity.getContent(), command.getContent());
        assertEquals(entity.getType(), command.getType());
        assertEquals(entity.getCreatedAt(), command.getCreatedAt());
        assertEquals(entity.getUpdatedAt(), command.getUpdatedAt());
        assertEquals(entity.isEnabled(), command.getEnabled());
        assertEquals(entity.getTemplateName(), command.getTemplateName());
    }

    @Test
    void should_map_command_to_entity() {
        var now = Instant.now();

        var command = aNotificationTemplateCommandEntity(now);

        var entity = cut.toNotificationTemplateEntity(command);

        assertEquals(command.getId(), entity.getId());
        assertEquals(command.getHook(), entity.getHook());
        assertEquals(command.getScope(), entity.getScope());
        assertEquals(command.getName(), entity.getName());
        assertEquals(command.getDescription(), entity.getDescription());
        assertEquals(command.getTitle(), entity.getTitle());
        assertEquals(command.getContent(), entity.getContent());
        assertEquals(command.getType(), entity.getType());
        assertEquals(command.getCreatedAt(), entity.getCreatedAt());
        assertEquals(command.getUpdatedAt(), entity.getUpdatedAt());
        assertEquals(command.getEnabled(), entity.isEnabled());
        assertEquals(command.getTemplateName(), entity.getTemplateName());
    }

    private NotificationTemplateCommandEntity aNotificationTemplateCommandEntity(Instant now) {
        return NotificationTemplateCommandEntity.builder()
            .id("1")
            .hook("hook")
            .scope("scope")
            .name("name")
            .description("description")
            .title("title")
            .content("content")
            .type(NotificationTemplateType.EMAIL)
            .createdAt(Date.from(now))
            .updatedAt(Date.from(now))
            .enabled(false)
            .templateName("template name")
            .build();
    }

    private static @NotNull NotificationTemplateEntity aNotificationTemplateEntity(Instant now) {
        var entity = new NotificationTemplateEntity();
        entity.setId("1");
        entity.setHook("hook");
        entity.setScope("scope");
        entity.setName("name");
        entity.setDescription("description");
        entity.setTitle("title");
        entity.setContent("content");
        entity.setType(NotificationTemplateType.EMAIL);
        entity.setCreatedAt(Date.from(now));
        entity.setUpdatedAt(Date.from(now));
        entity.setEnabled(false);
        entity.setTemplateName("template name");
        return entity;
    }
}
