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
package io.gravitee.apim.infra.query_service.event;

import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.EventAdapter;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.EventLatestCursor;
import io.gravitee.repository.management.api.search.EventLatestPage;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class EventLatestQueryServiceImpl implements EventLatestQueryService {

    static final int FIND_ALL_PAGE_SIZE = 100;

    /**
     * The event types that mean "the gateways run this API". {@code events_latest} holds one row per API, so the API
     * is deployed if and only if that row is one of these.
     */
    private static final Set<io.gravitee.rest.api.model.EventType> DEPLOYED = Set.of(
        io.gravitee.rest.api.model.EventType.PUBLISH_API,
        io.gravitee.rest.api.model.EventType.START_API
    );

    private final EventLatestRepository eventLatestRepository;

    public EventLatestQueryServiceImpl(@Lazy EventLatestRepository eventLatestRepository) {
        this.eventLatestRepository = eventLatestRepository;
    }

    @Override
    public List<Event> findLatestByEntityIds(
        Set<String> entityIds,
        io.gravitee.rest.api.model.EventType eventType,
        Event.EventProperties propertyKey
    ) {
        if (entityIds.isEmpty()) {
            return List.of();
        }
        try {
            // The ids go into the criteria as a collection, which both stores turn into an IN rather than a
            // single-value comparison, so the store returns this page's events and not the environment's. The
            // explicit size keeps a store's default page (ten rows on JDBC) from truncating the batch.
            List<io.gravitee.repository.management.model.Event> events = eventLatestRepository.search(
                EventCriteria.builder()
                    .types(List.of(EventType.valueOf(eventType.name())))
                    .properties(Map.of(propertyKey.getLabel(), entityIds))
                    .build(),
                io.gravitee.repository.management.model.Event.EventProperties.valueOf(propertyKey.name()),
                0L,
                (long) entityIds.size()
            );
            return events.stream().map(EventAdapter.INSTANCE::map).toList();
        } catch (Exception e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to find latest events for entities " + entityIds + ": " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public List<Event> findAllByTypeAndEnvironments(
        Set<io.gravitee.rest.api.model.EventType> eventTypes,
        Set<String> environments,
        Event.EventProperties groupBy
    ) {
        try {
            List<EventType> repoEventTypes = eventTypes
                .stream()
                .map(et -> EventType.valueOf(et.name()))
                .collect(Collectors.toList());
            List<io.gravitee.repository.management.model.Event> events = eventLatestRepository.search(
                EventCriteria.builder().types(repoEventTypes).environments(environments).build(),
                io.gravitee.repository.management.model.Event.EventProperties.valueOf(groupBy.name()),
                null,
                null
            );
            return events.stream().map(EventAdapter.INSTANCE::map).collect(Collectors.toList());
        } catch (Exception e) {
            throw new TechnicalManagementException("An error occurs while trying to find latest events: " + e.getMessage(), e);
        }
    }

    @Override
    public Stream<Event> findAll(Set<io.gravitee.rest.api.model.EventType> eventTypes, Instant expiresBefore, Instant updatedBefore) {
        var criteria = EventCriteria.builder().types(
            eventTypes
                .stream()
                .map(et -> EventType.valueOf(et.name()))
                .toList()
        );
        if (expiresBefore != null) {
            criteria.expiresBefore(expiresBefore.toEpochMilli());
        }
        if (updatedBefore != null) {
            criteria.to(updatedBefore.toEpochMilli());
        }
        var built = criteria.build();
        // Keyset pages fetched lazily, so the caller holds one page at a time.
        return Stream.iterate(searchAfter(built, null), Objects::nonNull, page ->
            page.events().size() < FIND_ALL_PAGE_SIZE ? null : searchAfter(built, page.next())
        )
            .flatMap(page -> page.events().stream())
            .map(EventAdapter.INSTANCE::map);
    }

    @Override
    public Optional<ApiDeployment> findRunningApiDeployment(String environmentId, String apiId) {
        // The store call alone is guarded: the reading below answers the contract's TechnicalDomainException for a
        // payload it cannot read, and wrapping that into a store failure would hide what actually went wrong.
        List<io.gravitee.repository.management.model.Event> deployed;
        try {
            deployed = eventLatestRepository.search(
                EventCriteria.builder()
                    .types(
                        DEPLOYED.stream()
                            .map(type -> EventType.valueOf(type.name()))
                            .toList()
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getLabel(), apiId))
                    .environments(Set.of(environmentId))
                    .build(),
                io.gravitee.repository.management.model.Event.EventProperties.API_ID,
                0L,
                1L
            );
        } catch (Exception e) {
            throw new TechnicalDomainException(
                "An error occurs while trying to find the deployment the gateways run for the api " + apiId,
                e
            );
        }
        return deployed
            .stream()
            .map(EventAdapter.INSTANCE::map)
            .filter(event -> event.getPayload() != null && !event.getPayload().isBlank())
            .findFirst()
            .map(EventAdapter.INSTANCE::toApiDeployment);
    }

    private EventLatestPage searchAfter(EventCriteria criteria, EventLatestCursor after) {
        try {
            return eventLatestRepository.searchAfter(criteria, null, after, FIND_ALL_PAGE_SIZE);
        } catch (Exception e) {
            throw new TechnicalManagementException("An error occurs while trying to find latest events after " + after, e);
        }
    }
}
