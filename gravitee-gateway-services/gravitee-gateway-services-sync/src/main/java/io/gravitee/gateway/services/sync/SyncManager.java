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
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.dictionary.model.Dictionary;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.node.api.cluster.ClusterManager;
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
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BinaryOperator;
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
    private ClusterManager clusterManager;

    @Value("${services.sync.distributed:false}")
    private boolean distributed;

    private final AtomicLong counter = new AtomicLong(0);

    private long lastRefreshAt = -1;

    private int totalErrors = 0;

    private int errors = 0;

    private String lastErrorMessage;

    void refresh() {
        long nextLastRefreshAt = System.currentTimeMillis();
        boolean error = false;
        if (clusterManager.isMasterNode() || (!clusterManager.isMasterNode() && !distributed)) {
            logger.debug("Synchronization #{} started at {}", counter.incrementAndGet(), Instant.now().toString());
            logger.debug("Refreshing gateway state...");

            try {
                synchronizeApis(nextLastRefreshAt);
            } catch (Exception ex) {
                error = true;
                lastErrorMessage = ex.getMessage();
                logger.error("An error occurs while synchronizing APIs", ex);
            }

            try {
                synchronizeDictionaries(nextLastRefreshAt);
            } catch (Exception ex) {
                error = true;
                lastErrorMessage = ex.getMessage();
                logger.error("An error occurs while synchronizing dictionaries", ex);
            }

            if (error) {
                errors++;
                totalErrors++;
            } else {
                errors = 0;
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

    private void synchronizeApis(long nextLastRefreshAt) {
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
                        apiManager.unregister(apiId);
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

                            enhanceWithData(api);

                            apiManager.register(api);
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
                // Search window is extended by 5 seconds for each sync error to ensure that we are never missing data
                .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY - (5000 * errors) )
                .to(nextLastRefreshAt + TIMEFRAME_AFTER_DELAY);

        return eventRepository.search(builder.build());
    }

    private List<Event> getLatestApiEvents(long nextLastRefreshAt) {
        final EventCriteria.Builder builder = new EventCriteria.Builder()
                .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                // Search window is extended by 5 seconds for each sync error to ensure that we are never missing data
                .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY - (5000 * errors) )
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
            // for v2, plans are already part of the api definition
            if (definition.getDefinitionVersion() == DefinitionVersion.V1) {
                // Deploy only published plan
                definition.setPlans(planRepository.findByApi(definition.getId())
                        .stream()
                        .filter(plan -> io.gravitee.repository.management.model.Plan.Status.PUBLISHED.equals(plan.getStatus())
                                || io.gravitee.repository.management.model.Plan.Status.DEPRECATED.equals(plan.getStatus()))
                        .map(this::convert)
                        .collect(Collectors.toList()));
            } else if (definition.getDefinitionVersion() == DefinitionVersion.V2) {
                definition.setPlans(definition.getPlans()
                        .stream()
                        .filter(plan -> "published".equalsIgnoreCase(plan.getStatus())
                                || "deprecated".equalsIgnoreCase(plan.getStatus()))
                        .collect(Collectors.toList()));
            }
        } catch (TechnicalException te) {
            logger.error("Unexpected error while adding plan to the API: {} [{}]", definition.getName(),
                    definition.getId(), te);
        }
    }

    private Plan convert(io.gravitee.repository.management.model.Plan repoPlan) {
        Plan plan = new Plan();

        plan.setId(repoPlan.getId());
        plan.setName(repoPlan.getName());
        plan.setSecurityDefinition(repoPlan.getSecurityDefinition());
        plan.setSelectionRule(repoPlan.getSelectionRule());
        plan.setTags(repoPlan.getTags());

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

    public void setDistributed(boolean distributed) {
        this.distributed = distributed;
    }

    public long getLastRefreshAt() {
        return lastRefreshAt;
    }

    public long getCounter() {
        return counter.longValue();
    }

    public int getTotalErrors() {
        return totalErrors;
    }

    public int getErrors() {
        return errors;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
}
