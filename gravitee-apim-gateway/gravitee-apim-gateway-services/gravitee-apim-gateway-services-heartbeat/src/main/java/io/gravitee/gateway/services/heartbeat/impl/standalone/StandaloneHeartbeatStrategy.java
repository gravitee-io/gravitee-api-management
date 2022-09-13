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
package io.gravitee.gateway.services.heartbeat.impl.standalone;

import static io.gravitee.gateway.services.heartbeat.HeartbeatService.EVENT_LAST_HEARTBEAT_PROPERTY;
import static io.gravitee.gateway.services.heartbeat.HeartbeatService.EVENT_STOPPED_AT_PROPERTY;

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.gateway.services.heartbeat.HeartbeatStrategy;
import io.gravitee.gateway.services.heartbeat.spring.configuration.HeartbeatStrategyConfiguration;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandaloneHeartbeatStrategy implements HeartbeatStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandaloneHeartbeatStrategy.class);

    private ExecutorService executorService;
    private final EventRepository eventRepository;
    private final int delay;
    private final TimeUnit unit;
    private Event heartbeatEvent;

    // We make this field protected to be able to test easily without relying on ExecutorService
    @VisibleForTesting
    protected Event eventToCreate;

    public StandaloneHeartbeatStrategy(HeartbeatStrategyConfiguration heartbeatStrategyConfiguration) {
        this.delay = heartbeatStrategyConfiguration.getDelay();
        this.unit = heartbeatStrategyConfiguration.getUnit();
        this.eventRepository = heartbeatStrategyConfiguration.getEventRepository();
    }

    @Override
    public void doStart(Event eventToCreate) throws Exception {
        LOGGER.info("Start gateway heartbeat service");

        this.eventToCreate = eventToCreate;

        executorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "gio-heartbeat"));

        LOGGER.info("Gateway heartbeat service scheduled with fixed delay {} {}", delay, unit);

        ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(this::sendHeartbeatEvent, 0, delay, unit);

        LOGGER.info("Start gateway heartbeat service: DONE");
    }

    @Override
    public void preStop() {
        if (heartbeatEvent != null) {
            sendStopHeartbeatEvent();
        }
        LOGGER.debug("Pre-stopping gateway heartbeat service");
    }

    @Override
    public void doStop() {
        if (!executorService.isShutdown()) {
            LOGGER.info("Stop gateway heartbeat service");
            executorService.shutdownNow();
        } else {
            LOGGER.info("Gateway heartbeat service already shut-downed");
        }
        LOGGER.info("Stop gateway heartbeat service: DONE");
    }

    @VisibleForTesting
    void sendHeartbeatEvent() {
        LOGGER.debug("Sending heartbeat event");

        try {
            if (heartbeatEvent == null) {
                heartbeatEvent = eventRepository.createOrPatch(eventToCreate);
            } else {
                // If heartbeatEvent is known, it means it has already been saved in database, so we can update it with a lite version
                eventRepository.createOrPatch(buildLiteEvent(heartbeatEvent));
            }
        } catch (Exception ex) {
            LOGGER.warn(
                "An error occurred while trying to create the heartbeat event id[{}] type[{}]",
                eventToCreate.getId(),
                eventToCreate.getType(),
                ex
            );
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
        Date updatedAt = new Date();
        eventLite.setUpdatedAt(updatedAt);
        eventLite.setType(heartbeatEvent.getType());
        eventLite.setEnvironments(heartbeatEvent.getEnvironments());
        eventLite.setProperties(new HashMap<>());
        eventLite.getProperties().put(EVENT_LAST_HEARTBEAT_PROPERTY, Long.toString(updatedAt.getTime()));
        return eventLite;
    }

    private void sendStopHeartbeatEvent() {
        if (heartbeatEvent == null) {
            return;
        }

        heartbeatEvent.setType(EventType.GATEWAY_STOPPED);
        heartbeatEvent.getProperties().put(EVENT_STOPPED_AT_PROPERTY, Long.toString(new Date().getTime()));

        try {
            heartbeatEvent = eventRepository.update(heartbeatEvent);
        } catch (Exception ex) {
            LOGGER.warn(
                "An error occurred while trying to update the STOP heartbeat event id[{}] type[{}]",
                heartbeatEvent.getId(),
                heartbeatEvent.getType(),
                ex
            );
        }
    }
}
