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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.model.notification.NotificationTemplateEntity;
import io.gravitee.rest.api.model.notification.NotificationTemplateType;
import io.gravitee.rest.api.service.event.CommandEvent;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.v4.mapper.NotificationTemplateMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateCommandListenerImplTest {

    private NotificationTemplateCommandListenerImpl cut;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationTemplateMapper notificationTemplateMapper = new NotificationTemplateMapper();
    private static final String EMAIL_CONTENT = """
            {"id":"c4f45b5d-526e-4660-b45b-5d526ee660ec", "hook":"USER_REGISTRATION","scope":"TEMPLATES_FOR_ACTION","name":"User registration","description":"Email sent to a user who has self-registered on portal or admin console. Contains a registration link.","title":"User ${registrationAction} - ${user.displayName}","content":"email content","type":"EMAIL","enabled":true,"templateName":"TEMPLATES_FOR_ACTION.USER_REGISTRATION.EMAIL","created_at":1718895875127}
        """;
    private static final String ORGANIZATION_ID = "DEFAULT";

    @Mock
    private EventManager eventManager;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @BeforeEach
    void setUp() {
        cut = new NotificationTemplateCommandListenerImpl(
            eventManager,
            objectMapper,
            notificationTemplateService,
            notificationTemplateMapper
        );
    }

    @Test
    void should_do_nothing_when_event_is_null() {
        cut.onEvent(null);

        verifyNoInteractions(notificationTemplateService);
    }

    @Test
    void should_do_nothing_when_event_content_is_null() {
        cut.onEvent(new SimpleEvent<>(CommandEvent.TO_PROCESS, null));

        verifyNoInteractions(notificationTemplateService);
    }

    @Test
    void should_do_nothing_when_event_is_not_an_email_template_update() {
        var event = new SimpleEvent<>(CommandEvent.TO_PROCESS, buildCommand(List.of(CommandTags.SUBSCRIPTION_FAILURE), "content"));

        cut.onEvent(event);

        verifyNoInteractions(notificationTemplateService);
    }

    @Test
    void should_update_email_template() {
        var event = new SimpleEvent<>(CommandEvent.TO_PROCESS, buildCommand(List.of(CommandTags.EMAIL_TEMPLATE_UPDATE), EMAIL_CONTENT));

        cut.onEvent(event);

        var templateCaptor = ArgumentCaptor.forClass(NotificationTemplateEntity.class);
        verify(notificationTemplateService).updateFreemarkerCache(templateCaptor.capture(), eq(ORGANIZATION_ID));

        var template = templateCaptor.getValue();
        assertThat(template).isNotNull();
        assertThat(template.getId()).isEqualTo("c4f45b5d-526e-4660-b45b-5d526ee660ec");
        assertThat(template.getHook()).isEqualTo("USER_REGISTRATION");
        assertThat(template.getScope()).isEqualTo("TEMPLATES_FOR_ACTION");
        assertThat(template.getName()).isEqualTo("User registration");
        assertThat(template.getDescription()).isEqualTo(
            "Email sent to a user who has self-registered on portal or admin console. Contains a registration link."
        );
        assertThat(template.getTitle()).isEqualTo("User ${registrationAction} - ${user.displayName}");
        assertThat(template.getContent()).isEqualTo("email content");
        assertThat(template.getType()).isEqualTo(NotificationTemplateType.EMAIL);
        assertThat(template.isEnabled()).isTrue();
        assertThat(template.getTemplateName()).isEqualTo("TEMPLATES_FOR_ACTION.USER_REGISTRATION.EMAIL");
    }

    private static CommandEntity buildCommand(List<CommandTags> tags, String content) {
        CommandEntity commandEntity = new CommandEntity();
        commandEntity.setId("command-id");
        commandEntity.setTags(tags);
        commandEntity.setContent(content);
        commandEntity.setOrganizationId(ORGANIZATION_ID);
        return commandEntity;
    }
}
