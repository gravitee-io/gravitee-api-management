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

import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.EventNotFoundException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;

public class EventCrudInMemory implements EventCrudService, InMemoryAlternative<Event> {

    private final List<Event> storage = new ArrayList<>();

    @SneakyThrows
    @Override
    public Event createEvent(
        String organizationId,
        String environmentId,
        Set<String> environmentIds,
        EventType eventType,
        Object content,
        Map<Event.EventProperties, String> properties
    ) {
        Event event = Event.builder()
            .id(UuidString.generateRandom())
            .type(eventType)
            .environments(environmentIds)
            .properties(new EnumMap<>(properties))
            .payload(GraviteeJacksonMapper.getInstance().writeValueAsString(content))
            .build();
        storage.add(event);
        return event;
    }

    @SneakyThrows
    @Override
    public Event get(String organizationId, String environmentId, String eventId) {
        return storage
            .stream()
            .filter(event -> event.getId().equals(eventId))
            .findFirst()
            .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    @Override
    public void cleanupEvents(String environmentId, int nbEventsToKeep, Duration timeToLive) {
        String NO_REMOVE = "NO_API";
        var byAPIs = storage
            .stream()
            .filter(event -> event.getEnvironments().contains(environmentId))
            .collect(Collectors.groupingBy(event -> event.getProperties().getOrDefault(Event.EventProperties.API_ID, "NO_API")));
        for (var entry : byAPIs.entrySet()) {
            if (!NO_REMOVE.equals(entry.getKey())) {
                var keep = entry
                    .getValue()
                    .stream()
                    .sorted(Comparator.comparing(Event::getUpdatedAt))
                    .limit(nbEventsToKeep)
                    .map(Event::getId)
                    .toList();
                storage.removeIf(event -> keep.contains(event.getId()));
            }
        }
    }

    @Override
    public void initWith(List<Event> items) {
        reset();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Event> storage() {
        return storage;
    }
}
