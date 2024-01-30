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

import io.gravitee.apim.core.event.model.Event;
import io.gravitee.rest.api.model.EventEntity;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface EventAdapter {
    EventAdapter INSTANCE = Mappers.getMapper(EventAdapter.class);

    @Mapping(target = "properties", expression = "java(computeEventProperties(source.getProperties()))")
    Event map(io.gravitee.repository.management.model.Event source);

    @Mapping(target = "properties", expression = "java(computeEventProperties(source.getProperties()))")
    Event fromEntity(EventEntity source);

    EventEntity toEntity(Event source);

    @Named("computeEventProperties")
    default EnumMap<Event.EventProperties, String> computeEventProperties(Map<String, String> properties) {
        return properties
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    entry -> Event.EventProperties.fromLabel(entry.getKey()),
                    Map.Entry::getValue,
                    (left, right) -> {
                        throw new IllegalArgumentException("Duplicate keys " + left + " and " + right);
                    },
                    () -> new EnumMap<>(Event.EventProperties.class)
                )
            );
    }

    default Map<String, String> toStringEventPropertiesMap(Map<Event.EventProperties, String> source) {
        return source.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getLabel(), Map.Entry::getValue));
    }
}
