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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventManager;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.event.CommandEvent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvalidateRoleCacheCommandListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MembershipService membershipService;

    @Mock
    private EventManager eventManager;

    @InjectMocks
    private InvalidateRoleCacheCommandListener listener;

    @BeforeEach
    void setUp() {
        listener = new InvalidateRoleCacheCommandListener(objectMapper, membershipService, eventManager);
    }

    @Test
    void should_invalidate_role_cache_when_valid_event() {
        // given
        String jsonContent = "{\"referenceType\":\"ORG\",\"referenceId\":\"org-1\",\"memberType\":\"USER\",\"memberId\":\"user-1\"}";
        CommandEntity commandEntity = mock(CommandEntity.class);
        when(commandEntity.getTags()).thenReturn(List.of(CommandTags.GROUP_DEFAULT_ROLES_UPDATE));
        when(commandEntity.getContent()).thenReturn(jsonContent);
        Event<CommandEvent, CommandEntity> event = mock(Event.class);
        when(event.type()).thenReturn(CommandEvent.TO_PROCESS);
        when(event.content()).thenReturn(commandEntity);

        // when
        listener.onEvent(event);

        // then
        verify(membershipService).invalidateRoleCache("ORG", "org-1", "USER", "user-1");
    }

    @Test
    void should_not_invalidate_cache_when_event_is_null() {
        // when
        listener.onEvent(null);

        // then
        verifyNoInteractions(membershipService);
    }

    @Test
    void should_not_invalidate_cache_when_wrong_event_type() {
        // given
        Event<CommandEvent, CommandEntity> event = mock(Event.class);
        when(event.type()).thenReturn(null);

        // when
        listener.onEvent(event);

        // then
        verifyNoInteractions(membershipService);
    }

    @Test
    void should_not_invalidate_cache_when_content_is_null() {
        // given
        Event<CommandEvent, CommandEntity> event = mock(Event.class);
        when(event.type()).thenReturn(CommandEvent.TO_PROCESS);
        when(event.content()).thenReturn(null);

        // when
        listener.onEvent(event);

        // then
        verifyNoInteractions(membershipService);
    }

    @Test
    void should_not_invalidate_cache_when_tags_do_not_match() {
        // given
        CommandEntity commandEntity = mock(CommandEntity.class);
        when(commandEntity.getTags()).thenReturn(List.of(CommandTags.EMAIL_TEMPLATE_UPDATE)); // not GROUP_DEFAULT_ROLES_UPDATE
        Event<CommandEvent, CommandEntity> event = mock(Event.class);
        when(event.type()).thenReturn(CommandEvent.TO_PROCESS);
        when(event.content()).thenReturn(commandEntity);

        // when
        listener.onEvent(event);

        // then
        verifyNoInteractions(membershipService);
    }

    @Test
    void should_throw_when_invalid_content() {
        // given
        String jsonContent = "{\"invalid_json\"}";
        CommandEntity commandEntity = mock(CommandEntity.class);
        when(commandEntity.getTags()).thenReturn(List.of(CommandTags.GROUP_DEFAULT_ROLES_UPDATE));
        when(commandEntity.getContent()).thenReturn(jsonContent);
        Event<CommandEvent, CommandEntity> event = mock(Event.class);
        when(event.type()).thenReturn(CommandEvent.TO_PROCESS);
        when(event.content()).thenReturn(commandEntity);

        // when
        listener.onEvent(event);

        // then
        verifyNoInteractions(membershipService);
    }
}
