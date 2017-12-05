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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SyncManager {

    private final Logger logger = LoggerFactory.getLogger(SyncManager.class);

    private static final int TIMEFRAME_BEFORE_DELAY = 10 * 60 * 1000;
    private static final int TIMEFRAME_AFTER_DELAY = 1 * 60 * 1000;

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

    private final AtomicLong counter = new AtomicLong(0);

    private long lastRefreshAt = -1;

    public void refresh() {
        logger.debug("Synchronization #{} started at {}", counter.incrementAndGet(), Instant.now().toString());
        logger.debug("Refreshing gateway state...");

        try {
            // Extract all registered APIs
            Map<String, io.gravitee.repository.management.model.Api> apis = apiRepository.findAll()
                    .stream()
                    .collect(Collectors.toMap(io.gravitee.repository.management.model.Api::getId, api -> api));

            long nextLastRefreshAt = System.currentTimeMillis();
            // Get last event by API
            Map<String, Event> events = new HashMap<>();
            apis.forEach((id, api) -> {
                Event event = getLastEvent(id, nextLastRefreshAt);
                if (event != null) {
                    events.put(id, event);
                }
            });

            // Extract definition for each event
            Map<String, Api> definitions = new HashMap<>();
            events
                    .values()
                    .forEach(event -> {
                        try {
                            // Read API definition from event
                            io.gravitee.repository.management.model.Api eventPayload = objectMapper.readValue(event.getPayload(), io.gravitee.repository.management.model.Api.class);
                            Api definition = objectMapper.readValue(eventPayload.getDefinition(), Api.class);
                            io.gravitee.repository.management.model.Api api = apis.get(definition.getId());

                            // Update definition with required information for deployment phase
                            definition.setEnabled(api.getLifecycleState() == LifecycleState.STARTED);
                            definition.setDeployedAt(api.getUpdatedAt());
                            definitions.put(definition.getId(), definition);
                        } catch (IOException ioe) {
                            logger.error("Error while determining deployed APIs store into events payload", ioe);
                        }
                    });

            // Determine APIs to undeploy because of deployment tags
            Set<String> apisToRemove = definitions.values()
                    .stream()
                    .filter(api -> !hasMatchingTags(api))
                    .map(Api::getId)
                    .collect(Collectors.toSet());

            // Undeploy APIs not relative to this gateway instance (different deployment tags)
            apisToRemove.forEach(apiId -> {
                apiManager.undeploy(apiId);
                apis.remove(apiId);
                events.remove(apiId);
                definitions.remove(apiId);
            });

            // Determine API which must be stopped and stop them
            events.entrySet()
                    .stream()
                    .filter(apiEvent -> {
                        Event event = apiEvent.getValue();
                        return event.getType() == EventType.STOP_API || event.getType() == EventType.UNPUBLISH_API;
                    })
                    .forEach(apiEvent -> apiManager.undeploy(apiEvent.getKey()));

            // Determine API which must be deployed
            events.entrySet()
                    .stream()
                    .filter(apiEvent -> {
                        Event event = apiEvent.getValue();
                        return event.getType() == EventType.START_API || event.getType() == EventType.PUBLISH_API;
                    })
                    .forEach(apiEvent -> {
                        // Read API definition from event
                        Api definition = definitions.get(apiEvent.getKey());

                        // API to deploy
                        enhanceWithData(definition);

                        if (definition != null) {
                            // Get deployed API
                            Api deployedApi = apiManager.get(definition.getId());

                            // API is not yet deployed, so let's do it !
                            if (deployedApi == null) {
                                apiManager.deploy(definition);
                            } else if (deployedApi.getDeployedAt().before(definition.getDeployedAt())) {
                                apiManager.update(definition);
                            }
                        }
                    });

            lastRefreshAt = nextLastRefreshAt;
        } catch (TechnicalException te) {
            logger.error("Unable to sync instance", te);
        }
        logger.debug("Synchronization #{} ended at {}", counter.get(), Instant.now().toString());
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
                    logger.debug("The API {} has been ignored because not in configured tags {}", api.getName(), tagList);
                }
                return hasMatchingTags;
            }
            logger.debug("Tags {} are configured on gateway instance but not found on the API {}", tagList, api.getName());
            return false;
        }
        // no tags configured on this gateway instance
        return true;
    }

    private Event getLastEvent(final String api, long nextLastRefreshAt) {
        final EventCriteria.Builder eventCriteriaBuilder;
        if (lastRefreshAt == -1) {
            eventCriteriaBuilder = new EventCriteria.Builder()
                    .property(Event.EventProperties.API_ID.getValue(), api);
        } else {
            eventCriteriaBuilder = new EventCriteria.Builder()
                    .property(Event.EventProperties.API_ID.getValue(), api)
                    .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
                    .to(nextLastRefreshAt + TIMEFRAME_AFTER_DELAY);
        }

        List<Event> events = eventRepository.search(eventCriteriaBuilder
                .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API).build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()).getContent();
        return (!events.isEmpty()) ? events.get(0) : null;
    }

    private void enhanceWithData(Api definition) {
        try {
            // Deploy only published plan
            definition.setPlans(planRepository.findByApi(definition.getId())
                    .stream()
                    .filter(plan -> plan.getStatus() == io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
                    .map(this::convert)
                    .collect(Collectors.toList()));
        } catch (TechnicalException te) {
            logger.error("Unexpected error while adding plan to the API: {} [{}]", definition.getName(),
                    definition.getId(), te);
        }
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

    public long getLastRefreshAt() {
        return lastRefreshAt;
    }

    public long getCounter() {
        return counter.longValue();
    }
}
