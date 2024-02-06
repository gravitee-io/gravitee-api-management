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

import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.infra.adapter.EventAdapter;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.rest.api.model.common.Pageable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class EventQueryServiceImpl implements EventQueryService {

    private final EventRepository eventRepository;

    public EventQueryServiceImpl(@Lazy EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public SearchResponse search(SearchQuery query, Pageable pageable) {
        var criteriaBuilder = EventCriteria.builder().environment(query.environmentId());
        query.properties().forEach((key, value) -> criteriaBuilder.property(key.getLabel(), value));
        query.types().forEach(type -> criteriaBuilder.type(EventType.valueOf(type.name())));
        query.id().ifPresent(id -> criteriaBuilder.property(Event.EventProperties.ID.getValue(), id));
        query.apiId().ifPresent(apiId -> criteriaBuilder.property(Event.EventProperties.API_ID.getValue(), apiId));
        query.from().ifPresent(criteriaBuilder::from);
        query.to().ifPresent(criteriaBuilder::to);

        var result = eventRepository.search(
            criteriaBuilder.build(),
            new PageableBuilder().pageNumber(pageable.getPageNumber() - 1).pageSize(pageable.getPageSize()).build()
        );

        return new SearchResponse(result.getTotalElements(), result.getContent().stream().map(EventAdapter.INSTANCE::map).toList());
    }
}
