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
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
@Slf4j
public class ApiEventQueryServiceImpl implements ApiEventQueryService {

    private final EventLatestRepository eventLatestRepository;
    private static final ApiAdapter apiAdapter = ApiAdapter.INSTANCE;

    public ApiEventQueryServiceImpl(@Lazy EventLatestRepository eventLatestRepository) {
        this.eventLatestRepository = eventLatestRepository;
    }

    @Override
    public Optional<Api> findLastPublishedApi(String organizationId, String environmentId, String apiId) {
        final Optional<Event> latestEvent = eventLatestRepository
            .search(buildCriteria(environmentId, apiId), Event.EventProperties.API_ID, 0L, 1L)
            .stream()
            .findFirst();

        if (latestEvent.isEmpty()) {
            return Optional.empty();
        }

        return latestEvent.flatMap(event -> {
            try {
                final io.gravitee.repository.management.model.Api api = GraviteeJacksonMapper.getInstance().readValue(
                    event.getPayload(),
                    io.gravitee.repository.management.model.Api.class
                );
                return Optional.ofNullable(apiAdapter.toCoreModel(api));
            } catch (JsonProcessingException e) {
                log.warn("Impossible to deserialize event payload for api: {}", apiId);
                return Optional.empty();
            }
        });
    }

    private static EventCriteria buildCriteria(String environmentId, String apiId) {
        return EventCriteria.builder()
            .environment(environmentId)
            .types(Set.of(io.gravitee.repository.management.model.EventType.PUBLISH_API))
            .property(Event.EventProperties.API_ID.getValue(), apiId)
            .build();
    }
}
