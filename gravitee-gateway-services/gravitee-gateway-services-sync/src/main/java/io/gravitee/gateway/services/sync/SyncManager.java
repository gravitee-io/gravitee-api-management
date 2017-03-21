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
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.definition.Plan;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.text.Collator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SyncManager {

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

    @Autowired
    private GatewayConfiguration gatewayConfiguration;

    private long lastRefreshAt = -1;

    public void refresh() {
        logger.debug("Refreshing gateway state...");

        try {
            // Extract all registered APIs
            Map<String, io.gravitee.repository.management.model.Api> apis = apiRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(io.gravitee.repository.management.model.Api::getId, api -> api));

            // Determine APIs to undeploy because of deployment tags
            Set<String> apisToRemove = apiManager.apis().stream()
                    .filter(api -> !hasMatchingTags(api))
                    .map(Api::getId)
                    .collect(Collectors.toSet());

            // Undeploy APIs not relative to this gateway instance (different deployment tags)
            apisToRemove.forEach(apiId -> {
                apiManager.undeploy(apiId);
                apis.remove(apiId);
            });

            // Remove api not relative to this gateway instance
            Set<String> apisToDeploy = apis.entrySet().stream()
                    .map(apiEntry -> {
                        Api api = new Api();
                        api.setId(apiEntry.getValue().getId());
                        api.setName(apiEntry.getValue().getName());
                        api.setTags(apiEntry.getValue().getTags());
                        return api;
                    })
                    .filter(this::hasMatchingTags)
                    .map(io.gravitee.definition.model.Api::getId)
                    .collect(Collectors.toSet());

            // Get last event for each API
            Map<String, Event> events = new HashMap<>();
            apisToDeploy.forEach(api -> {
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
                            Api apiToDeploy = convert(payloadApi);

                            if (apiToDeploy != null) {
                                // Get deployed API
                                Api deployedApi = apiManager.get(apiToDeploy.getId());

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

    private boolean hasMatchingTags(Api api) {
        final Optional<List<String>> optTagList = gatewayConfiguration.shardingTags();

        if (optTagList.isPresent()) {
            List<String> tagList = optTagList.get();
            if (api.getTags() != null) {
                final List<String> inclusionTags = tagList.stream()
                        .map(String::trim)
                        .filter(tag -> !tag.startsWith("!"))
                        .collect(Collectors.toList());

                final List<String> exclusionTags = tagList.stream()
                        .map(String::trim)
                        .filter(tag -> tag.startsWith("!"))
                        .map(tag -> tag.substring(1))
                        .collect(Collectors.toList());

                if (inclusionTags.stream()
                        .filter(exclusionTags::contains)
                        .count() > 0) {
                    throw new IllegalArgumentException("You must not configure a tag to be included and excluded");
                }

                final boolean hasMatchingTags =
                        inclusionTags.stream()
                                .anyMatch(tag -> api.getTags().stream()
                                        .anyMatch(apiTag -> {
                                            final Collator collator = Collator.getInstance();
                                            collator.setStrength(Collator.NO_DECOMPOSITION);
                                            return collator.compare(tag, apiTag) == 0;
                                        })
                                ) || (!exclusionTags.isEmpty() &&
                                exclusionTags.stream()
                                        .noneMatch(tag -> api.getTags().stream()
                                                .anyMatch(apiTag -> {
                                                    final Collator collator = Collator.getInstance();
                                                    collator.setStrength(Collator.NO_DECOMPOSITION);
                                                    return collator.compare(tag, apiTag) == 0;
                                                })
                                        ));

                if (!hasMatchingTags) {
                    logger.info("The API {} has been ignored because not in configured tags {}", api.getName(), tagList);
                }
                return hasMatchingTags;
            }
            logger.info("Tags {} are configured on gateway instance but not found on the API {}", tagList, api.getName());
            return false;
        }
        // no tags configured on this gateway instance
        return true;
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
                    // Deploy only published plan
                    api.setPlans(planRepository.findByApi(api.getId())
                            .stream()
                            .filter(plan -> plan.getStatus() == io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
                            .map(this::convert)
                            .collect(Collectors.toList()));
                } catch (TechnicalException te) {
                    logger.error("Unexpected error while adding plan to the API: {} [{}]", remoteApi.getName(),
                            remoteApi.getId(), te);
                }
                return api;
            }
        } catch (IOException ioe) {
            logger.error("Unable to prepare API definition from repository for {}", remoteApi, ioe);
        }

        return null;
    }

    private Plan convert(io.gravitee.repository.management.model.Plan repoPlan) {
        Plan plan = new Plan();

        plan.setId(repoPlan.getId());
        plan.setName(repoPlan.getName());
        plan.setApis(repoPlan.getApis());

        if (repoPlan.getSecurity() != null) {
            plan.setSecurity(repoPlan.getSecurity().name());
        } else {
            // TODO: must be handle by a migration script
            plan.setSecurity("api_key");
        }

        try {
            if (repoPlan.getDefinition() != null && !repoPlan.getDefinition().trim().isEmpty()) {
                HashMap<String, Path> paths = objectMapper.readValue(repoPlan.getDefinition(),
                        new TypeReference<HashMap<String, Path>>() {
                        });

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
