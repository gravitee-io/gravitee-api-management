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

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.messaging.Message;
import io.gravitee.node.api.cluster.messaging.MessageListener;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class HeartbeatEventListener implements MessageListener<Event> {

    private final ClusterManager clusterManager;
    private final EventRepository eventRepository;

    private final ExecutorService heartbeatExecutor;

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    /**
     * Once the final event of the node has been persisted, any heartbeat still in flight on the topic must be ignored:
     * persisting it would bring the node back to a started state.
     */
    private volatile boolean closed = false;

    public HeartbeatEventListener(final ClusterManager clusterManager, final EventRepository eventRepository) {
        this(clusterManager, eventRepository, Executors.newSingleThreadExecutor(r -> new Thread(r, "gio-heartbeat-listener")));
    }

    /**
     * Lets the test provide the executor the events are processed on, so it can wait for a submitted task to be fully
     * completed instead of relying on timings.
     */
    @VisibleForTesting
    HeartbeatEventListener(
        final ClusterManager clusterManager,
        final EventRepository eventRepository,
        final ExecutorService heartbeatExecutor
    ) {
        this.clusterManager = clusterManager;
        this.eventRepository = eventRepository;
        this.heartbeatExecutor = heartbeatExecutor;
    }

    @Override
    public void onMessage(Message<Event> message) {
        if (closed) {
            return;
        }
        if (clusterManager.self().primary()) {
            Event event = message.content();

            // Check if already processing a heartbeat event
            if (!isProcessing.compareAndSet(false, true)) {
                log.warn(
                    "Discarding heartbeat event id[{}] type[{}] - another heartbeat event is already being processed",
                    event.getId(),
                    event.getType()
                );
                return;
            }

            heartbeatExecutor.submit(() -> {
                try {
                    if (!closed) {
                        eventRepository.createOrPatch(event);
                    }
                } catch (Exception ex) {
                    log.warn(
                        "An error occurred while trying to create or update the heartbeat event id[{}] type[{}]",
                        event.getId(),
                        event.getType(),
                        ex
                    );
                } finally {
                    isProcessing.set(false);
                }
            });
        }
    }

    /**
     * Persists the final event of this node and waits for the write to complete, so it is not lost when the node stops
     * right after. The write is queued after the one being processed, if any, instead of being discarded. Every event
     * received afterwards is ignored.
     *
     * @param event the final event to persist
     * @param timeout the maximum time to wait for the write
     * @param unit the unit of the timeout
     */
    public void persistFinalEvent(final Event event, final long timeout, final TimeUnit unit) {
        closed = true;
        try {
            heartbeatExecutor.submit(() -> eventRepository.createOrPatch(event)).get(timeout, unit);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while persisting the final heartbeat event id[{}] type[{}]", event.getId(), event.getType(), ex);
        } catch (ExecutionException | TimeoutException | RejectedExecutionException ex) {
            log.warn("Unable to persist the final heartbeat event id[{}] type[{}]", event.getId(), event.getType(), ex);
        }
    }

    public void shutdownNow() {
        if (!heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdownNow();
        }
    }
}
