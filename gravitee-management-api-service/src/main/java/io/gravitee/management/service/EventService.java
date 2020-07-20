/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.management.model.EventEntity;
import io.gravitee.management.model.EventQuery;
import io.gravitee.management.model.EventType;
import io.gravitee.management.model.NewEventEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Titouan COMPIEGNE
 */
public interface EventService {

    EventEntity findById(String id);

    EventEntity create(NewEventEntity event);

    EventEntity create(EventType type, String payload, Map<String, String> properties);
    
    void delete(String eventId);

    Page<EventEntity> search(
            List<EventType> eventTypes, Map<String, Object> properties, long from, long to, int page, int size);

    <T> Page<T> search(List<EventType> eventTypes,
                       Map<String, Object> properties, long from, long to, int page, int size,
                       Function<EventEntity, T> mapper);

    <T> Page<T> search(List<EventType> eventTypes,
                       Map<String, Object> properties, long from, long to, int page, int size,
                       Function<EventEntity, T> mapper, Predicate<T> filter);

    Collection<EventEntity> search(EventQuery query);
}
