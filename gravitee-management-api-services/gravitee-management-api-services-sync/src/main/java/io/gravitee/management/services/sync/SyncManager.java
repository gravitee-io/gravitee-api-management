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
package io.gravitee.management.services.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SyncManager {

    private final Logger logger = LoggerFactory.getLogger(SyncManager.class);

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ApiManager apiManager;

    @Autowired
    private ObjectMapper objectMapper;

    private long lastRefreshAt = -1;

    public void refresh() {
        logger.debug("Refreshing state...");

        try {
            // Extract all registered APIs
            Map<String, io.gravitee.repository.management.model.Api> apis = apiRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(io.gravitee.repository.management.model.Api::getId, api -> api));

            // Get last event for each API
            Map<String, Event> events = new HashMap<>();
            apis.keySet().forEach(api -> {
                Event event = getLastEvent(api);
                events.put(api, event);
            });

            // Determine API which must be stopped and stop them
            events.entrySet()
                    .stream()
                    .filter(apiEvent -> {
                        Event event = apiEvent.getValue();
                        return event != null &&
                                (event.getType() == EventType.STOP_API || event.getType() == EventType.UNPUBLISH_API);
                    })
                    .forEach(apiEvent -> apiManager.undeploy(apiEvent.getKey()));

            // Determine API which must be deployed
            events.entrySet()
                    .stream()
                    .filter(apiEvent -> {
                        Event event = apiEvent.getValue();
                        return event != null && (
                                event.getType() == EventType.START_API || event.getType() == EventType.PUBLISH_API);
                    })
                    .forEach(apiEvent -> {
                        try {
                            // Read API definition from event
                            io.gravitee.repository.management.model.Api payloadApi = objectMapper.readValue(
                                    apiEvent.getValue().getPayload(), io.gravitee.repository.management.model.Api.class);

                            // API to deploy
                            ApiEntity apiToDeploy = convert(payloadApi);

                            if (apiToDeploy != null) {
                                // Get deployed API
                                ApiEntity deployedApi = apiManager.get(apiToDeploy.getId());

                                // API is not yet deployed, so let's do it !
                                if (deployedApi == null) {
                                    apiManager.deploy(apiToDeploy);
                                } else {
                                    if (deployedApi.getDeployedAt().before(apiToDeploy.getDeployedAt())) {
                                        apiManager.update(apiToDeploy);
                                    }
                                }
                            }
                        } catch (IOException ioe) {
                            logger.error("Error while determining deployed APIs store into events payload", ioe);
                        }
                    });

            lastRefreshAt = System.currentTimeMillis();
        } catch (TechnicalException te) {
            logger.error("Unable to sync instance", te);
        }
    }

    private Event getLastEvent(String api) {
        EventCriteria eventCriteria;
        if (lastRefreshAt == -1) {
            eventCriteria = new EventCriteria.Builder()
                    .property(Event.EventProperties.API_ID.getValue(), api)
                    .build();
        } else {
            eventCriteria = new EventCriteria.Builder()
                    .property(Event.EventProperties.API_ID.getValue(), api)
                    .from(lastRefreshAt).to(System.currentTimeMillis())
                    .build();
        }

        List<Event> events = eventRepository.search(eventCriteria,
                new PageableBuilder().pageNumber(0).pageSize(1).build()).getContent();
        return (!events.isEmpty()) ? events.get(0) : null;
    }

    private ApiEntity convert(Api api) {
        ApiEntity apiEntity = new ApiEntity();

        apiEntity.setId(api.getId());
        apiEntity.setName(api.getName());
        apiEntity.setDeployedAt(api.getDeployedAt());
        apiEntity.setCreatedAt(api.getCreatedAt());

        if (api.getDefinition() != null) {
            try {
                io.gravitee.definition.model.Api apiDefinition = objectMapper.readValue(api.getDefinition(),
                        io.gravitee.definition.model.Api.class);

                apiEntity.setProxy(apiDefinition.getProxy());
                apiEntity.setPaths(apiDefinition.getPaths());
                apiEntity.setServices(apiDefinition.getServices());
                apiEntity.setResources(apiDefinition.getResources());
                apiEntity.setProperties(apiDefinition.getProperties());
                apiEntity.setTags(apiDefinition.getTags());
                if (apiDefinition.getPathMappings() != null) {
                    apiEntity.setPathMappings(new HashSet<>(apiDefinition.getPathMappings().keySet()));
                }
            } catch (IOException ioe) {
                logger.error("Unexpected error while generating API definition", ioe);
            }
        }
        apiEntity.setUpdatedAt(api.getUpdatedAt());
        apiEntity.setVersion(api.getVersion());
        apiEntity.setDescription(api.getDescription());
        apiEntity.setPicture(api.getPicture());
        apiEntity.setViews(api.getViews());

        final LifecycleState lifecycleState = api.getLifecycleState();
        if (lifecycleState != null) {
            apiEntity.setState(Lifecycle.State.valueOf(lifecycleState.name()));
        }
        if (api.getVisibility() != null) {
            apiEntity.setVisibility(io.gravitee.management.model.Visibility.valueOf(api.getVisibility().toString()));
        }

        return apiEntity;
    }

    public void setApiRepository(ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    public void setEventRepository(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void setApiManager(ApiManager apiManager) {
        this.apiManager = apiManager;
    }
}
