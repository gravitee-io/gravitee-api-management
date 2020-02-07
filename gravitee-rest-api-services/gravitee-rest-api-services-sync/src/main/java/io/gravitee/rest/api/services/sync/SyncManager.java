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
package io.gravitee.rest.api.services.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.event.EventManager;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.event.DictionaryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
 * @author GraviteeSource Team
 */
public class SyncManager {

    private final Logger logger = LoggerFactory.getLogger(SyncManager.class);

    private static final int TIMEFRAME_BEFORE_DELAY = 10 * 60 * 1000;
    private static final int TIMEFRAME_AFTER_DELAY = 1 * 60 * 1000;

    @Autowired
    private ApiRepository apiRepository;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private DictionaryRepository dictionaryRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ApiManager apiManager;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EventManager eventManager;
    @Autowired
    private MembershipService membershipService;
    @Autowired
    private UserService userService;

    private final AtomicLong counter = new AtomicLong(0);

    private long lastRefreshAt = -1;

    public void refresh() {
        logger.debug("Synchronization #{} started at {}", counter.incrementAndGet(), Instant.now());
        logger.debug("Refreshing state...");

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
        logger.debug("Synchronization #{} ended at {}", counter.get(), Instant.now());
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
            List<Dictionary> dictionaries = dictionaryRepository.findAll()
                    .stream()
                    .filter(dictionary -> dictionary.getType() == DictionaryType.DYNAMIC)
                    .collect(Collectors.toList());

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


        computeDictionaryEvents(dictionaryEvents);
    }

    private void computeDictionaryEvents(Map<String, Event> dictionaryEvents) {
        dictionaryEvents.forEach((id, event) -> {

            switch (event.getType()) {
                case START_DICTIONARY:
                    // Read dictionary
                    DictionaryEntity dictionary = dictionaryService.findById(id);
                    eventManager.publishEvent(DictionaryEvent.START, dictionary);
                    break;
                case STOP_DICTIONARY:
                    // We get a stop, which can be a follow-up to a deleted dictionary
                    // In that case just pass a dictionary with a reference only
                    DictionaryEntity stoppedDictionary = new DictionaryEntity();
                    stoppedDictionary.setId(id);
                    eventManager.publishEvent(DictionaryEvent.STOP, stoppedDictionary);
                    break;
                default:
                    break;
            }
        });
    }

    private void computeApiEvents(Map<String, Event> apiEvents) {
        apiEvents.forEach((apiId, apiEvent) -> {
            switch (apiEvent.getType()) {
                case UNPUBLISH_API:
                case STOP_API:
                    apiManager.undeploy(apiId);
                    break;
                case START_API:
                case PUBLISH_API:
                    try {
                        // Read API definition from event
                        io.gravitee.repository.management.model.Api payloadApi =
                                objectMapper.readValue(apiEvent.getPayload(), io.gravitee.repository.management.model.Api.class);

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
                    } catch (Exception e) {
                        logger.error("Error while determining deployed APIs store into events payload", e);
                    }
                    break;
                default:
                    break;
            }
        });
    }

    private Event getLastDictionaryEvent(final String dictionary) {
        final EventCriteria.Builder eventCriteriaBuilder =
                new EventCriteria.Builder()
                        .property(Event.EventProperties.DICTIONARY_ID.getValue(), dictionary);

        List<Event> events = eventRepository.search(eventCriteriaBuilder
                        .types(EventType.START_DICTIONARY, EventType.STOP_DICTIONARY).build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()).getContent();

        return (!events.isEmpty()) ? events.get(0) : null;
    }

    private List<Event> getLatestDictionaryEvents(long nextLastRefreshAt) {
        final EventCriteria.Builder builder = new EventCriteria.Builder()
                .types(EventType.START_DICTIONARY, EventType.STOP_DICTIONARY)
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
        final ApiLifecycleState apiLifecycleState = api.getApiLifecycleState();
        if (apiLifecycleState != null) {
            apiEntity.setLifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.valueOf(apiLifecycleState.name()));
        }
        if (api.getVisibility() != null) {
            apiEntity.setVisibility(io.gravitee.rest.api.model.Visibility.valueOf(api.getVisibility().toString()));
        }

        MemberEntity optPrimaryOwner = membershipService.getPrimaryOwner(MembershipReferenceType.API, api.getId());
        final UserEntity user = userService.findById(optPrimaryOwner.getId());
        apiEntity.setPrimaryOwner(new PrimaryOwnerEntity(user));
            
        return apiEntity;
    }
}
