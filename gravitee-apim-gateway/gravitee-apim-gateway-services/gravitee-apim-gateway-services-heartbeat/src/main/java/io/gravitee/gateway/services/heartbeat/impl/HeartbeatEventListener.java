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

import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.messaging.Message;
import io.gravitee.node.api.cluster.messaging.MessageListener;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Event;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private final ExecutorService heartbeatExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "gio-heartbeat-listener"));

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    @Override
    public void onMessage(Message<Event> message) {
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
                    eventRepository.createOrPatch(event);
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

    public void shutdownNow() {
        if (!heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdownNow();
        }
    }
}
