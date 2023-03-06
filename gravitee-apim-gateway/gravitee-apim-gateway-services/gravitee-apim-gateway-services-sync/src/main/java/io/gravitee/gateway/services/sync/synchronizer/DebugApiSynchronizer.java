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
package io.gravitee.gateway.services.sync.synchronizer;

import static io.gravitee.gateway.services.sync.SyncManager.TIMEFRAME_AFTER_DELAY;
import static io.gravitee.gateway.services.sync.SyncManager.TIMEFRAME_BEFORE_DELAY;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.impl.ReactableWrapper;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugApiSynchronizer extends AbstractService<Synchronizer> implements Synchronizer {

    private final Logger logger = LoggerFactory.getLogger(DebugApiSynchronizer.class);
    private final EventRepository eventRepository;
    private final EventManager eventManager;
    private final Node node;
    private final boolean isDebugModeAvailable;

    public DebugApiSynchronizer(
        EventRepository eventRepository,
        EventManager eventManager,
        PluginRegistry pluginRegistry,
        Configuration configuration,
        Node node
    ) {
        this.eventRepository = eventRepository;
        this.eventManager = eventManager;
        this.node = node;

        this.isDebugModeAvailable =
            pluginRegistry.plugins().stream().map(Plugin::id).anyMatch("gateway-debug"::equalsIgnoreCase) &&
            configuration.getProperty("gravitee.services.gateway-debug.enabled", Boolean.class, true);
    }

    public void synchronize(Long lastRefreshAt, Long nextLastRefreshAt, List<String> environments) {
        if (isDebugModeAvailable) {
            final long start = System.currentTimeMillis();
            if (lastRefreshAt != -1) {
                EventCriteria.Builder criteriaBuilder = new EventCriteria.Builder()
                    .types(EventType.DEBUG_API)
                    .property(Event.EventProperties.API_DEBUG_STATUS.getValue(), ApiDebugStatus.TO_DEBUG.name())
                    .from(lastRefreshAt == null ? 0 : lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
                    .to(nextLastRefreshAt == null ? 0 : nextLastRefreshAt + TIMEFRAME_AFTER_DELAY)
                    .environments(environments);

                List<String> events = eventRepository
                    .search(criteriaBuilder.build())
                    .stream()
                    .filter(event -> node.id().equals(event.getProperties().get(Event.EventProperties.GATEWAY_ID.getValue())))
                    .map(event -> {
                        eventManager.publishEvent(ReactorEvent.DEBUG, new ReactableWrapper(event));
                        return event.getId();
                    })
                    .collect(Collectors.toList());
                logger.debug("{} debug apis synchronized in {}ms", events.size(), (System.currentTimeMillis() - start));
            }
        }
    }
}
