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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.event.model.Event;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EventAdapterTest {

    private EventAdapter cut = EventAdapter.INSTANCE;

    @Test
    void should_convert_entity_to_event() {
        EventEntity entity = new EventEntity();
        entity.setId("id");
        entity.setEnvironments(Set.of("env1", "env2"));
        entity.setPayload("payload");
        entity.setType(EventType.DEBUG_API);
        entity.setProperties(
            Map.of(Event.EventProperties.API_ID.getLabel(), "api-id", Event.EventProperties.GATEWAY_ID.getLabel(), "gateway-id")
        );

        final Event result = cut.fromEntity(entity);
        assertThat(result)
            .extracting(Event::getId, Event::getEnvironments, Event::getPayload, Event::getType, Event::getProperties)
            .containsExactly(
                entity.getId(),
                entity.getEnvironments(),
                entity.getPayload(),
                entity.getType(),
                Map.of(Event.EventProperties.API_ID, "api-id", Event.EventProperties.GATEWAY_ID, "gateway-id")
            );
    }

    @Test
    void should_convert_event_to_entity() {
        Event event = Event.builder()
            .id("id")
            .environments(Set.of("env1", "env2"))
            .payload("payload")
            .type(EventType.DEBUG_API)
            .properties(new EnumMap<>(Map.of(Event.EventProperties.API_ID, "api-id", Event.EventProperties.GATEWAY_ID, "gateway-id")))
            .build();

        final EventEntity result = cut.toEntity(event);
        assertThat(result)
            .extracting(
                EventEntity::getId,
                EventEntity::getEnvironments,
                EventEntity::getPayload,
                EventEntity::getType,
                EventEntity::getProperties
            )
            .containsExactly(
                event.getId(),
                event.getEnvironments(),
                event.getPayload(),
                event.getType(),
                Map.of(Event.EventProperties.API_ID.getLabel(), "api-id", Event.EventProperties.GATEWAY_ID.getLabel(), "gateway-id")
            );
    }
}
