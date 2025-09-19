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
import java.util.*;

public class EventQueryServiceInMemory implements EventQueryService, InMemoryAlternative<Event> {

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
                    .map(id -> event.getId().equals(id))
                    .orElse(true)
            )
            .filter(event ->
                query
                    .apiId()
                    .map(apiId -> event.getProperties().getOrDefault(Event.EventProperties.API_ID, "").equals(apiId))
                    .orElse(true)
            )
            .filter(event -> event.getEnvironments().contains(query.environmentId()))
            .filter(event -> query.types().isEmpty() || query.types().contains(event.getType()))
            .filter(event ->
                query
                    .from()
                    .map(from -> event.getCreatedAt().toInstant().isAfter(new Date(from).toInstant()))
                    .orElse(true)
            )
            .filter(event ->
                query
                    .to()
                    .map(to -> event.getCreatedAt().toInstant().isBefore(new Date(to).toInstant()))
                    .orElse(true)
            )
            .sorted(Comparator.comparing(Event::getCreatedAt).reversed())
            .toList();

        var page = matches.size() <= pageSize ? matches : matches.subList((pageNumber - 1) * pageSize, pageNumber * pageSize);

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
