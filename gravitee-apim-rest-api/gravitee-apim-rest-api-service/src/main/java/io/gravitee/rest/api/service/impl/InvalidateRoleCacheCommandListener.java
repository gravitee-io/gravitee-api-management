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
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.rest.api.model.InvalidateRoleCacheCommandEntity;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.event.CommandEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InvalidateRoleCacheCommandListener implements EventListener<CommandEvent, CommandEntity> {

    private final ObjectMapper objectMapper;
    private final MembershipService membershipService;

    public InvalidateRoleCacheCommandListener(ObjectMapper objectMapper, MembershipService membershipService, EventManager eventManager) {
        this.objectMapper = objectMapper;
        this.membershipService = membershipService;
        eventManager.subscribeForEvents(this, CommandEvent.class);
    }

    @Override
    public void onEvent(Event<CommandEvent, CommandEntity> event) {
        if (
            event != null &&
            CommandEvent.TO_PROCESS.equals(event.type()) &&
            event.content() != null &&
            event.content().getTags().contains(CommandTags.GROUP_DEFAULT_ROLES_UPDATE)
        ) {
            String content = event.content().getContent();
            log.debug("Command event: {}", content);

            InvalidateRoleCacheCommandEntity eventData;
            try {
                eventData = objectMapper.readValue(content, InvalidateRoleCacheCommandEntity.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize map of properties {}", content, e);
                return;
            }

            if (eventData != null) {
                membershipService.invalidateRoleCache(
                    eventData.getReferenceType(),
                    eventData.getReferenceId(),
                    eventData.getMemberType(),
                    eventData.getMemberId()
                );
            }
        }
    }
}
