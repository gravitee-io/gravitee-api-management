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
package io.gravitee.gateway.services.heartbeat.impl;

import static io.gravitee.gateway.services.heartbeat.HeartbeatService.EVENT_CLUSTER_PRIMARY_NODE_PROPERTY;
import static io.gravitee.gateway.services.heartbeat.HeartbeatService.EVENT_LAST_HEARTBEAT_PROPERTY;

import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.messaging.Topic;
import io.gravitee.repository.management.model.Event;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class HeartbeatEventPublisher implements Runnable {

    private final ClusterManager clusterManager;
    private final Topic<Event> topic;
    private final Event event;

    @Override
    public void run() {
        log.debug("Run monitor for gateway at {}", new Date());
        try {
            synchronized (event) {
                event.setUpdatedAt(new Date());
                if (event.getProperties() == null) {
                    event.setProperties(new HashMap<>());
                }
                event.getProperties().put(EVENT_LAST_HEARTBEAT_PROPERTY, Long.toString(event.getUpdatedAt().getTime()));
                event.getProperties().put(EVENT_CLUSTER_PRIMARY_NODE_PROPERTY, Boolean.toString(clusterManager.self().primary()));
                topic.publish(buildLiteEvent(event));
            }
        } catch (Exception ex) {
            log.error("An unexpected error occurs while monitoring the gateway", ex);
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
        eventLite.setUpdatedAt(heartbeatEvent.getUpdatedAt());
        eventLite.setProperties(
            Map.of(
                EVENT_LAST_HEARTBEAT_PROPERTY,
                heartbeatEvent.getProperties().get(EVENT_LAST_HEARTBEAT_PROPERTY),
                EVENT_CLUSTER_PRIMARY_NODE_PROPERTY,
                heartbeatEvent.getProperties().get(EVENT_CLUSTER_PRIMARY_NODE_PROPERTY)
            )
        );
        return eventLite;
    }
}
