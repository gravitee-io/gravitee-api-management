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
package io.gravitee.apim.core.event.query_service;

import io.gravitee.apim.core.event.model.Event;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface EventQueryService {
    SearchResponse search(SearchQuery query, Pageable pageable);

    record SearchQuery(
        String environmentId,
        Optional<String> id,
        Optional<String> apiId,
        Collection<EventType> types,
        Map<Event.EventProperties, String> properties,
        Optional<Long> from,
        Optional<Long> to
    ) {}

    record SearchResponse(long total, List<Event> events) {}
}
