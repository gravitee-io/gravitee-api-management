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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.EventAdapter;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import lombok.SneakyThrows;
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

    @Override
    public Optional<io.gravitee.apim.core.event.model.Event> findByIdForEnvironmentAndApi(
        String eventId,
        String environmentId,
        String apiId
    ) {
        try {
            return eventRepository
                .findById(eventId)
                .map(EventAdapter.INSTANCE::map)
                .filter(
                    e ->
                        e.getEnvironments().contains(environmentId) &&
                        apiId.equalsIgnoreCase(e.getProperties().get(io.gravitee.apim.core.event.model.Event.EventProperties.API_ID))
                );
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find API event by id: " + eventId, e);
        }
    }

    @SneakyThrows
    @Override
    public Optional<io.gravitee.apim.core.api.model.Api> findApiFromPublishApiEvent(String eventId) {
        try {
            return eventRepository
                .findById(eventId)
                .map(EventAdapter.INSTANCE::map)
                .filter(e -> e.getType().equals(io.gravitee.rest.api.model.EventType.PUBLISH_API) && e.getPayload() != null)
                .map(EventQueryServiceImpl::extractApiFromEvent);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find API event by id: " + eventId, e);
        }
    }

    public static Api extractApiFromEvent(io.gravitee.apim.core.event.model.Event event) {
        try {
            var apiRepositoryModel = toApiRepositoryModel(event);
            return ApiAdapter.INSTANCE.toCoreModel(apiRepositoryModel);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read API definition from event" + event.getId(), e);
        }
    }

    private static io.gravitee.repository.management.model.Api toApiRepositoryModel(io.gravitee.apim.core.event.model.Event event)
        throws JsonProcessingException {
        return GraviteeJacksonMapper.getInstance().readValue(event.getPayload(), io.gravitee.repository.management.model.Api.class);
    }
}
