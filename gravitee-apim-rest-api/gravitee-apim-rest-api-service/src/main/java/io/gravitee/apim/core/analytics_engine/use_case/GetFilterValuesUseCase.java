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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoader;
import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterValue;
import io.gravitee.apim.core.analytics_engine.model.FilterValuesPage;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.analytics_engine.query_service.FilterValuesQueryService;
import io.gravitee.apim.core.application.query_service.ApplicationQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetFilterValuesUseCase {

    /** Upper bound aligned with OpenAPI `page` parameter for observability filter values. */
    private static final int MAX_PAGE = 10_000;

    private static final Set<FilterSpec.Name> ID_BASED_FILTERS = Set.of(
        FilterSpec.Name.API,
        FilterSpec.Name.APPLICATION,
        FilterSpec.Name.PLAN
    );

    private final AnalyticsDefinitionQueryService definitionQueryService;
    private final FilterValuesQueryService filterValuesQueryService;
    private final FilterValueNameResolver filterValueNameResolver;
    private final AnalyticsQueryContextLoader contextLoader;
    private final ApplicationQueryService applicationQueryService;
    private final PlanQueryService planQueryService;

    public record Input(AuditInfo auditInfo, String filterName, Instant from, Instant to, int page, int perPage, String query) {}

    public record Output(FilterValuesPage valuesPage) {}

    public Output execute(Input input) {
        if (input.page() < 1 || input.page() > MAX_PAGE) {
            throw new ValidationDomainException("page must be between 1 and " + MAX_PAGE, Map.of("page", String.valueOf(input.page())));
        }
        if (input.perPage() < 1 || input.perPage() > 100) {
            throw new ValidationDomainException("perPage must be between 1 and 100", Map.of("perPage", String.valueOf(input.perPage())));
        }

        var filterSpecName = validateFilterName(input.filterName());
        var filterSpec = findFilterSpec(filterSpecName);

        var analyticsContext = contextLoader.load(input.auditInfo());

        return switch (filterSpec.type()) {
            case ENUM -> handleEnum(filterSpec, input);
            case KEYWORD -> handleKeyword(input, filterSpecName, analyticsContext);
            case NUMBER, STRING -> throw new ValidationDomainException(
                "Filter type " + filterSpec.type() + " does not support value listing",
                Map.of("filterName", input.filterName(), "filterType", filterSpec.type().name())
            );
        };
    }

    private Output handleEnum(FilterSpec filterSpec, Input input) {
        var enumValues = filterSpec.enumValues();
        if (enumValues == null || enumValues.isEmpty()) {
            return new Output(new FilterValuesPage(Collections.emptyList(), null, 0L));
        }

        var stream = enumValues.stream();

        if (input.query() != null && !input.query().isBlank()) {
            var lowerQuery = input.query().toLowerCase();
            stream = stream.filter(v -> v.toLowerCase().contains(lowerQuery));
        }

        var allValues = stream.map(FilterValue::new).toList();

        var fromIndex = Math.min((input.page() - 1) * input.perPage(), allValues.size());
        var toIndex = Math.min(fromIndex + input.perPage(), allValues.size());
        var pageValues = allValues.subList(fromIndex, toIndex);

        return new Output(new FilterValuesPage(pageValues, null, (long) allValues.size()));
    }

    private Output handleKeyword(Input input, FilterSpec.Name filterSpecName, AnalyticsQueryContext analyticsContext) {
        Objects.requireNonNull(analyticsContext);

        boolean isIdBased = ID_BASED_FILTERS.contains(filterSpecName);
        boolean hasQuery = input.query() != null && !input.query().isBlank();

        if (isIdBased && hasQuery) {
            return handleIdBasedSearch(input, filterSpecName, analyticsContext);
        }

        if (isIdBased) {
            return handleIdBasedNoSearch(input, filterSpecName, analyticsContext);
        }

        return handleDirectValueKeyword(input, filterSpecName, analyticsContext);
    }

    private Output handleIdBasedSearch(Input input, FilterSpec.Name filterSpecName, AnalyticsQueryContext analyticsContext) {
        if (filterSpecName == FilterSpec.Name.API) {
            return searchApisByNameFromContext(input, analyticsContext);
        }
        if (filterSpecName == FilterSpec.Name.APPLICATION) {
            return searchApplicationsByNameFromEnvironment(input);
        }
        if (filterSpecName == FilterSpec.Name.PLAN) {
            return searchPlansByNameFromAuthorizedApis(input, analyticsContext);
        }
        return new Output(new FilterValuesPage(Collections.emptyList(), null, 0L));
    }

    private Output searchApisByNameFromContext(Input input, AnalyticsQueryContext analyticsContext) {
        var lowerQuery = input.query().toLowerCase();
        var matching = analyticsContext
            .apiNamesById()
            .entrySet()
            .stream()
            .filter(e -> {
                var name = e.getValue();
                return name != null && name.toLowerCase().contains(lowerQuery);
            })
            .sorted(
                Comparator.comparing(Map.Entry<String, String>::getValue, String.CASE_INSENSITIVE_ORDER).thenComparing(Map.Entry::getKey)
            )
            .toList();

        var totalFiltered = (long) matching.size();
        var fromIndex = Math.min((input.page() - 1) * input.perPage(), matching.size());
        var toIndex = Math.min(fromIndex + input.perPage(), matching.size());
        var pageEntries = matching.subList(fromIndex, toIndex);
        var values = pageEntries
            .stream()
            .map(e -> new FilterValue(e.getValue(), e.getKey()))
            .toList();

        return new Output(new FilterValuesPage(values, null, totalFiltered));
    }

    private Output searchApplicationsByNameFromEnvironment(Input input) {
        var environmentId = input.auditInfo().environmentId();
        var lowerQuery = input.query().toLowerCase();
        var matching = applicationQueryService
            .findByEnvironment(environmentId)
            .stream()
            .filter(app -> app.getName() != null && app.getName().toLowerCase().contains(lowerQuery))
            .sorted(
                Comparator.comparing(BaseApplicationEntity::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(
                    BaseApplicationEntity::getId
                )
            )
            .toList();

        var totalFiltered = (long) matching.size();
        var fromIndex = Math.min((input.page() - 1) * input.perPage(), matching.size());
        var toIndex = Math.min(fromIndex + input.perPage(), matching.size());
        var pageApps = matching.subList(fromIndex, toIndex);
        var values = pageApps
            .stream()
            .map(app -> new FilterValue(app.getName(), app.getId()))
            .toList();

        return new Output(new FilterValuesPage(values, null, totalFiltered));
    }

    private Output searchPlansByNameFromAuthorizedApis(Input input, AnalyticsQueryContext analyticsContext) {
        Objects.requireNonNull(analyticsContext);
        var environmentId = input.auditInfo().environmentId();
        var apiIds = analyticsContext.authorizedApiIds();
        if (apiIds == null || apiIds.isEmpty()) {
            return new Output(new FilterValuesPage(Collections.emptyList(), null, 0L));
        }

        var lowerQuery = input.query().toLowerCase();
        var plans = planQueryService.findAllByApiIds(apiIds, Set.of(environmentId));
        var matching = plans
            .stream()
            .filter(plan -> plan.getName() != null && plan.getName().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toMap(Plan::getId, p -> p, (a, b) -> a, LinkedHashMap::new))
            .values()
            .stream()
            .sorted(Comparator.comparing(Plan::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(Plan::getId))
            .toList();

        var totalFiltered = (long) matching.size();
        var fromIndex = Math.min((input.page() - 1) * input.perPage(), matching.size());
        var toIndex = Math.min(fromIndex + input.perPage(), matching.size());
        var pagePlans = matching.subList(fromIndex, toIndex);
        var values = pagePlans
            .stream()
            .map(plan -> new FilterValue(plan.getName(), plan.getId()))
            .toList();

        return new Output(new FilterValuesPage(values, null, totalFiltered));
    }

    private Output handleIdBasedNoSearch(Input input, FilterSpec.Name filterSpecName, AnalyticsQueryContext analyticsContext) {
        var organizationId = input.auditInfo().organizationId();
        var environmentId = input.auditInfo().environmentId();

        var esPage = filterValuesQueryService.searchFilterValues(
            organizationId,
            environmentId,
            filterSpecName,
            input.from(),
            input.to(),
            input.page(),
            input.perPage(),
            null,
            null,
            analyticsContext.authorizedApiIds()
        );

        if (filterSpecName == FilterSpec.Name.API) {
            var resolvedValues = esPage
                .data()
                .stream()
                .map(fv -> new FilterValue(analyticsContext.apiNamesById().getOrDefault(fv.value(), fv.value()), fv.value()))
                .toList();
            return new Output(new FilterValuesPage(resolvedValues, esPage.afterKey()));
        }

        var ids = esPage.data().stream().map(FilterValue::value).toList();
        var namesByIds = filterValueNameResolver.resolveNames(environmentId, filterSpecName, ids);

        var resolvedValues = esPage
            .data()
            .stream()
            .map(fv -> new FilterValue(namesByIds.getOrDefault(fv.value(), fv.value()), fv.value()))
            .toList();

        return new Output(new FilterValuesPage(resolvedValues, esPage.afterKey()));
    }

    private Output handleDirectValueKeyword(Input input, FilterSpec.Name filterSpecName, AnalyticsQueryContext analyticsContext) {
        var organizationId = input.auditInfo().organizationId();
        var environmentId = input.auditInfo().environmentId();

        var page = filterValuesQueryService.searchFilterValues(
            organizationId,
            environmentId,
            filterSpecName,
            input.from(),
            input.to(),
            input.page(),
            input.perPage(),
            null,
            input.query(),
            analyticsContext.authorizedApiIds()
        );
        return new Output(page);
    }

    private FilterSpec.Name validateFilterName(String filterName) {
        try {
            return FilterSpec.Name.valueOf(filterName);
        } catch (IllegalArgumentException e) {
            throw new ValidationDomainException(
                "Invalid filter name",
                Map.of("invalidName", filterName, "validNames", Arrays.toString(FilterSpec.Name.values()))
            );
        }
    }

    private FilterSpec findFilterSpec(FilterSpec.Name name) {
        return definitionQueryService
            .getAllFilters()
            .stream()
            .filter(f -> f.name() == name)
            .findFirst()
            .orElseThrow(() -> new ValidationDomainException("Filter not found", Map.of("filterName", name.name())));
    }
}
