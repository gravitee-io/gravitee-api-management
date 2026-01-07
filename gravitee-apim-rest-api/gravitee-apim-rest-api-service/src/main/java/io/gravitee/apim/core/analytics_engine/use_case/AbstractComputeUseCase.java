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
package io.gravitee.apim.core.analytics_engine.use_case;

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_API_TYPE;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author GraviteeSource Team
 */
@UseCase
class AbstractComputeUseCase {

    private final SearchEngineService searchEngineService;

    AbstractComputeUseCase(SearchEngineService searchEngineService) {
        this.searchEngineService = searchEngineService;
    }

    Optional<Filter> getFilterByApiName(Collection<Filter> filters) {
        var apiTypes = getEffectiveFilterByApiType(filters);
        if (apiTypes.isEmpty()) {
            return Optional.empty();
        }

        if (apiTypes.get().isEmpty()) {
            return Optional.of(new Filter(FilterSpec.Name.API, FilterSpec.Operator.IN, Collections.emptyList()));
        }

        var apiQueryBuilder = QueryBuilder.create(ApiEntity.class);
        apiQueryBuilder.addFilter(FIELD_API_TYPE, apiTypes.get());

        var searchResult = searchEngineService.search(GraviteeContext.getExecutionContext(), apiQueryBuilder.build());

        var apiIds = searchResult.getDocuments();

        var filter = new Filter(FilterSpec.Name.API, FilterSpec.Operator.IN, apiIds);

        return Optional.of(filter);
    }

    Optional<List<String>> getEffectiveFilterByApiType(Collection<Filter> filters) {
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

        if (apiNameFilters.size() > 1) {
            // Filters are AND-ed so multiple filters by API_NAME lead to no result.
            // TODO: Handle corner cases where multiple equivalent filters are added. e.g. EQ HTTP_PROXY and IN HTTP_PROXY.
            return Optional.of(Collections.emptyList());
        }

        var operator = apiNameFilters.getFirst().operator();
        switch (operator) {
            case FilterSpec.Operator.EQ:
                var stringValue = mapApiName(apiNameFilters.getFirst().value().toString());
                return Optional.of(List.of(stringValue));
            case FilterSpec.Operator.IN:
                var value = apiNameFilters.getFirst().value();
                if (value instanceof List) {
                    List<String> values = ((List<?>) value).stream().map(Object::toString).map(this::mapApiName).toList();
                    return Optional.of(values);
                }
                return Optional.of(Collections.emptyList());
            default:
                return Optional.of(Collections.emptyList());
        }
    }

    private String mapApiName(String apiName) {
        return switch (ApiSpec.Name.valueOf(apiName)) {
            case HTTP_PROXY -> "V4_HTTP_PROXY";
            case MESSAGE -> "V4_MESSAGE";
            case KAFKA -> "V4_KAFKA";
            case LLM -> "FEDERATED";
            case MCP -> "FEDERATED_AGENT";
        };
    }
}
