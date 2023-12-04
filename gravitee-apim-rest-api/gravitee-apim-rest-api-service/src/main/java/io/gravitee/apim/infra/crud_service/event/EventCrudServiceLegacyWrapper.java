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
package io.gravitee.apim.infra.crud_service.event;

import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.infra.adapter.EventAdapter;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EventCrudServiceLegacyWrapper implements EventCrudService {

    private final EventService eventService;

    @Override
    public Event createEvent(
        String organizationId,
        String environmentId,
        Set<String> environmentIds,
        EventType eventType,
        Object content,
        Map<Event.EventProperties, String> properties
    ) {
        return EventAdapter.INSTANCE.fromEntity(
            eventService.createEvent(
                new ExecutionContext(organizationId, environmentId),
                environmentIds,
                eventType,
                content,
                EventAdapter.INSTANCE.toStringEventPropertiesMap(properties)
            )
        );
    }
}
