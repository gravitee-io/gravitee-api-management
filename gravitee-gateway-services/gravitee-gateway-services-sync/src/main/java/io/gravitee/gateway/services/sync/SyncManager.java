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
package io.gravitee.gateway.services.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Path;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.definition.Plan;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SyncManager {

    public static final String TAGS_PROP = "tags";

    private final Logger logger = LoggerFactory.getLogger(SyncManager.class);

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ApiManager apiManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${tags:}")
    private String propertyTags;

    public void refresh() {
        logger.debug("Refreshing gateway state...");

        try {
            Set<io.gravitee.repository.management.model.Api> apis = apiRepository.findAll();
            
            // Determine deployed APIs store into events payload
            Set<Api> deployedApis = getDeployedApis(apis);

            Map<String, Api> apisMap = deployedApis.stream()
                    .filter(api -> api != null && isConfiguredTags(api))
                    .collect(Collectors.toMap(Api::getId, api -> api));

            // Determine APIs to undeploy
            Set<String> apiToRemove = apiManager.apis().stream()
                    .filter(api -> !apisMap.containsKey(api.getId()) || !isConfiguredTags(api))
                    .map(Api::getId)
                    .collect(Collectors.toSet());

            apiToRemove.stream().forEach(apiManager::undeploy);

            // Determine APIs to update
            apisMap.keySet().stream()
                    .filter(apiId -> apiManager.get(apiId) != null)
                    .forEach(apiId -> {
                        // Get local cached API
                        Api deployedApi = apiManager.get(apiId);

                        // Get API from store
                        Api remoteApi = apisMap.get(apiId);

                        if (deployedApi.getDeployedAt().before(remoteApi.getDeployedAt())) {
                            apiManager.update(remoteApi);
                        }
                    });

            // Determine APIs to deploy
            apisMap.keySet().stream()
                    .filter(api -> apiManager.get(api) == null)
                    .forEach(api -> {
                        Api newApi = apisMap.get(api);
                        apiManager.deploy(newApi);
                    });
        } catch (TechnicalException te) {
            logger.error("Unable to sync instance", te);
        }
    }

    private boolean isConfiguredTags(Api api) {
        final String systemPropertyTags = System.getProperty(TAGS_PROP);
        final String tags = systemPropertyTags == null ? propertyTags : systemPropertyTags;
        if (tags != null && !tags.isEmpty()) {
            if (api.getTags() != null) {
                final List<String> tagList = Arrays.asList(tags.split(","));
                final boolean isTagConfigured = tagList.stream()
                        .anyMatch(tag -> api.getTags().stream()
                                .anyMatch(apiTag -> {
                                    final Collator collator = Collator.getInstance();
                                    collator.setStrength(Collator.NO_DECOMPOSITION);
                                    return collator.compare(tag, apiTag) == 0;
                                })
                        );
                if (!isTagConfigured) {
                    logger.info("The API {} has been ignored because not in configured tags {}", api.getName(), tags);
                }
                return isTagConfigured;
            }
            logger.info("Tags {} are configured on gateway instance but not found on the API {}", tags, api.getName());
            return false;
        }
        // no tags configured on this gateway instance
        return true;
    }
    
    private Set<Api> getDeployedApis(Set<io.gravitee.repository.management.model.Api> apis) {
        Set<Api> deployedApis = new HashSet<>();

        List<Event> events = eventRepository.search(
                    new EventCriteria.Builder().types(
                            EventType.PUBLISH_API,
                            EventType.UNPUBLISH_API,
                            EventType.START_API,
                            EventType.STOP_API).build());

        if (events != null && ! events.isEmpty()) {
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

    private Api convert(io.gravitee.repository.management.model.Api remoteApi) {
        try {
            String definition = remoteApi.getDefinition();
            if (definition != null && !definition.isEmpty()) {
                Api api = objectMapper.readValue(definition, Api.class);

                api.setId(remoteApi.getId());
                api.setName(remoteApi.getName());
                api.setVersion(remoteApi.getVersion());
                api.setEnabled(remoteApi.getLifecycleState() == LifecycleState.STARTED);
                api.setDeployedAt(remoteApi.getUpdatedAt());

                try {
                    api.setPlans(planRepository.findByApi(api.getId())
                            .stream()
                            .map(this::convert)
                            .collect(Collectors.toList()));
                } catch (TechnicalException te) {
                    logger.error("Unexpected error while adding plan to the API: {} [{}]", remoteApi.getName(),
                            remoteApi.getId(), te);
                }
                return api;
            }
        } catch (IOException ioe) {
            logger.error("Unable to prepare API definition from repository", ioe);
        }

        return null;
    }

    private Plan convert(io.gravitee.repository.management.model.Plan repoPlan) {
        Plan plan = new Plan();

        plan.setId(repoPlan.getId());
        plan.setName(repoPlan.getName());

        try {
            if (repoPlan.getDefinition() != null && ! repoPlan.getDefinition().trim().isEmpty()) {
                HashMap<String, Path> paths = objectMapper.readValue(repoPlan.getDefinition(),
                        new TypeReference<HashMap<String, Path>>() {});

                plan.setPaths(paths);
            }
        } catch (IOException ioe) {
            logger.error("Unexpected error while converting plan: {}", plan, ioe);
        }

        return plan;
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
