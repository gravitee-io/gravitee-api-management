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
import io.gravitee.definition.model.command.ApiDeploymentFailureCommand;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.service.event.CommandEvent;
import io.gravitee.rest.api.service.v4.ApiStateService;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class ApiDeploymentFailureCommandListenerImpl implements EventListener<CommandEvent, CommandEntity> {

    private final ApiStateService apiStateService;

    private final ObjectMapper objectMapper;

    public ApiDeploymentFailureCommandListenerImpl(EventManager eventManager, ApiStateService apiStateService, ObjectMapper objectMapper) {
        this.apiStateService = apiStateService;
        this.objectMapper = objectMapper;
        eventManager.subscribeForEvents(this, CommandEvent.class);
    }

    @Override
    public void onEvent(Event<CommandEvent, CommandEntity> event) {
        if (
            CommandEvent.TO_PROCESS.equals(event.type()) &&
            event.content() != null &&
            event.content().getTags().contains(CommandTags.API_DEPLOYMENT_FAILURE)
        ) {
            log.debug("Command event: {}", event.content().getContent());
            getApiDeploymentFailureCommand(event).ifPresent(apiStateService::deploymentFail);
        }
    }

    private Optional<ApiDeploymentFailureCommand> getApiDeploymentFailureCommand(Event<CommandEvent, CommandEntity> event) {
        try {
            if (event.content().getContent() == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(event.content().getContent(), ApiDeploymentFailureCommand.class));
        } catch (JsonProcessingException e) {
            log.error("Error processing SubscriptionCommand", e);
            return Optional.empty();
        }
    }
}
