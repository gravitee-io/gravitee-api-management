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
package io.gravitee.apim.infra.domain_service.analytics_engine;

import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterValue;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FilterValueNameResolverImpl implements FilterValueNameResolver {

    private static final String UNKNOWN_APPLICATION_ID = "1";
    private static final String UNKNOWN_APPLICATION_NAME = "Unknown";

    private final ApiCrudService apiCrudService;
    private final ApiQueryService apiQueryService;
    private final ApplicationCrudService applicationCrudService;
    private final PlanCrudService planCrudService;

    @Override
    public Map<String, String> resolveNames(String environmentId, FilterSpec.Name filterName, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        return switch (filterName) {
            case API -> apiCrudService.findByIds(ids).stream().collect(Collectors.toMap(api -> api.getId(), api -> api.getName()));
            case APPLICATION -> {
                var names = new HashMap<>(
                    applicationCrudService
                        .findByIds(ids, environmentId)
                        .stream()
                        .collect(Collectors.toMap(app -> app.getId(), app -> app.getName()))
                );
                names.put(UNKNOWN_APPLICATION_ID, UNKNOWN_APPLICATION_NAME);
                yield names;
            }
            case PLAN -> planCrudService.findByIds(ids).stream().collect(Collectors.toMap(plan -> plan.getId(), plan -> plan.getName()));
            default -> Collections.emptyMap();
        };
    }

    @Override
    public List<FilterValue> searchByName(String environmentId, FilterSpec.Name filterName, String query, int page, int perPage) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        return switch (filterName) {
            case API -> searchApisByName(environmentId, query, page, perPage);
            default -> Collections.emptyList();
        };
    }

    private List<FilterValue> searchApisByName(String environmentId, String query, int page, int perPage) {
        var criteria = ApiSearchCriteria.builder().name(query).environmentId(environmentId).build();

        var fieldFilter = ApiFieldFilter.builder().pictureExcluded(true).definitionExcluded(true).build();

        var skip = (long) (page - 1) * perPage;
        return apiQueryService
            .search(criteria, null, fieldFilter)
            .skip(skip)
            .limit(perPage)
            .map(api -> new FilterValue(api.getName(), api.getId()))
            .toList();
    }
}
