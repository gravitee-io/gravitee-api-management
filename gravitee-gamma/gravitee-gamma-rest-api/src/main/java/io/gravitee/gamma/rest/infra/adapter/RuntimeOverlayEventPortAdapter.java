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
package io.gravitee.gamma.rest.infra.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.gamma.rest.core.runtime_overlay.port.service_provider.RuntimeOverlayEventPort;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RuntimeOverlayEventPortAdapter implements RuntimeOverlayEventPort {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final int MAX_EVENTS = 500;
    static final String KIND = "runtime-overlay";

    private final EventCrudService eventCrudService;
    private final EventQueryService eventQueryService;
    private final ObjectMapper objectMapper;

    @Override
    public String publish(
        final String organizationId,
        final String environmentId,
        final String apiId,
        final String userId,
        final String runtimeOverlayId,
        final String action,
        final Map<String, Object> payload
    ) {
        final var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        properties.put(Event.EventProperties.ID, runtimeOverlayId);
        properties.put(Event.EventProperties.GENERIC_EVENT_KIND, KIND);
        properties.put(Event.EventProperties.GENERIC_EVENT_ACTION, action);
        properties.put(Event.EventProperties.API_ID, apiId);
        if (userId != null) {
            properties.put(Event.EventProperties.USER, userId);
        }
        return eventCrudService
            .createEvent(organizationId, environmentId, Set.of(environmentId), EventType.GENERIC_EVENT, payload, properties)
            .getId();
    }

    @Override
    public List<Map<String, Object>> payloads(final String environmentId, final long from, final long to) {
        return eventQueryService
            .search(
                new EventQueryService.SearchQuery(
                    environmentId,
                    Optional.empty(),
                    Optional.empty(),
                    List.of(EventType.GENERIC_EVENT),
                    Map.of(Event.EventProperties.GENERIC_EVENT_KIND, KIND),
                    Optional.of(from),
                    Optional.of(to)
                ),
                new PageableImpl(1, MAX_EVENTS)
            )
            .events()
            .stream()
            .sorted(Comparator.comparing(Event::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
            .map(event -> read(event.getPayload()))
            .toList();
    }

    private Map<String, Object> read(final String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (IOException e) {
            throw new UncheckedIOException("Unreadable runtime overlay event payload", e);
        }
    }
}
