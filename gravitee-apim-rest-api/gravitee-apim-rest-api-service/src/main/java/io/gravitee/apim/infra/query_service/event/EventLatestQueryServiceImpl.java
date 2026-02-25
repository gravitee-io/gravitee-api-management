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
import io.gravitee.apim.infra.adapter.EventAdapter;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class EventLatestQueryServiceImpl implements EventLatestQueryService {

    private final EventLatestRepository eventLatestRepository;

    public EventLatestQueryServiceImpl(@Lazy EventLatestRepository eventLatestRepository) {
        this.eventLatestRepository = eventLatestRepository;
    }

    @Override
    public Optional<Event> findLatestByEntityId(
        String entityId,
        io.gravitee.rest.api.model.EventType eventType,
        Event.EventProperties propertyKey
    ) {
        try {
            List<io.gravitee.repository.management.model.Event> events = eventLatestRepository.search(
                EventCriteria.builder()
                    .types(List.of(EventType.valueOf(eventType.name())))
                    .properties(Map.of(propertyKey.getLabel(), entityId))
                    .build(),
                io.gravitee.repository.management.model.Event.EventProperties.valueOf(propertyKey.name()),
                0L,
                1L
            );
            return events.stream().findFirst().map(EventAdapter.INSTANCE::map);
        } catch (Exception e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to find latest event for entity [" + entityId + "]: " + e.getMessage(),
                e
            );
        }
    }
}
