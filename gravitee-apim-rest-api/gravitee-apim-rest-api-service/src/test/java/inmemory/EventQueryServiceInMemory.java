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
package inmemory;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.infra.query_service.event.EventQueryServiceImpl;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.common.Pageable;
import java.time.Instant;
import java.util.*;

public class EventQueryServiceInMemory implements EventQueryService, InMemoryAlternative<Event> {

    // As MongoDB and JDBC sort: updatedAt desc, id desc, a missing date last.
    private static final Comparator<Event> NEWEST_FIRST = Comparator.comparing(
        (Event event) -> event.getUpdatedAt() == null ? null : event.getUpdatedAt().toInstant(),
        Comparator.nullsFirst(Comparator.<Instant>naturalOrder())
    )
        .thenComparing(Event::getId, Comparator.nullsFirst(Comparator.<String>naturalOrder()))
        .reversed();

    private final List<Event> storage;

    public EventQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    @Override
    public SearchResponse search(SearchQuery query, Pageable pageable) {
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();

        var matches = storage
            .stream()
            .filter(event ->
                query
                    .id()
                    .map(id -> id.equals(event.getProperties().get(Event.EventProperties.ID)))
                    .orElse(true)
            )
            .filter(event ->
                query
                    .apiId()
                    .map(apiId -> event.getProperties().getOrDefault(Event.EventProperties.API_ID, "").equals(apiId))
                    .orElse(true)
            )
            .filter(event ->
                query
                    .properties()
                    .entrySet()
                    .stream()
                    .allMatch(property -> property.getValue().equals(event.getProperties().get(property.getKey())))
            )
            .filter(event -> event.getEnvironments().contains(query.environmentId()))
            .filter(event -> query.types().isEmpty() || query.types().contains(event.getType()))
            .filter(event ->
                query
                    .from()
                    .map(from -> event.getUpdatedAt() != null && !event.getUpdatedAt().toInstant().isBefore(Instant.ofEpochMilli(from)))
                    .orElse(true)
            )
            .filter(event ->
                query
                    .to()
                    .map(to -> event.getUpdatedAt() != null && event.getUpdatedAt().toInstant().isBefore(Instant.ofEpochMilli(to)))
                    .orElse(true)
            )
            .sorted(NEWEST_FIRST)
            .toList();

        var page = matches.size() <= pageSize
            ? matches
            : matches.subList(Math.min((pageNumber - 1) * pageSize, matches.size()), Math.min(pageNumber * pageSize, matches.size()));

        return new SearchResponse(matches.size(), page);
    }

    @Override
    public Optional<Event> findByIdForEnvironmentAndApi(String eventId, String environmentId, String apiId) {
        return storage()
            .stream()
            .filter(
                event ->
                    event.getId().equals(eventId) &&
                    event.getEnvironments().contains(environmentId) &&
                    apiId.equalsIgnoreCase(event.getProperties().get(Event.EventProperties.API_ID))
            )
            .findFirst();
    }

    @Override
    public Optional<Event> findByIdForEnvironment(String eventId, String environmentId) {
        return storage()
            .stream()
            .filter(event -> event.getId().equals(eventId) && event.getEnvironments().contains(environmentId))
            .findFirst();
    }

    @Override
    public Optional<Api> findApiFromPublishApiEvent(String eventId) {
        return storage()
            .stream()
            .filter(event -> event.getId().equals(eventId) && event.getType() == EventType.PUBLISH_API && event.getPayload() != null)
            .findFirst()
            .map(EventQueryServiceImpl::extractApiFromEvent);
    }

    @Override
    public void initWith(List<Event> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Event> storage() {
        return Collections.unmodifiableList(storage);
    }
}
