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

import static io.gravitee.apim.core.analytics_engine.model.FilterSpec.Operator.IN;

import io.gravitee.apim.core.analytics_engine.domain_service.FilterPreProcessor;
import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricsContext;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.v4.ApiType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class ApiTypePreProcessor implements FilterPreProcessor {

    @Override
    public List<Filter> buildFilters(MetricsContext context, List<Filter> requestFilters) {
        if (context.apis().isEmpty()) {
            return List.of(new Filter(FilterSpec.Name.API, FilterSpec.Operator.IN, Collections.emptyList()));
        }

        var apiTypes = getEffectiveFilterByApiType(requestFilters);
        if (apiTypes.isEmpty()) {
            return Collections.emptyList();
        }

        if (apiTypes.get().isEmpty()) {
            return List.of(new Filter(FilterSpec.Name.API, FilterSpec.Operator.IN, Collections.emptyList()));
        }

        var apiIds = context
            .apis()
            .get()
            .stream()
            .filter(api -> apiTypes.get().contains(api.getType()))
            .map(Api::getId)
            .toList();

        return List.of(new Filter(FilterSpec.Name.API, FilterSpec.Operator.IN, apiIds));
    }

    Optional<List<ApiType>> getEffectiveFilterByApiType(Collection<Filter> filters) {
        if (filters.isEmpty()) {
            return Optional.empty();
        }

        var apiNameFilters = filters
            .stream()
            .filter(filter -> filter.name() == FilterSpec.Name.API_NAME)
            .toList();

        if (apiNameFilters.isEmpty()) {
            return Optional.empty();
        }

        var normalizedFilters = normalizeFilters(apiNameFilters);

        if (normalizedFilters.size() != 1) {
            // Filters are AND-eÏd, so two or more filters by API_NAME lead to no result.
            return Optional.of(Collections.emptyList());
        }

        var filter = normalizedFilters.stream().findAny().get();
        var filterValue = filter.value();

        if (filterValue instanceof List<?> listValue) {
            var apiTypes = listValue.stream().map(Object::toString).map(this::mapApiName).toList();
            if (apiTypes.contains(null)) {
                return Optional.of(Collections.emptyList());
            }

            return Optional.of(apiTypes);
        }

        return Optional.of(Collections.emptyList());
    }

    private Set<Filter> normalizeFilters(List<Filter> filters) {
        return filters
            .stream()
            .map(filter ->
                switch (filter.operator()) {
                    case FilterSpec.Operator.EQ -> new Filter(filter.name(), IN, List.of(filter.value()));
                    case FilterSpec.Operator.IN -> {
                        if (filter.value() instanceof List<?> listValue) {
                            var values = listValue.stream().filter(Objects::nonNull).map(Object::toString).sorted().toList();
                            yield new Filter(filter.name(), IN, values);
                        }
                        yield new Filter(filter.name(), IN, Collections.emptyList());
                    }
                    default -> new Filter(filter.name(), IN, Collections.emptyList());
                }
            )
            .collect(Collectors.toSet());
    }

    private ApiType mapApiName(String apiName) {
        try {
            return switch (ApiSpec.Name.valueOf(apiName)) {
                case HTTP_PROXY -> ApiType.PROXY;
                case MESSAGE -> ApiType.MESSAGE;
                case KAFKA -> ApiType.NATIVE;
                case LLM -> ApiType.LLM_PROXY;
                case MCP -> ApiType.MCP_PROXY;
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
