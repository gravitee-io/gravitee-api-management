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
package io.gravitee.apim.infra.query_service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiEventQueryService;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ApiEventQueryServiceLegacyWrapper implements ApiEventQueryService {

    private final EventService eventService;
    private static final ApiAdapter apiAdapter = ApiAdapter.INSTANCE;

    @Override
    public Optional<Api> findLastPublishedApi(String organizationId, String environmentId, String apiId) {
        final EventQuery eventQuery = new EventQuery();
        eventQuery.setApi(apiId);
        eventQuery.setTypes(List.of(EventType.PUBLISH_API));
        final Optional<EventEntity> latestEvent = eventService
            .search(new ExecutionContext(organizationId, environmentId), eventQuery)
            .stream()
            .max(Comparator.comparing(EventEntity::getUpdatedAt));

        if (latestEvent.isEmpty()) {
            return Optional.empty();
        }

        return latestEvent.flatMap(event -> {
            try {
                final io.gravitee.repository.management.model.Api api = GraviteeJacksonMapper
                    .getInstance()
                    .readValue(event.getPayload(), io.gravitee.repository.management.model.Api.class);
                return Optional.ofNullable(apiAdapter.toCoreModel(api));
            } catch (JsonProcessingException e) {
                log.warn("Impossible to deserialize event payload for api: {}", apiId);
                return Optional.empty();
            }
        });
    }
}
