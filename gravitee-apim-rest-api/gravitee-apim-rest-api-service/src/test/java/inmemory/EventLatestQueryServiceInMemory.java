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

import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.rest.api.model.EventType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EventLatestQueryServiceInMemory implements EventLatestQueryService, InMemoryAlternative<Event> {

    private final List<Event> storage;

    public EventLatestQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    @Override
    public Optional<Event> findLatestByEntityId(String entityId, EventType eventType, Event.EventProperties propertyKey) {
        return storage
            .stream()
            .filter(event -> eventType.name().equals(event.getType() != null ? event.getType().name() : null))
            .filter(event -> entityId.equals(event.getProperties().getOrDefault(propertyKey, null)))
            .max(Comparator.comparing(event -> event.getUpdatedAt() != null ? event.getUpdatedAt() : event.getCreatedAt()));
    }

    @Override
    public void initWith(List<Event> items) {
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
