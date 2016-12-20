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
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public void refresh() {
        logger.debug("Refreshing state...");

        try {
            Set<io.gravitee.repository.management.model.Api> apis = apiRepository.findAll();

            // Determine deployed APIs store into events payload
            Set<ApiEntity> deployedApis = getDeployedApis(apis);

            Map<String, ApiEntity> apisMap = deployedApis.stream()
                    .filter(api -> api != null)
                    .collect(Collectors.toMap(ApiEntity::getId, api -> api));

            // Determine APIs to undeploy
            Set<String> apiToRemove = apiManager.apis().stream()
                    .filter(api -> !apisMap.containsKey(api.getId()))
                    .map(ApiEntity::getId)
                    .collect(Collectors.toSet());

            apiToRemove.forEach(apiManager::undeploy);

            // Determine APIs to update
            apisMap.keySet().stream()
                    .filter(apiId -> apiManager.get(apiId) != null)
                    .forEach(apiId -> {
                        // Get local cached API
                        ApiEntity deployedApi = apiManager.get(apiId);

                        // Get API from store
                        ApiEntity remoteApi = apisMap.get(apiId);

                        if (deployedApi.getDeployedAt().before(remoteApi.getDeployedAt())) {
                            apiManager.update(remoteApi);
                        }
                    });

            // Determine APIs to deploy
            apisMap.keySet().stream()
                    .filter(api -> apiManager.get(api) == null)
                    .forEach(api -> {
                        ApiEntity newApi = apisMap.get(api);
                        apiManager.deploy(newApi);
                    });
        } catch (TechnicalException te) {
            logger.error("Unable to sync instance", te);
        }
    }

    private Set<ApiEntity> getDeployedApis(Set<io.gravitee.repository.management.model.Api> apis) {
        Set<ApiEntity> deployedApis = new HashSet<>();

        List<Event> events = eventRepository.search(
                new EventCriteria.Builder().types(
                        EventType.PUBLISH_API,
                        EventType.UNPUBLISH_API,
                        EventType.START_API,
                        EventType.STOP_API).build());

        if (events != null && !events.isEmpty()) {
            try {
                for (io.gravitee.repository.management.model.Api api : apis) {
                    for (Event _event : events) {
                        if (api.getId().equals(_event.getProperties().get(Event.EventProperties.API_ID.getValue()))) {
                            if (!EventType.UNPUBLISH_API.equals(_event.getType())) {
                                io.gravitee.repository.management.model.Api payloadApi = objectMapper.readValue(
                                        _event.getPayload(), io.gravitee.repository.management.model.Api.class);
                                deployedApis.add(convert(payloadApi));
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error while determining deployed APIs store into events payload", e);
            }
        }

        return deployedApis;
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
