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
package io.gravitee.gateway.services.heartbeat.impl.hazelcast;

import static io.gravitee.gateway.services.heartbeat.HeartbeatService.*;

import io.gravitee.gateway.services.heartbeat.HeartbeatStrategy;
import io.gravitee.gateway.services.heartbeat.spring.configuration.HeartbeatStrategyConfiguration;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.message.Message;
import io.gravitee.node.api.message.MessageConsumer;
import io.gravitee.node.api.message.MessageProducer;
import io.gravitee.node.api.message.Topic;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastHeartbeatStrategy implements HeartbeatStrategy, MessageConsumer<Event> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HazelcastHeartbeatStrategy.class);
    public static final String HEARTBEATS = "heartbeats";
    public static final String HEARTBEATS_FAILURE = "heartbeats-failure";

    private ExecutorService executorService;

    private final ClusterManager clusterManager;

    private final EventRepository eventRepository;

    private final int delay;

    private final TimeUnit unit;

    // How to avoid duplicate
    private final Topic<Event> topic;

    private final java.util.UUID subscriptionId;

    private Event heartbeatEvent;

    public HazelcastHeartbeatStrategy(
        HeartbeatStrategyConfiguration heartbeatStrategyConfiguration,
        ClusterManager clusterManager,
        MessageProducer messageProducer
    ) {
        this.clusterManager = clusterManager;
        this.eventRepository = heartbeatStrategyConfiguration.getEventRepository();
        this.delay = heartbeatStrategyConfiguration.getDelay();
        this.unit = heartbeatStrategyConfiguration.getUnit();
        this.topic = messageProducer.getTopic(HEARTBEATS);
        this.subscriptionId = topic.addMessageConsumer(this);
    }

    @Override
    public void doStart(Event eventToCreate) {
        LOGGER.info("Start gateway heartbeat");

        heartbeatEvent = eventToCreate;

        topic.publish(heartbeatEvent);

        executorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "gio-heartbeat"));

        HeartbeatThread heartbeatThread = new HeartbeatThread(topic, heartbeatEvent);

        LOGGER.info("Monitoring scheduled with fixed delay {} {}", delay, unit);

        ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(heartbeatThread, delay, delay, unit);

        LOGGER.info("Start gateway heartbeat: DONE");
    }

    @Override
    public void onMessage(Message<Event> message) {
        if (clusterManager.isMasterNode()) {
            Event event = message.getMessageObject();
            try {
                eventRepository.createOrPatch(event);
            } catch (Exception ex) {
                LOGGER.warn(
                    "An error occurred while trying to create or update the heartbeat event id[{}] type[{}]",
                    event.getId(),
                    event.getType(),
                    ex
                );
            }
        }
    }

    @Override
    public void preStop() {
        heartbeatEvent.setType(EventType.GATEWAY_STOPPED);
        heartbeatEvent.getProperties().put(EVENT_STOPPED_AT_PROPERTY, Long.toString(new Date().getTime()));
        LOGGER.debug("Pre-stopping Heartbeat Service");
        LOGGER.debug("Sending a {} event", heartbeatEvent.getType());

        topic.publish(heartbeatEvent);

        topic.removeMessageConsumer(subscriptionId);
    }

    @Override
    public void doStop() {
        if (!executorService.isShutdown()) {
            LOGGER.info("Stop gateway monitor");
            executorService.shutdownNow();
        } else {
            LOGGER.info("Gateway monitor already shut-downed");
        }

        heartbeatEvent.setType(EventType.GATEWAY_STOPPED);
        heartbeatEvent.getProperties().put(EVENT_STOPPED_AT_PROPERTY, Long.toString(new Date().getTime()));
        LOGGER.debug("Sending a {} event", heartbeatEvent.getType());

        topic.publish(heartbeatEvent);

        topic.removeMessageConsumer(subscriptionId);

        LOGGER.info("Stop gateway monitor : DONE");
    }
}
