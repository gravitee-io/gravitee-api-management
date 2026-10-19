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
package inmemory;

import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterValue;
import io.gravitee.apim.core.analytics_engine.model.FilterValuesPage;
import io.gravitee.apim.core.analytics_engine.model.TrafficScope;
import io.gravitee.apim.core.analytics_engine.query_service.FilterValuesQueryService;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Calls stored as their field values: a value is offered when a stored call of the given APIs holds it. */
public class FilterValuesQueryServiceInMemory
    extends AbstractQueryServiceInMemory<FilterValuesQueryServiceInMemory.Call>
    implements FilterValuesQueryService {

    public record Call(String apiId, String apiProductId, Map<FilterSpec.Name, String> fields) {
        String valueOf(FilterSpec.Name filterName) {
            return switch (filterName) {
                case API -> apiId;
                case API_PRODUCT -> apiProductId;
                default -> fields.get(filterName);
            };
        }
    }

    @Override
    public FilterValuesPage searchFilterValues(
        String organizationId,
        String environmentId,
        FilterSpec.Name filterName,
        Instant from,
        Instant to,
        int page,
        int size,
        Map<String, Object> afterKey,
        String searchPattern,
        Set<String> authorizedApiIds,
        TrafficScope trafficScope,
        String apiProductId
    ) {
        if (trafficScope != null) {
            throw new UnsupportedOperationException("Calls are not stored with their entrypoint");
        }
        var values = valuesOf(filterName, authorizedApiIds, apiProductId)
            .filter(value -> searchPattern == null || value.equalsIgnoreCase(searchPattern))
            .sorted()
            .toList();
        var pageValues = values.stream().skip((long) (page - 1) * size).limit(size).map(FilterValue::new).toList();
        return new FilterValuesPage(pageValues, null, (long) values.size());
    }

    @Override
    public Set<String> findValuesInCalls(
        String organizationId,
        String environmentId,
        FilterSpec.Name filterName,
        Instant from,
        Instant to,
        Set<String> values,
        Set<String> authorizedApiIds,
        String apiProductId
    ) {
        return valuesOf(filterName, authorizedApiIds, apiProductId).filter(values::contains).collect(Collectors.toSet());
    }

    private Stream<String> valuesOf(FilterSpec.Name filterName, Set<String> authorizedApiIds, String apiProductId) {
        return storage
            .stream()
            .filter(call -> authorizedApiIds == null || authorizedApiIds.contains(call.apiId()))
            .filter(call -> apiProductId == null || apiProductId.equals(call.apiProductId()))
            .map(call -> call.valueOf(filterName))
            .filter(Objects::nonNull)
            .distinct();
    }
}
