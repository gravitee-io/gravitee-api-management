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
package io.gravitee.gateway.services.heartbeat;

import com.hazelcast.topic.ITopic;
import io.gravitee.repository.management.model.Event;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HeartbeatThread implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatThread.class);

    private final ITopic<Event> topic;
    private final Event event;

    HeartbeatThread(ITopic<Event> topic, Event event) {
        this.topic = topic;
        this.event = event;
    }

    @Override
    public void run() {
        LOGGER.debug("Run monitor for gateway at {}", new Date());

        try {
            // Update heartbeat timestamp
            event.setUpdatedAt(new Date());
            event.getProperties().put(HeartbeatService.EVENT_LAST_HEARTBEAT_PROPERTY, Long.toString(event.getUpdatedAt().getTime()));
            topic.publish(event);
        } catch (Exception ex) {
            LOGGER.error("An unexpected error occurs while monitoring the gateway", ex);
        }
    }
}
