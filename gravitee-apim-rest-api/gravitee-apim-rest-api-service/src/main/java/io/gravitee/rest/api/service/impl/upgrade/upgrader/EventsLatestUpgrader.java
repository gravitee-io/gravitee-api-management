/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryType;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.service.EventService;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class EventsLatestUpgrader implements Upgrader {

    private static final int BULK_SIZE = 100;
    private final ApiRepository apiRepository;
    private final DictionaryRepository dictionaryRepository;
    private final OrganizationRepository organizationRepository;
    private final EventRepository eventRepository;
    private final EventLatestRepository eventLatestRepository;
    private int modelCounter;
    private int eventsForModelCounter;
    private final ObjectMapper objectMapper;

    @Autowired
    public EventsLatestUpgrader(
        @Lazy ApiRepository apiRepository,
        @Lazy DictionaryRepository dictionaryRepository,
        @Lazy OrganizationRepository organizationRepository,
        @Lazy EventRepository eventRepository,
        @Lazy EventLatestRepository eventLatestRepository,
        @Lazy ObjectMapper objectMapper
    ) {
        this.apiRepository = apiRepository;
        this.dictionaryRepository = dictionaryRepository;
        this.organizationRepository = organizationRepository;
        this.eventRepository = eventRepository;
        this.eventLatestRepository = eventLatestRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.EVENTS_LATEST_UPGRADER;
    }

    @Override
    public boolean upgrade() {
        try {
            migrateApiEvents();
            migrateDictionaryEvents();
            migrateOrganizationEvents();
        } catch (Exception e) {
            log.error("error occurred while applying upgrader {}", this.getClass().getSimpleName());
            return false;
        }

        return true;
    }

    private void migrateApiEvents() throws TechnicalException {
        log.info("Starting migrating latest event for APIs");
        modelCounter = 0;
        eventsForModelCounter = 0;
        List<ApiCriteria> emptyCriteria = List.of();
        Page<String> firstPage =
            this.apiRepository.searchIds(emptyCriteria, new PageableBuilder().pageNumber(0).pageSize(BULK_SIZE).build(), null);
        if (firstPage != null && firstPage.getContent() != null) {
            migrateEvents(Event.EventProperties.API_ID.getValue(), firstPage.getContent());

            // Cover others pages if required
            if (firstPage.getTotalElements() > firstPage.getPageElements()) {
                long pageNumber = (int) Math.ceil((double) firstPage.getTotalElements() / BULK_SIZE);
                for (int pageIdx = 1; pageIdx < pageNumber; pageIdx++) {
                    Page<String> nextPage =
                        this.apiRepository.searchIds(
                                emptyCriteria,
                                new PageableBuilder().pageNumber(pageIdx).pageSize(BULK_SIZE).build(),
                                null
                            );
                    migrateEvents(Event.EventProperties.API_ID.getValue(), nextPage.getContent());
                }
            }
        }
        log.info("{} events regarding {} APIs have been migrated", eventsForModelCounter, modelCounter);
    }

    private void migrateDictionaryEvents() throws TechnicalException {
        log.info("Starting migrating latest event for dictionaries");
        modelCounter = 0;
        eventsForModelCounter = 0;
        Set<Dictionary> dictionaries = this.dictionaryRepository.findAll();
        if (dictionaries != null) {
            for (Dictionary dictionary : dictionaries) {
                migrateDictionaryEvents(dictionary);
            }
        }
        log.info("{} events regarding {} dictionaries have been migrated", eventsForModelCounter, modelCounter);
    }

    private void migrateOrganizationEvents() throws TechnicalException {
        log.info("Starting migrating latest event for organizations");
        modelCounter = 0;
        eventsForModelCounter = 0;
        Set<Organization> organizations = this.organizationRepository.findAll();
        if (organizations != null) {
            List<String> ids = organizations.stream().map(Organization::getId).collect(Collectors.toList());
            migrateEvents(Event.EventProperties.ORGANIZATION_ID.getValue(), ids);
        }
        log.info("{} events regarding {} organizations have been migrated", eventsForModelCounter, modelCounter);
    }

    private void migrateEvents(final String propertyId, final List<String> ids) throws TechnicalException {
        for (String id : ids) {
            modelCounter++;
            Page<Event> eventPage = searchEvents(propertyId, id);
            if (eventPage.getPageElements() > 0) {
                processEvent(eventPage.getContent().get(0), id);
            }
        }
    }

    private void migrateDictionaryEvents(Dictionary dictionary) throws TechnicalException {
        Page<Event> eventPage = searchEvents(
            Event.EventProperties.DICTIONARY_ID.getValue(),
            dictionary.getId(),
            Set.of(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY)
        );
        if (eventPage.getPageElements() > 0) {
            Event event = eventPage.getContent().get(0);
            processEvent(event, dictionary.getId());
        }

        if (DictionaryType.DYNAMIC.equals(dictionary.getType())) {
            eventPage =
                searchEvents(
                    Event.EventProperties.DICTIONARY_ID.getValue(),
                    dictionary.getId(),
                    Set.of(EventType.START_DICTIONARY, EventType.STOP_DICTIONARY)
                );
            if (eventPage.getPageElements() > 0) {
                Event event = eventPage.getContent().get(0);
                processEvent(event, dictionary.getId() + EventService.EVENT_LATEST_DYNAMIC_SUFFIX);
            }
        }
    }

    private Page<Event> searchEvents(String propertyId, String id) {
        return searchEvents(propertyId, id, null);
    }

    private Page<Event> searchEvents(String propertyId, String id, Set<EventType> types) {
        EventCriteria.EventCriteriaBuilder criteria = EventCriteria.builder().property(propertyId, id);

        if (types != null && !types.isEmpty()) {
            criteria.types(types);
        }

        return this.eventRepository.search(criteria.build(), new PageableBuilder().pageNumber(0).pageSize(1).build());
    }

    private void processEvent(Event event, String id) throws TechnicalException {
        if (event.getProperties() == null) {
            event.setProperties(new HashMap<>());
        }
        event.getProperties().put(Event.EventProperties.ID.getValue(), event.getId());
        event.setId(id);
        // allow to reformat json payload without pretty
        if (event.getPayload() != null) {
            try {
                event.setPayload(objectMapper.writeValueAsString(objectMapper.readTree(event.getPayload())));
            } catch (JsonProcessingException e) {
                // Ignore this and keep existing payload
            }
        }
        this.eventLatestRepository.createOrUpdate(event);
        eventsForModelCounter++;
    }
}
