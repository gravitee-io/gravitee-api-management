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
import io.gravitee.definition.model.command.SubscriptionFailureNotificationCommand;
import io.gravitee.rest.api.model.command.CommandEntity;
import io.gravitee.rest.api.model.command.CommandTags;
import io.gravitee.rest.api.service.SubscriptionCommandListener;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.event.CommandEvent;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SubscriptionFailureNotificationCommandListenerImpl implements SubscriptionCommandListener {

    public static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionFailureNotificationCommandListenerImpl.class);

    private final SubscriptionService subscriptionService;

    private final ObjectMapper objectMapper;

    public SubscriptionFailureNotificationCommandListenerImpl(
        EventManager eventManager,
        SubscriptionService subscriptionService,
        ObjectMapper objectMapper
    ) {
        this.subscriptionService = subscriptionService;
        this.objectMapper = objectMapper;
        eventManager.subscribeForEvents(this, CommandEvent.class);
    }

    @Override
    public void onEvent(Event<CommandEvent, CommandEntity> event) {
        if (
            CommandEvent.TO_PROCESS.equals(event.type()) &&
            event.content() != null &&
            event.content().getTags().contains(CommandTags.SUBSCRIPTION_FAILURE_NOTIFICATION_RETRY)
        ) {
            LOGGER.debug("Command event: {}", event.content().getContent());
            getSubscriptionCommand(event)
                .ifPresent(command -> subscriptionService.notifyError(command.getSubscriptionId(), command.getFailureCause()));
        }
    }

    private Optional<SubscriptionFailureNotificationCommand> getSubscriptionCommand(Event<CommandEvent, CommandEntity> event) {
        try {
            if (event.content().getContent() == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(event.content().getContent(), SubscriptionFailureNotificationCommand.class));
        } catch (JsonProcessingException e) {
            LOGGER.error("Error processing SubscriptionCommand", e);
            return Optional.empty();
        }
    }
}
