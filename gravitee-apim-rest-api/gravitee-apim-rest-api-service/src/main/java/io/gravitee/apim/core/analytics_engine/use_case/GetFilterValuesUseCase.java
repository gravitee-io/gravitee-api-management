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
import io.gravitee.apim.core.analytics.domain_service.AiWorkspaceAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoaderResolver;
import io.gravitee.apim.core.analytics_engine.domain_service.FilterValueNameResolver;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsScope;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterValue;
import io.gravitee.apim.core.analytics_engine.model.FilterValuesPage;
import io.gravitee.apim.core.analytics_engine.model.TrafficScope;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.analytics_engine.query_service.FilterValuesQueryService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.application.query_service.ApplicationQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.observability.model.Signal;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetFilterValuesUseCase {

    /** Upper bound aligned with OpenAPI `page` parameter for observability filter values. */
    private static final int MAX_PAGE = 10_000;

    private static final Set<FilterSpec.Name> ID_BASED_FILTERS = Set.of(
        FilterSpec.Name.API,
        FilterSpec.Name.APPLICATION,
        FilterSpec.Name.PLAN,
        FilterSpec.Name.API_PRODUCT
    );

    private static final Map<ApiType, String> PLATFORM_TO_WIRE_API_TYPE = Map.of(
        ApiType.PROXY,
        "HTTP_PROXY",
        ApiType.LLM_PROXY,
        "LLM",
        ApiType.MCP_PROXY,
        "MCP",
        ApiType.A2A_PROXY,
        "A2A",
        ApiType.MESSAGE,
        "MESSAGE",
        ApiType.NATIVE,
        "NATIVE",
        ApiType.EDGE,
        "EDGE"
    );

    private final AnalyticsDefinitionQueryService definitionQueryService;
    private final FilterValuesQueryService filterValuesQueryService;
    private final FilterValueNameResolver filterValueNameResolver;
    private final AnalyticsQueryContextLoaderResolver contextLoader;
    private final ApplicationQueryService applicationQueryService;
    private final PlanQueryService planQueryService;
    private final ApiProductQueryService apiProductQueryService;
    private final AiWorkspaceAnalyticsApisDomainService workspaceApis;

    public record Input(
        AuditInfo auditInfo,
        String filterName,
        Instant from,
        Instant to,
        int page,
        int perPage,
        String query,
        Set<ApiType> apiTypes,
        AnalyticsScope scope,
        /**
         * Entrypoints whose calls belong to one of {@code apiTypes} whatever the API's type, so an API of another
         * type enters through those calls only. Empty when no entrypoint adds one of the kinds.
         */
        Set<String> trafficEntrypointIds,
        /**
         * The single API product the request is scoped to, or {@code null}: only the calls made through it count. A reader
         * of that AI workspace also gets its APIs, for the aggregates its calls show them.
         */
        String apiProductId
    ) {
        public Input {
            trafficEntrypointIds = trafficEntrypointIds == null ? Set.of() : Set.copyOf(trafficEntrypointIds);
        }

        public Input(
            AuditInfo auditInfo,
            String filterName,
            Instant from,
            Instant to,
            int page,
            int perPage,
            String query,
            Set<ApiType> apiTypes,
            AnalyticsScope scope,
            Set<String> trafficEntrypointIds
        ) {
            this(auditInfo, filterName, from, to, page, perPage, query, apiTypes, scope, trafficEntrypointIds, null);
        }

        public Input(AuditInfo auditInfo, String filterName, Instant from, Instant to, int page, int perPage, String query) {
            this(auditInfo, filterName, from, to, page, perPage, query, Set.of(), AnalyticsScope.MANAGEMENT);
        }

        public Input(
            AuditInfo auditInfo,
            String filterName,
            Instant from,
            Instant to,
            int page,
            int perPage,
            String query,
            Set<ApiType> apiTypes,
            AnalyticsScope scope
        ) {
            this(auditInfo, filterName, from, to, page, perPage, query, apiTypes, scope, Set.of());
        }

        public Input(
            AuditInfo auditInfo,
            String filterName,
            Instant from,
            Instant to,
            int page,
            int perPage,
            String query,
            Set<ApiType> apiTypes
        ) {
            this(auditInfo, filterName, from, to, page, perPage, query, apiTypes, AnalyticsScope.MANAGEMENT);
        }
    }

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

        var dataKind = dataKindOf(filterSpec);
        var analyticsContext = withWorkspaceApis(input, dataKind, contextLoader.load(input.auditInfo(), input.scope(), dataKind));
        TrafficScope trafficScope = null;
        if (input.apiTypes() != null && !input.apiTypes().isEmpty()) {
            var ofTheTypes = narrowByApiTypes(analyticsContext, input.apiTypes());
            if (input.trafficEntrypointIds().isEmpty()) {
                analyticsContext = ofTheTypes;
            } else {
                // Any readable API may have calls through those entrypoints: the context stays whole.
                trafficScope = new TrafficScope(ofTheTypes.authorizedApiIds(), input.trafficEntrypointIds());
            }
        }

        return switch (filterSpec.type()) {
            case ENUM -> handleEnum(filterSpec, input);
            case KEYWORD -> handleKeyword(input, filterSpecName, analyticsContext, trafficScope);
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

        if (filterSpec.name() == FilterSpec.Name.API_TYPE && input.apiTypes() != null && !input.apiTypes().isEmpty()) {
            Set<String> allowedWireValues = input
                .apiTypes()
                .stream()
                .map(PLATFORM_TO_WIRE_API_TYPE::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            stream = stream.filter(allowedWireValues::contains);
        }

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

    private Output handleKeyword(
        Input input,
        FilterSpec.Name filterSpecName,
        AnalyticsQueryContext analyticsContext,
        TrafficScope trafficScope
    ) {
        Objects.requireNonNull(analyticsContext);

        boolean isIdBased = ID_BASED_FILTERS.contains(filterSpecName);
        boolean hasQuery = input.query() != null && !input.query().isBlank();

        if (isIdBased) {
            if (hasQuery) {
                return handleIdBasedSearch(input, filterSpecName, analyticsContext, trafficScope);
            }
            return handleIdBasedNoSearch(input, filterSpecName, analyticsContext, trafficScope);
        }

        return handleDirectValueKeyword(input, filterSpecName, analyticsContext, trafficScope);
    }

    private AnalyticsQueryContext inTrafficScope(Input input, AnalyticsQueryContext analyticsContext, TrafficScope trafficScope) {
        return trafficScope == null
            ? analyticsContext
            : restrictTo(analyticsContext, apisInTrafficScope(input, analyticsContext, trafficScope));
    }

    /**
     * The readable APIs a module scoped by kind sees: those its kinds name by type, plus those with calls through
     * an entrypoint adding one of its kinds in the period. Only the calls tell the second group apart.
     */
    private Set<String> apisInTrafficScope(Input input, AnalyticsQueryContext analyticsContext, TrafficScope trafficScope) {
        var others = analyticsContext
            .authorizedApiIds()
            .stream()
            .filter(id -> !trafficScope.apiIds().contains(id))
            .collect(Collectors.toSet());
        var apis = new HashSet<>(trafficScope.apiIds());
        if (others.isEmpty()) {
            return apis;
        }
        searchValues(input, FilterSpec.Name.API, 1, others.size(), null, others, new TrafficScope(Set.of(), trafficScope.entrypointIds()))
            .data()
            .forEach(value -> apis.add(value.value()));
        return apis;
    }

    /** A caller scoped neither by kind nor by API product sends exactly the query it always did. */
    private FilterValuesPage searchValues(
        Input input,
        FilterSpec.Name filterName,
        int page,
        int size,
        String searchPattern,
        Set<String> apiIds,
        TrafficScope trafficScope
    ) {
        var organizationId = input.auditInfo().organizationId();
        var environmentId = input.auditInfo().environmentId();
        if (input.apiProductId() != null) {
            return filterValuesQueryService.searchFilterValues(
                organizationId,
                environmentId,
                filterName,
                input.from(),
                input.to(),
                page,
                size,
                null,
                searchPattern,
                apiIds,
                trafficScope,
                input.apiProductId()
            );
        }
        if (trafficScope == null) {
            return filterValuesQueryService.searchFilterValues(
                organizationId,
                environmentId,
                filterName,
                input.from(),
                input.to(),
                page,
                size,
                null,
                searchPattern,
                apiIds
            );
        }
        return filterValuesQueryService.searchFilterValues(
            organizationId,
            environmentId,
            filterName,
            input.from(),
            input.to(),
            page,
            size,
            null,
            searchPattern,
            apiIds,
            trafficScope
        );
    }

    private Output handleIdBasedSearch(
        Input input,
        FilterSpec.Name filterSpecName,
        AnalyticsQueryContext analyticsContext,
        TrafficScope trafficScope
    ) {
        boolean hasQuery = input.query() != null && !input.query().isBlank();
        var lowerQuery = hasQuery ? input.query().toLowerCase() : null;

        if (filterSpecName == FilterSpec.Name.API) {
            return searchApisByNameFromContext(input, lowerQuery, inTrafficScope(input, analyticsContext, trafficScope));
        }
        if (filterSpecName == FilterSpec.Name.APPLICATION) {
            return searchApplicationsByNameInCalls(input, lowerQuery, analyticsContext);
        }
        if (filterSpecName == FilterSpec.Name.PLAN) {
            return searchPlansByNameFromAuthorizedApis(input, lowerQuery, inTrafficScope(input, analyticsContext, trafficScope));
        }
        if (filterSpecName == FilterSpec.Name.API_PRODUCT) {
            return searchApiProductsByNameInCalls(input, lowerQuery, analyticsContext);
        }
        return new Output(new FilterValuesPage(Collections.emptyList(), null, 0L));
    }

    /** The readable APIs by name; on a request scoped to an API product, those with calls made through it only. */
    private Output searchApisByNameFromContext(Input input, String lowerQuery, AnalyticsQueryContext analyticsContext) {
        var named = analyticsContext
            .apiNamesById()
            .entrySet()
            .stream()
            .filter(e -> {
                var name = e.getValue();
                return name != null && name.toLowerCase().contains(lowerQuery);
            })
            .toList();
        var inCalls = input.apiProductId() == null
            ? null
            : findInCalls(input, FilterSpec.Name.API, named.stream().map(Map.Entry::getKey), analyticsContext);
        var matching = named
            .stream()
            .filter(e -> inCalls == null || inCalls.contains(e.getKey()))
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

    /** Applications are not attached to an API: only the calls of the readable APIs tell which ones the caller may see. */
    private Output searchApplicationsByNameInCalls(Input input, String lowerQuery, AnalyticsQueryContext analyticsContext) {
        var environmentId = input.auditInfo().environmentId();
        var named = applicationQueryService
            .findByEnvironment(environmentId)
            .stream()
            .filter(app -> app.getName() != null && app.getName().toLowerCase().contains(lowerQuery))
            .toList();
        var inCalls = findInCalls(input, FilterSpec.Name.APPLICATION, named.stream().map(BaseApplicationEntity::getId), analyticsContext);
        var matching = named
            .stream()
            .filter(app -> inCalls.contains(app.getId()))
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

    private Output searchApiProductsByNameInCalls(Input input, String lowerQuery, AnalyticsQueryContext analyticsContext) {
        var environmentId = input.auditInfo().environmentId();

        var stream = apiProductQueryService
            .findByEnvironmentId(environmentId)
            .stream()
            // Exclude products with no name — they cannot be meaningfully displayed.
            .filter(ap -> ap.getName() != null);

        if (lowerQuery != null) {
            stream = stream.filter(ap -> ap.getName().toLowerCase().contains(lowerQuery));
        }

        var named = stream.toList();
        var inCalls = findInCalls(input, FilterSpec.Name.API_PRODUCT, named.stream().map(ApiProduct::getId), analyticsContext);
        var matching = named
            .stream()
            .filter(ap -> inCalls.contains(ap.getId()))
            .sorted(Comparator.comparing(ApiProduct::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(ApiProduct::getId))
            .toList();

        var totalFiltered = (long) matching.size();
        var fromIndex = Math.min((input.page() - 1) * input.perPage(), matching.size());
        var toIndex = Math.min(fromIndex + input.perPage(), matching.size());
        var pageProducts = matching.subList(fromIndex, toIndex);
        var values = pageProducts
            .stream()
            .map(ap -> new FilterValue(ap.getName(), ap.getId()))
            .toList();

        return new Output(new FilterValuesPage(values, null, totalFiltered));
    }

    /** A field identifying callers, or one the logs only show, lists the values of the requests the caller reads. */
    private static ApiDataKind dataKindOf(FilterSpec filterSpec) {
        var perRequest = filterSpec.name().isIdentifying() || !filterSpec.appliesTo(Signal.ANALYTICS);
        return perRequest ? ApiDataKind.PER_REQUEST : ApiDataKind.AGGREGATES;
    }

    private Set<String> findInCalls(Input input, FilterSpec.Name filterName, Stream<String> ids, AnalyticsQueryContext analyticsContext) {
        var values = ids.collect(Collectors.toSet());
        if (input.apiProductId() != null) {
            return filterValuesQueryService.findValuesInCalls(
                input.auditInfo().organizationId(),
                input.auditInfo().environmentId(),
                filterName,
                input.from(),
                input.to(),
                values,
                analyticsContext.authorizedApiIds(),
                input.apiProductId()
            );
        }
        return filterValuesQueryService.findValuesInCalls(
            input.auditInfo().organizationId(),
            input.auditInfo().environmentId(),
            filterName,
            input.from(),
            input.to(),
            values,
            analyticsContext.authorizedApiIds()
        );
    }

    /**
     * The AI workspace a request is scoped to adds its APIs for its reader, for aggregates only: every query of the
     * request keeps the calls made through that workspace, so another workspace sharing an API never shows.
     */
    private AnalyticsQueryContext withWorkspaceApis(Input input, ApiDataKind dataKind, AnalyticsQueryContext context) {
        if (input.apiProductId() == null || dataKind != ApiDataKind.AGGREGATES) {
            return context;
        }
        var auditInfo = input.auditInfo();
        var apis = workspaceApis.findWorkspaceApis(
            new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId()),
            auditInfo.actor().userId(),
            input.apiProductId()
        );
        if (apis.isEmpty()) {
            return context;
        }
        var authorizedApiIds = new HashSet<>(context.authorizedApiIds());
        var apiNamesById = new HashMap<>(context.apiNamesById());
        var apiIdsByType = new HashMap<ApiType, Set<String>>();
        context.apiIdsByType().forEach((type, ids) -> apiIdsByType.put(type, new HashSet<>(ids)));
        for (var api : apis) {
            authorizedApiIds.add(api.getId());
            if (api.getName() != null) {
                apiNamesById.putIfAbsent(api.getId(), api.getName());
            }
            if (api.getType() != null) {
                apiIdsByType.computeIfAbsent(api.getType(), type -> new HashSet<>()).add(api.getId());
            }
        }
        return new AnalyticsQueryContext(
            context.auditInfo(),
            context.executionContext(),
            authorizedApiIds,
            apiNamesById,
            context.applicationNamesById(),
            apiIdsByType,
            context.restrictedToApplicationIds()
        );
    }

    private Output searchPlansByNameFromAuthorizedApis(Input input, String lowerQuery, AnalyticsQueryContext analyticsContext) {
        Objects.requireNonNull(analyticsContext);
        var environmentId = input.auditInfo().environmentId();
        var apiIds = analyticsContext.authorizedApiIds();
        if (apiIds == null || apiIds.isEmpty()) {
            return new Output(new FilterValuesPage(Collections.emptyList(), null, 0L));
        }

        var named = planQueryService
            .findAllByApiIds(apiIds, Set.of(environmentId))
            .stream()
            .filter(plan -> plan.getName() != null && plan.getName().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toMap(Plan::getId, p -> p, (a, b) -> a, LinkedHashMap::new))
            .values();
        // Offered like the labels resolve them: a plan without calls in the period has none to show.
        var inCalls = findInCalls(input, FilterSpec.Name.PLAN, named.stream().map(Plan::getId), analyticsContext);
        var matching = named
            .stream()
            .filter(plan -> inCalls.contains(plan.getId()))
            .sorted(Comparator.comparing(Plan::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(Plan::getId))
            .toList();

        var totalFiltered = (long) matching.size();
        var fromIndex = Math.min((input.page() - 1) * input.perPage(), matching.size());
        var toIndex = Math.min(fromIndex + input.perPage(), matching.size());
        var pagePlans = matching.subList(fromIndex, toIndex);
        // Labelled like the plan buckets and chips: plan names are unique per API only.
        var labels = filterValueNameResolver.planLabels(environmentId, pagePlans);
        var values = pagePlans
            .stream()
            .map(plan -> new FilterValue(labels.getOrDefault(plan.getId(), plan.getName()), plan.getId()))
            .toList();

        return new Output(new FilterValuesPage(values, null, totalFiltered));
    }

    private Output handleIdBasedNoSearch(
        Input input,
        FilterSpec.Name filterSpecName,
        AnalyticsQueryContext analyticsContext,
        TrafficScope trafficScope
    ) {
        var environmentId = input.auditInfo().environmentId();

        var esPage = searchValues(
            input,
            filterSpecName,
            input.page(),
            input.perPage(),
            null,
            analyticsContext.authorizedApiIds(),
            trafficScope
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

    private Output handleDirectValueKeyword(
        Input input,
        FilterSpec.Name filterSpecName,
        AnalyticsQueryContext analyticsContext,
        TrafficScope trafficScope
    ) {
        var page = searchValues(
            input,
            filterSpecName,
            input.page(),
            input.perPage(),
            input.query(),
            analyticsContext.authorizedApiIds(),
            trafficScope
        );
        return new Output(page);
    }

    private FilterSpec.Name validateFilterName(String filterName) {
        var offered = Arrays.stream(FilterSpec.Name.values())
            .filter(name -> !name.isInternal())
            .toList();
        return offered
            .stream()
            .filter(name -> name.name().equals(filterName))
            .findFirst()
            .orElseThrow(() ->
                new ValidationDomainException(
                    "Invalid filter name",
                    Map.of("invalidName", String.valueOf(filterName), "validNames", offered.toString())
                )
            );
    }

    private static AnalyticsQueryContext narrowByApiTypes(AnalyticsQueryContext context, Set<ApiType> apiTypes) {
        Set<String> allowedIds = new HashSet<>();
        for (var type : apiTypes) {
            var idsForType = context.apiIdsByType().get(type);
            if (idsForType != null) {
                allowedIds.addAll(idsForType);
            }
        }
        return restrictTo(context, allowedIds);
    }

    private static AnalyticsQueryContext restrictTo(AnalyticsQueryContext context, Set<String> allowedIds) {
        var narrowedAuthorizedIds = context.authorizedApiIds().stream().filter(allowedIds::contains).collect(Collectors.toSet());
        var narrowedNames = context
            .apiNamesById()
            .entrySet()
            .stream()
            .filter(e -> narrowedAuthorizedIds.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new AnalyticsQueryContext(
            context.auditInfo(),
            context.executionContext(),
            narrowedAuthorizedIds,
            narrowedNames,
            context.applicationNamesById(),
            context.apiIdsByType(),
            context.restrictedToApplicationIds()
        );
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
