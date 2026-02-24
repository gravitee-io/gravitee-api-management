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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import io.gravitee.apim.core.analytics_engine.domain_service.QueryFilterTransformer;
import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.definition.model.v4.ApiType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Transforms analytics query filters by replacing any {@code API_TYPE} filter
 * with a single tight {@code API IN [...]} filter. When an {@code API_TYPE}
 * filter is present, the matching API IDs are looked up from
 * {@link AnalyticsQueryContext#apiIdsByType()} (already scoped to authorized
 * APIs). When absent, the authorized IDs are used as-is.
 *
 * @author GraviteeSource Team
 */
public class ApiTypeFilterTransformer implements QueryFilterTransformer {

    private static final Map<String, ApiType> API_TYPE_MAPPING = Map.of(
        "HTTP_PROXY",
        ApiType.PROXY,
        "LLM",
        ApiType.LLM_PROXY,
        "MESSAGE",
        ApiType.MESSAGE,
        "MCP",
        ApiType.MCP_PROXY,
        "KAFKA",
        ApiType.NATIVE
    );

    @Override
    public List<Filter> transform(AnalyticsQueryContext context, List<Filter> filters) {
        var transformed = filters
            .stream()
            .filter(f -> f.name() != FilterSpec.Name.API_TYPE)
            .collect(Collectors.toCollection(ArrayList::new));

        var apiIds = findApiTypeFilter(filters)
            .map(f -> resolveApiIds(context, f))
            .orElseGet(context::authorizedApiIds);

        transformed.add(new Filter(FilterSpec.Name.API, FilterSpec.Operator.IN, apiIds));

        return transformed;
    }

    private static Optional<Filter> findApiTypeFilter(List<Filter> filters) {
        return filters
            .stream()
            .filter(f -> f.name() == FilterSpec.Name.API_TYPE)
            .findFirst();
    }

    private static Collection<String> resolveApiIds(AnalyticsQueryContext context, Filter apiTypeFilter) {
        return toApiTypes(apiTypeFilter)
            .stream()
            .flatMap(type -> context.apiIdsByType().getOrDefault(type, Set.of()).stream())
            .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private static List<ApiType> toApiTypes(Filter apiTypeFilter) {
        if (apiTypeFilter.operator() == FilterSpec.Operator.IN) {
            return ((Collection<String>) apiTypeFilter.value()).stream().map(ApiTypeFilterTransformer::mapApiType).toList();
        }
        return List.of(mapApiType((String) apiTypeFilter.value()));
    }

    private static ApiType mapApiType(String value) {
        var apiType = API_TYPE_MAPPING.get(value);
        if (apiType == null) {
            throw InvalidQueryException.forUnknownAPIType(value);
        }
        return apiType;
    }
}
