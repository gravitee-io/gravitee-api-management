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
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.definition.Plan;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
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
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toMap;

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
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ApiManager apiManager;

    @Autowired
    private DictionaryManager dictionaryManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GatewayConfiguration gatewayConfiguration;

    private final AtomicLong counter = new AtomicLong(0);

    private long lastRefreshAt = -1;

    public void refresh() {
        logger.debug("Synchronization #{} started at {}", counter.incrementAndGet(), Instant.now().toString());
        logger.debug("Refreshing gateway state...");

        long nextLastRefreshAt = System.currentTimeMillis();

        try {
            synchronizeApis(nextLastRefreshAt);
        } catch (Exception ex) {
            logger.error("An error occurs while synchronizing APIs", ex);
        }

        try {
            synchronizeDictionaries(nextLastRefreshAt);
        } catch (Exception ex) {
            logger.error("An error occurs while synchronizing dictionaries", ex);
        }

        lastRefreshAt = nextLastRefreshAt;
        logger.debug("Synchronization #{} ended at {}", counter.get(), Instant.now().toString());
    }

    private void synchronizeApis(long nextLastRefreshAt) {//} throws Exception {
        Map<String, Event> apiEvents;

        // Initial synchronization
        if (lastRefreshAt == -1) {
            // Extract all registered APIs
            List<io.gravitee.repository.management.model.Api> apis =
                    apiRepository.search(null, new ApiFieldExclusionFilter.Builder()
                            .excludeDefinition()
                            .excludePicture().build());

            // Get last event by API
            apiEvents = apis
                    .stream()
                    .map(api -> getLastApiEvent(api.getId()))
                    .filter(Objects::nonNull)
                    .collect(
                            toMap(
                                    event -> event.getProperties().get(Event.EventProperties.API_ID.getValue()),
                                    event -> event
                            )
                    );
        } else {
            // Get latest API events
            List<Event> events = getLatestApiEvents(nextLastRefreshAt);

            // Extract only the latest event by API
            apiEvents = events
                    .stream()
                    .collect(
                            toMap(
                                    event -> event.getProperties().get(Event.EventProperties.API_ID.getValue()),
                                    event -> event,
                                    BinaryOperator.maxBy(comparing(Event::getCreatedAt))));
        }

        // Then, compute events
        computeApiEvents(apiEvents);
    }

    private void synchronizeDictionaries(long nextLastRefreshAt) throws Exception {
        Map<String, Event> dictionaryEvents;

        // Initial synchronization
        if (lastRefreshAt == -1) {
            Set<io.gravitee.repository.management.model.Dictionary> dictionaries = dictionaryRepository.findAll();

            // Get last event by dictionary
            dictionaryEvents = dictionaries
                    .stream()
                    .map(api -> getLastDictionaryEvent(api.getId()))
                    .filter(Objects::nonNull)
                    .collect(
                            toMap(
                                    event -> event.getProperties().get(Event.EventProperties.DICTIONARY_ID.getValue()),
                                    event -> event
                            )
                    );
        } else {
            // Get latest dictionary events
            List<Event> events = getLatestDictionaryEvents(nextLastRefreshAt);

            // Extract only the latest event by API
            dictionaryEvents = events
                    .stream()
                    .collect(
                            toMap(
                                    event -> event.getProperties().get(Event.EventProperties.DICTIONARY_ID.getValue()),
                                    event -> event,
                                    BinaryOperator.maxBy(comparing(Event::getCreatedAt))));
        }

        // Then, compute events
        computeDictionaryEvents(dictionaryEvents);
    }

    private void computeDictionaryEvents(Map<String, Event> dictionaryEvents) {
        dictionaryEvents.forEach((dictionaryId, event) -> {
            switch (event.getType()) {
                case UNPUBLISH_DICTIONARY:
                    dictionaryManager.undeploy(dictionaryId);
                    break;
                case PUBLISH_DICTIONARY:
                    try {
                        // Read dictionary definition from event
                        Dictionary dictionary = objectMapper.readValue(event.getPayload(), Dictionary.class);
                        dictionaryManager.deploy(dictionary);
                    } catch (IOException ioe) {
                        logger.error("Error while determining deployed dictionaries into events payload", ioe);
                    }
                    break;
            }
        });
    }

    private void computeApiEvents(Map<String, Event> apiEvents) {
        apiEvents.forEach((apiId, apiEvent) -> {
            try {
                switch (apiEvent.getType()) {
                    case UNPUBLISH_API:
                    case STOP_API:
                        apiManager.undeploy(apiId);
                        break;
                    case START_API:
                    case PUBLISH_API:
                        try {
                            // Read API definition from event
                            io.gravitee.repository.management.model.Api eventPayload =
                                    objectMapper.readValue(apiEvent.getPayload(), io.gravitee.repository.management.model.Api.class);

                            io.gravitee.definition.model.Api eventApiDefinition =
                                    objectMapper.readValue(eventPayload.getDefinition(), io.gravitee.definition.model.Api.class);

                            // Update definition with required information for deployment phase
                            final Api api = new Api(eventApiDefinition);
                            api.setEnabled(eventPayload.getLifecycleState() == LifecycleState.STARTED);
                            api.setDeployedAt(eventPayload.getDeployedAt());

                            // Get deployed API
                            Api deployedApi = apiManager.get(api.getId());

                            // Does the API have a matching sharding tags ?
                            if (hasMatchingTags(api.getTags())) {
                                // API to deploy
                                enhanceWithData(api);

                                // API is not yet deployed, so let's do it !
                                if (deployedApi == null) {
                                    apiManager.deploy(api);
                                } else if (deployedApi.getDeployedAt().before(api.getDeployedAt())) {
                                    apiManager.update(api);
                                }
                            } else {
                                logger.debug("The API {} has been ignored because not in configured tags {}", api.getName(), api.getTags());

                                // Check that the API was not previously deployed with other tags
                                // In that case, we must undeploy it
                                if (deployedApi != null) {
                                    apiManager.undeploy(apiId);
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error while determining deployed APIs store into events payload", e);
                        }
                        break;
                }
            } catch (Throwable t) {
                logger.error("An unexpected error occurs while managing the deployment of API id[{}]", apiId, t);
            }
        });
    }

    private boolean hasMatchingTags(Set<String> tags) {
        final Optional<List<String>> optTagList = gatewayConfiguration.shardingTags();

        if (optTagList.isPresent()) {
            List<String> tagList = optTagList.get();
            if (tags != null) {
                final List<String> inclusionTags = tagList.stream()
                        .map(String::trim)
                        .filter(tag -> !tag.startsWith("!"))
                        .collect(Collectors.toList());

                final List<String> exclusionTags = tagList.stream()
                        .map(String::trim)
                        .filter(tag -> tag.startsWith("!"))
                        .map(tag -> tag.substring(1))
                        .collect(Collectors.toList());

                if (inclusionTags.stream().anyMatch(exclusionTags::contains)) {
                    throw new IllegalArgumentException("You must not configure a tag to be included and excluded");
                }

                final boolean hasMatchingTags =
                        inclusionTags.stream()
                                .anyMatch(tag -> tags.stream()
                                        .anyMatch(crtTag -> {
                                            final Collator collator = Collator.getInstance();
                                            collator.setStrength(Collator.NO_DECOMPOSITION);
                                            return collator.compare(tag, crtTag) == 0;
                                        })
                                ) || (!exclusionTags.isEmpty() &&
                                exclusionTags.stream()
                                        .noneMatch(tag -> tags.stream()
                                                .anyMatch(crtTag -> {
                                                    final Collator collator = Collator.getInstance();
                                                    collator.setStrength(Collator.NO_DECOMPOSITION);
                                                    return collator.compare(tag, crtTag) == 0;
                                                })
                                        ));
                return hasMatchingTags;
            }
        //    logger.debug("Tags {} are configured on gateway instance but not found on the API {}", tagList, api.getName());
            return false;
        }
        // no tags configured on this gateway instance
        return true;
    }

    private Event getLastDictionaryEvent(final String dictionary) {
        final EventCriteria.Builder eventCriteriaBuilder =
                new EventCriteria.Builder()
                        .property(Event.EventProperties.DICTIONARY_ID.getValue(), dictionary);

        List<Event> events = eventRepository.search(eventCriteriaBuilder
                        .types(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY).build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()).getContent();

        return (!events.isEmpty()) ? events.get(0) : null;
    }

    private List<Event> getLatestDictionaryEvents(long nextLastRefreshAt) {
        final EventCriteria.Builder builder = new EventCriteria.Builder()
                .types(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY)
                .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
                .to(nextLastRefreshAt + TIMEFRAME_AFTER_DELAY);

        return eventRepository.search(builder.build());
    }

    private List<Event> getLatestApiEvents(long nextLastRefreshAt) {
        final EventCriteria.Builder builder = new EventCriteria.Builder()
                .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
                .to(nextLastRefreshAt + TIMEFRAME_AFTER_DELAY);

        return eventRepository.search(builder.build());
    }

    private Event getLastApiEvent(final String api) {
        final EventCriteria.Builder eventCriteriaBuilder =
                new EventCriteria.Builder()
                        .property(Event.EventProperties.API_ID.getValue(), api);

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
                    .filter(plan -> io.gravitee.repository.management.model.Plan.Status.PUBLISHED.equals(plan.getStatus())
                                 || io.gravitee.repository.management.model.Plan.Status.DEPRECATED.equals(plan.getStatus()))
                    .filter(new Predicate<io.gravitee.repository.management.model.Plan>() {
                        @Override
                        public boolean test(io.gravitee.repository.management.model.Plan plan) {
                            if (plan.getTags() != null && ! plan.getTags().isEmpty()) {
                                boolean hasMatchingTags = hasMatchingTags(plan.getTags());
                                logger.debug("Plan name[{}] api[{}] has been ignored because not in configured sharding tags {}", plan.getName(), definition.getName());
                                return hasMatchingTags;
                            }

                            return true;
                        }
                    })
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
        plan.setApi(repoPlan.getApi());
        plan.setSecurityDefinition(repoPlan.getSecurityDefinition());
        plan.setSelectionRule(repoPlan.getSelectionRule());

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
