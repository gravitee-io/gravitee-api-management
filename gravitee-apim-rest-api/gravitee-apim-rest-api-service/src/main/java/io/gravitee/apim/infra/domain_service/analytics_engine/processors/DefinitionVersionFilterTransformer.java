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
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.definition.model.DefinitionVersion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Transforms analytics query filters by replacing any {@code DEFINITION_VERSION}
 * filter with a single tight {@code API IN [...]} filter. When a
 * {@code DEFINITION_VERSION} filter is present, the matching API IDs are looked
 * up from {@link AnalyticsQueryContext#apiIdsByDefinitionVersion()} (already
 * scoped to authorized APIs). When absent, the filter chain is left untouched.
 *
 * @author GraviteeSource Team
 */
public class DefinitionVersionFilterTransformer implements QueryFilterTransformer {

    private static final Map<String, DefinitionVersion> VERSION_MAPPING = Map.of(
        "V2",
        DefinitionVersion.V2,
        "V4",
        DefinitionVersion.V4,
        "FEDERATED",
        DefinitionVersion.FEDERATED,
        "FEDERATED_AGENT",
        DefinitionVersion.FEDERATED_AGENT
    );

    @Override
    public List<Filter> transform(AnalyticsQueryContext context, List<Filter> filters) {
        var definitionVersionFilter = findDefinitionVersionFilter(filters);

        if (definitionVersionFilter.isEmpty()) {
            return filters;
        }

        var transformed = filters
            .stream()
            .filter(f -> f.name() != FilterSpec.Name.DEFINITION_VERSION)
            .collect(Collectors.toCollection(ArrayList::new));

        var apiIds = resolveApiIds(context, definitionVersionFilter.get());

        transformed.add(new Filter(FilterSpec.Name.API, FilterOperator.IN, apiIds));

        return transformed;
    }

    private static Optional<Filter> findDefinitionVersionFilter(List<Filter> filters) {
        return filters
            .stream()
            .filter(f -> f.name() == FilterSpec.Name.DEFINITION_VERSION)
            .findFirst();
    }

    private static Collection<String> resolveApiIds(AnalyticsQueryContext context, Filter filter) {
        return toDefinitionVersions(filter)
            .stream()
            .flatMap(version -> context.apiIdsByDefinitionVersion().getOrDefault(version, Set.of()).stream())
            .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private static List<DefinitionVersion> toDefinitionVersions(Filter filter) {
        if (filter.operator() == FilterOperator.IN) {
            return ((Collection<String>) filter.value()).stream().map(DefinitionVersionFilterTransformer::mapDefinitionVersion).toList();
        }
        return List.of(mapDefinitionVersion((String) filter.value()));
    }

    private static DefinitionVersion mapDefinitionVersion(String value) {
        if (value == null) {
            throw InvalidQueryException.forNullFilterValue(FilterSpec.Name.DEFINITION_VERSION.name());
        }
        var version = VERSION_MAPPING.get(value);
        if (version == null) {
            throw InvalidQueryException.forUnknownDefinitionVersion(value);
        }
        return version;
    }
}
