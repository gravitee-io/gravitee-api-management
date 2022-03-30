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
package io.gravitee.rest.api.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.NewEventEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Titouan COMPIEGNE
 */
public interface EventService {
    EventEntity findById(ExecutionContext executionContext, String id);

    EventEntity create(ExecutionContext executionContext, final Set<String> environments, NewEventEntity event);

    EventEntity create(
        ExecutionContext executionContext,
        final Set<String> environmentsIds,
        EventType type,
        String payload,
        Map<String, String> properties
    );

    void delete(String eventId);

    Page<EventEntity> search(
        ExecutionContext executionContext,
        List<EventType> eventTypes,
        Map<String, Object> properties,
        long from,
        long to,
        int page,
        int size,
        final List<String> environments
    );

    <T> Page<T> search(
        ExecutionContext executionContext,
        List<EventType> eventTypes,
        Map<String, Object> properties,
        long from,
        long to,
        int page,
        int size,
        Function<EventEntity, T> mapper,
        final List<String> environmentsIds
    );

    <T> Page<T> search(
        ExecutionContext executionContext,
        List<EventType> eventTypes,
        Map<String, Object> properties,
        long from,
        long to,
        int page,
        int size,
        Function<EventEntity, T> mapper,
        Predicate<T> filter,
        final List<String> environmentsIds
    );

    Collection<EventEntity> search(ExecutionContext executionContext, EventQuery query);
}
