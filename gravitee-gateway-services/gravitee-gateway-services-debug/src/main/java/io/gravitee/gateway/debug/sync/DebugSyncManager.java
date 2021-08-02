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
package io.gravitee.gateway.debug.sync;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.gateway.debug.handler.definition.DebugApi;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.services.sync.SyncManager;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugSyncManager extends SyncManager {

    private final Logger logger = LoggerFactory.getLogger(DebugSyncManager.class);

    protected void refresh(List<String> environments) {
        long nextLastRefreshAt = System.currentTimeMillis();
        boolean error = false;
        if (clusterManager.isMasterNode() || (!clusterManager.isMasterNode() && !distributed)) {
            logger.debug("Synchronization #{} started at {}", counter.incrementAndGet(), Instant.now().toString());
            logger.debug("Refreshing gateway state...");

            try {
                synchronizeApis(nextLastRefreshAt, environments);
            } catch (Exception ex) {
                error = true;
                logger.error("An error occurs while synchronizing debug APIs", ex);
            }

            logger.debug("Synchronization #{} ended at {}", counter.get(), Instant.now().toString());
        }

        // If there was no error during the sync process, let's continue it with the next period of time
        if (!error) {
            // We refresh the date even if process did not run (not a master node) to ensure that we sync the same way as
            // soon as the node is becoming the master later.
            lastRefreshAt = nextLastRefreshAt;
        }
    }

    @Override
    protected void computeApiEvents(Map<String, Event> apiEvents) {
        apiEvents.forEach(
            (apiId, apiEvent) -> {
                try {
                    switch (apiEvent.getType()) {
                        case DEBUG_API:
                            try {
                                // Read API definition from event
                                io.gravitee.definition.model.DebugApi eventPayload = objectMapper.readValue(
                                    apiEvent.getPayload(),
                                    io.gravitee.definition.model.DebugApi.class
                                );

                                DebugApi debugApi = new DebugApi(apiEvent.getId(), eventPayload);
                                debugApi.setEnabled(true);
                                debugApi.setDeployedAt(new Date());
                                enhanceWithData(debugApi);

                                if (!debugApi.getPlans().isEmpty()) {
                                    eventManager.publishEvent(ReactorEvent.DEBUG, debugApi);
                                } else {
                                    logger.info("No plan for API, skipping debug");
                                }
                            } catch (Exception e) {
                                logger.error("Error while determining deployed APIs store into events payload", e);
                            }
                            break;
                    }
                } catch (Throwable t) {
                    logger.error("An unexpected error occurs while managing the deployment of API id[{}]", apiId, t);
                }
            }
        );
    }

    @Override
    protected void enhanceWithData(Api definition) {
        // Managing only v2 for debug mode
        if (definition.getDefinitionVersion() == DefinitionVersion.V2) {
            definition.setPlans(
                definition
                    .getPlans()
                    .stream()
                    .filter(plan -> "staging".equalsIgnoreCase(plan.getStatus()) || "published".equalsIgnoreCase(plan.getStatus()))
                    .collect(Collectors.toList())
            );
        }
    }

    @Override
    public EventCriteria.Builder getLastApiEventCriteria(String api, List<String> environments) {
        return super
            .getLastApiEventCriteria(api, environments)
            .property(Event.EventProperties.API_DEBUG_STATUS.getValue(), ApiDebugStatus.TO_DEBUG.name());
    }

    @Override
    public EventCriteria.Builder getLatestApiEventsCriteria(long nextLastRefreshAt, List<String> environments) {
        return super
            .getLatestApiEventsCriteria(nextLastRefreshAt, environments)
            .property(Event.EventProperties.API_DEBUG_STATUS.getValue(), ApiDebugStatus.TO_DEBUG.name());
    }

    protected EventType[] getApiEventTypes() {
        return new EventType[] { EventType.DEBUG_API };
    }
}
