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
import io.gravitee.rest.api.model.InvalidateParameterCacheCommandEntity;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.event.CommandEvent;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class InvalidateParameterCacheCommandListener implements EventListener<CommandEvent, CommandEntity> {

    private final ObjectMapper objectMapper;
    private final ParameterService parameterService;

    public InvalidateParameterCacheCommandListener(
        ObjectMapper objectMapper,
        ParameterService parameterService,
        EventManager eventManager
    ) {
        this.objectMapper = objectMapper;
        this.parameterService = parameterService;
        eventManager.subscribeForEvents(this, CommandEvent.class);
    }

    @Override
    public void onEvent(Event<CommandEvent, CommandEntity> event) {
        if (
            event != null &&
            CommandEvent.TO_PROCESS.equals(event.type()) &&
            event.content() != null &&
            event.content().getTags().contains(CommandTags.PARAMETER_CACHE_UPDATE)
        ) {
            String content = event.content().getContent();
            log.debug("Command event: {}", content);

            InvalidateParameterCacheCommandEntity eventData;
            try {
                eventData = objectMapper.readValue(content, InvalidateParameterCacheCommandEntity.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize parameter cache invalidation data {}", content, e);
                return;
            }

            if (eventData != null) {
                parameterService.invalidateCache(eventData.getKey(), eventData.getReferenceId(), eventData.getReferenceType());
            }
        }
    }
}
