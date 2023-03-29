/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.heartbeat.impl;

import static io.gravitee.gateway.services.heartbeat.HeartbeatService.EVENT_LAST_HEARTBEAT_PROPERTY;

import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.messaging.Message;
import io.gravitee.node.api.cluster.messaging.MessageListener;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import java.util.Date;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class HeartbeatEventListener implements MessageListener<Event> {

    private final ClusterManager clusterManager;

    private final EventRepository eventRepository;
    private boolean initialEvent = true;

    @Override
    public void onMessage(Message<Event> message) {
        if (clusterManager.self().primary()) {
            Event event = message.content();
            try {
                if (initialEvent) {
                    eventRepository.createOrPatch(event);
                    initialEvent = false;
                } else {
                    // If heartbeatEvent is known, it means it has already been saved in database, so we can update it with a lite version
                    eventRepository.createOrPatch(buildLiteEvent(event));
                }
            } catch (Exception ex) {
                log.warn(
                    "An error occurred while trying to create or update the heartbeat event id[{}] type[{}]",
                    event.getId(),
                    event.getType(),
                    ex
                );
            }
        }
    }

    /**
     * Provides a lite version of the Heartbeat Event to only update date related fields
     * @param heartbeatEvent the original heartbeat event
     * @return the lite database
     */
    private Event buildLiteEvent(Event heartbeatEvent) {
        Event eventLite = new Event();
        eventLite.setId(heartbeatEvent.getId());
        eventLite.setType(heartbeatEvent.getType());
        Date updatedAt = new Date();
        eventLite.setUpdatedAt(updatedAt);
        eventLite.setProperties(Map.of(EVENT_LAST_HEARTBEAT_PROPERTY, Long.toString(updatedAt.getTime())));
        return eventLite;
    }
}
