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

import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import java.util.ArrayList;
import java.util.List;

public class EventLatestCrudInMemory implements EventLatestCrudService, InMemoryAlternative<Event> {

    private final List<Event> storage = new ArrayList<>();

    @Override
    public Event createOrPatchLatestEvent(String organizationId, String latestEventId, Event event) {
        event.setId(latestEventId);
        if (storage.contains(event)) {
            storage.remove(event);
        }
        storage.add(event);
        return event;
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
