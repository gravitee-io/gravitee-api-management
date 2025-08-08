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
import static io.gravitee.gateway.services.heartbeat.HeartbeatService.EVENT_STOPPED_AT_PROPERTY;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.messaging.Topic;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeartbeatEventScheduler extends AbstractService<HeartbeatEventScheduler> {

    public static final String HEARTBEATS = "heartbeats";
    private final ClusterManager clusterManager;
    private final int delay;
    private final TimeUnit unit;
    private final Topic<Event> topic;
    private final String subscriptionId;
    private final Event heartbeatEvent;

    @Getter
    private final HeartbeatEventListener heartbeatEventListener;

    private ScheduledExecutorService executorService;

    public HeartbeatEventScheduler(
        final ClusterManager clusterManager,
        final EventRepository eventRepository,
        final int delay,
        final TimeUnit delayUnit,
        final Event heartbeatEvent
    ) {
        this.clusterManager = clusterManager;
        this.delay = delay;
        this.unit = delayUnit;
        this.topic = clusterManager.topic(HEARTBEATS);
        this.heartbeatEvent = heartbeatEvent;
        this.heartbeatEventListener = new HeartbeatEventListener(clusterManager, eventRepository);
        this.subscriptionId = topic.addMessageListener(heartbeatEventListener);
    }

    @Override
    protected void doStart() {
        log.info("Starting gateway heartbeat");

        // Publish initial event
        topic.publish(heartbeatEvent);

        log.info("Monitoring scheduled with fixed delay {} {}", delay, unit);
        executorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "gio-heartbeat"));
        executorService.scheduleWithFixedDelay(new HeartbeatEventPublisher(clusterManager, topic, heartbeatEvent), delay, delay, unit);

        log.info("Start gateway heartbeat done successfully");
    }

    @Override
    public HeartbeatEventScheduler preStop() {
        heartbeatEvent.setType(EventType.GATEWAY_STOPPED);
        heartbeatEvent.setUpdatedAt(new Date());
        if (heartbeatEvent.getProperties() == null) {
            heartbeatEvent.setProperties(new HashMap<>());
        }
        heartbeatEvent.getProperties().put(EVENT_CLUSTER_PRIMARY_NODE_PROPERTY, Boolean.toString(clusterManager.self().primary()));
        heartbeatEvent.getProperties().put(EVENT_STOPPED_AT_PROPERTY, Long.toString(heartbeatEvent.getUpdatedAt().getTime()));
        log.debug("Sending a {} event", heartbeatEvent.getType());
        topic.publish(heartbeatEvent);
        return this;
    }

    @Override
    protected void doStop() {
        if (executorService != null && !executorService.isShutdown()) {
            log.info("Stop gateway monitor");
            executorService.shutdownNow();
        } else {
            log.info("Gateway monitor already shut-downed");
        }

        if (heartbeatEventListener != null) {
            heartbeatEventListener.shutdownNow();
        }

        topic.removeMessageListener(subscriptionId);

        log.info("Stop gateway monitor : DONE");
    }
}
