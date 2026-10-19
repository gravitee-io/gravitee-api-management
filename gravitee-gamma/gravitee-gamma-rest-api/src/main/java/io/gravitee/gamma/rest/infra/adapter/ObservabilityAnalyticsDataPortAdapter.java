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
package io.gravitee.gamma.rest.infra.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.analytics.domain_service.AiWorkspaceAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.domain_service.UnitEnrichmentPostProcessor;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.core.analytics_engine.model.TrafficScope;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.observability.model.NumberRange;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsFacetMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsNumberRange;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsSortSpec;
import io.gravitee.gamma.rest.core.observability.analytics.model.DataKind;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.AnalyticsRequestPipeline;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Adapter bridging the Gamma analytics port to the APIM analytics compute use cases. Translates
 * Gamma-native types to the analytics-engine model, builds the {@link AuditInfo} from the ambient
 * request context, and serializes APIM responses to {@link JsonNode} for framework independence.
 *
 * <p>A measures query runs one engine query per group, one after the other. The engine answers one
 * entry per metric name, measures merged, in no particular order: the response is rebuilt in request
 * order, per metric when they carry ids, per name otherwise.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class ObservabilityAnalyticsDataPortAdapter implements ObservabilityAnalyticsDataPort {

    private static final String HTTP_STATUS_CODE_GROUP = "HTTP_STATUS_CODE_GROUP";

    private final ComputeMeasuresUseCase computeMeasuresUseCase;
    private final ComputeFacetsUseCase computeFacetsUseCase;
    private final ComputeTimeSeriesUseCase computeTimeSeriesUseCase;
    private final AnalyticsQueryValidator analyticsQueryValidator;
    private final UnitEnrichmentPostProcessor unitEnrichmentPostProcessor;
    private final UserContextLoader userContextLoader;
    private final AiWorkspaceAnalyticsApisDomainService workspaceApis;
    private final AnalyticsQueryContextProvider queryContextProvider;
    private final ObjectMapper objectMapper;

    @Override
    public List<AccessibleApi> loadAccessibleApis(String organizationId, String environmentId, DataKind kind) {
        var auditInfo = currentAuditInfo(organizationId, environmentId);
        var userContext = userContextLoader.loadApis(
            new UserContext(auditInfo),
            kind == DataKind.PER_REQUEST ? ApiDataKind.PER_REQUEST : ApiDataKind.AGGREGATES
        );
        return userContext
            .apis()
            .orElseGet(Collections::emptyList)
            .stream()
            .map(api -> new AccessibleApi(api.getId(), api.getName(), ApiTypeAdapter.toObservability(api.getType())))
            .toList();
    }

    @Override
    public List<AccessibleApi> loadWorkspaceApis(String organizationId, String environmentId, String workspaceId) {
        var auditInfo = currentAuditInfo(organizationId, environmentId);
        return workspaceApis
            .findWorkspaceApis(new ExecutionContext(organizationId, environmentId), auditInfo.actor().userId(), workspaceId)
            .stream()
            .map(api -> new AccessibleApi(api.getId(), api.getName(), ApiTypeAdapter.toObservability(api.getType())))
            .toList();
    }

    @Override
    public boolean narrowsByApiProduct(Collection<String> metricNames) {
        return (
            !metricNames.isEmpty() &&
            metricNames.stream().allMatch(metric -> queryContextProvider.narrowsBy(toApimMetricName(metric), FilterSpec.Name.API_PRODUCT))
        );
    }

    @Override
    public void validate(MeasuresQuery query) {
        for (int group = 0; group < query.groups().size(); group++) {
            analyticsQueryValidator.validateMeasuresRequest(toApimRequest(query.groups().get(group), query.metricsOf(group)));
        }
    }

    @Override
    public void validate(FacetsQuery query) {
        analyticsQueryValidator.validateFacetsRequest(toApimRequest(query));
    }

    @Override
    public void validate(TimeSeriesQuery query) {
        analyticsQueryValidator.validateTimeSeriesRequest(toApimRequest(query, List.of()));
    }

    @Override
    public JsonNode computeMeasures(MeasuresQuery query) {
        var auditInfo = currentAuditInfo(query.organizationId(), query.environmentId());
        var responses = new ArrayList<MeasuresResponse>();
        for (int group = 0; group < query.groups().size(); group++) {
            var scope = query.groups().get(group);
            var request = toApimRequest(scope, query.metricsOf(group));
            // The group scope is already narrowed from the readable APIs, which Gamma loads with the same
            // rule as the engine's context: reloading that context per group would repeat the same search.
            responses.add(
                scope.isEmpty() ? zeroValued(request) : computeMeasuresUseCase.executeForApis(auditInfo, request, scope.apiIds()).response()
            );
        }
        var node = objectMapper.createObjectNode();
        node.set("metrics", query.identified() ? entriesById(query, responses) : entriesByName(query, responses));
        return node;
    }

    // What the engine answers when no document matches, without asking it: no readable API meets the conditions.
    private MeasuresResponse zeroValued(MeasuresRequest request) {
        var measuresByMetric = new LinkedHashMap<MetricSpec.Name, LinkedHashSet<MetricSpec.Measure>>();
        request
            .metrics()
            .forEach(metric -> measuresByMetric.computeIfAbsent(metric.name(), name -> new LinkedHashSet<>()).addAll(metric.measures()));
        return unitEnrichmentPostProcessor.enrichUnits(
            new MeasuresResponse(
                measuresByMetric
                    .entrySet()
                    .stream()
                    .map(metric ->
                        new MetricMeasuresResponse(
                            metric.getKey(),
                            null,
                            metric
                                .getValue()
                                .stream()
                                .map(measure -> new Measure(measure, 0))
                                .toList(),
                            List.of()
                        )
                    )
                    .toList()
            )
        );
    }

    private ArrayNode entriesById(MeasuresQuery query, List<MeasuresResponse> responses) {
        var entries = objectMapper.createArrayNode();
        for (var metric : query.metrics()) {
            findEntry(responses.get(metric.group()), metric.query().metricName()).ifPresent(entry -> {
                var requested = metric.query().measures();
                var own = entry
                    .measures()
                    .stream()
                    .filter(measure -> requested.contains(measure.name().name()))
                    .toList();
                var node = objectMapper.createObjectNode().put("id", metric.query().id());
                node.setAll(
                    (ObjectNode) objectMapper.valueToTree(
                        new MetricMeasuresResponse(entry.name(), entry.unit(), own, entry.ignoredFilters())
                    )
                );
                ConditionsNotAppliedEnricher.enrichMetric(node, metric.conditionsNotApplied());
                entries.add(node);
            });
        }
        return entries;
    }

    private ArrayNode entriesByName(MeasuresQuery query, List<MeasuresResponse> responses) {
        var notAppliedByName = new LinkedHashMap<String, LinkedHashSet<String>>();
        var groupByName = new HashMap<String, Integer>();
        for (var metric : query.metrics()) {
            var name = metric.query().metricName();
            notAppliedByName.computeIfAbsent(name, key -> new LinkedHashSet<>()).addAll(metric.conditionsNotApplied());
            groupByName.putIfAbsent(name, metric.group());
        }
        var entries = objectMapper.createArrayNode();
        notAppliedByName.forEach((name, notApplied) ->
            findEntry(responses.get(groupByName.get(name)), name).ifPresent(entry -> {
                ObjectNode node = objectMapper.valueToTree(entry);
                ConditionsNotAppliedEnricher.enrichMetric(node, List.copyOf(notApplied));
                entries.add(node);
            })
        );
        return entries;
    }

    private static Optional<MetricMeasuresResponse> findEntry(MeasuresResponse response, String metricName) {
        return response
            .metrics()
            .stream()
            .filter(entry -> entry.name().name().equals(metricName))
            .findFirst();
    }

    @Override
    public JsonNode computeFacets(FacetsQuery query) {
        var auditInfo = currentAuditInfo(query.organizationId(), query.environmentId());
        // The scope is already narrowed from the APIs Gamma reads for this query: the engine must not narrow it again.
        var response = computeFacetsUseCase.executeForApis(auditInfo, toApimRequest(query), query.scope().apiIds()).response();
        var node = (ObjectNode) objectMapper.valueToTree(response);
        AnalyticsBucketTypeEnricher.enrichFacetsResponse(node);
        ConditionsNotAppliedEnricher.enrichFacets(node, query.conditionsNotApplied());
        return node;
    }

    @Override
    public JsonNode computeTimeSeries(TimeSeriesQuery query) {
        var auditInfo = currentAuditInfo(query.organizationId(), query.environmentId());
        var rankingConditions = rankingConditions(query);
        var response = computeTimeSeriesUseCase
            .executeForApis(auditInfo, toApimRequest(query, rankingConditions), query.scope().apiIds())
            .response();
        if (query.ranking() != null) {
            response = RankedSeriesFilter.keepRankedSeries(response, query.ranking(), rankingConditions, query.scope());
        }
        var node = (ObjectNode) objectMapper.valueToTree(response);
        AnalyticsBucketTypeEnricher.enrichTimeSeriesResponse(node);
        ConditionsNotAppliedEnricher.enrichTimeSeries(node, query.conditionsNotApplied());
        return node;
    }

    @Override
    public JsonNode emptyFacetsResponse() {
        return objectMapper.valueToTree(new FacetsResponse(List.of()));
    }

    @Override
    public JsonNode emptyTimeSeriesResponse() {
        return objectMapper.valueToTree(new TimeSeriesResponse(List.of()));
    }

    // ---- Translation: Gamma → APIM ----

    private static MeasuresRequest toApimRequest(AnalyticsRequestPipeline.PreparedScope scope, List<AnalyticsMetricQuery> metrics) {
        return new MeasuresRequest(toTimeRange(scope), translateFilters(scope), toApimMetrics(metrics));
    }

    private static FacetsRequest toApimRequest(FacetsQuery query) {
        return new FacetsRequest(
            toTimeRange(query.scope()),
            translateFilters(query.scope()),
            toApimFacetMetrics(query.metrics()),
            toApimFacetNames(query.facets()),
            query.limit(),
            toApimRanges(query.ranges())
        );
    }

    private static TimeSeriesRequest toApimRequest(TimeSeriesQuery query, List<Filter> rankingConditions) {
        var filters = new ArrayList<>(translateFilters(query.scope()));
        filters.addAll(rankingConditions);
        return new TimeSeriesRequest(
            toTimeRange(query.scope()),
            query.interval(),
            List.copyOf(filters),
            toApimFacetMetrics(query.metrics()),
            toApimFacetNames(query.facets()),
            query.ranking() == null ? null : query.ranking().keysAt(query.ranking().depth() - 1).size(),
            toApimRanges(query.ranges())
        );
    }

    /**
     * Restricts each facet to its ranked values, so a ranked series is in every time bucket it has data in,
     * not only where it is among the engine's top values of that bucket. A path that ignores one of these
     * filters can still miss a ranked series in some buckets. The bucket key of a status group or of a range
     * is the Elasticsearch range key, not a filter value; those facets are range aggregations returning all
     * their buckets anyway. Levels the ranking did not reach are left alone.
     */
    private static List<Filter> rankingConditions(TimeSeriesQuery query) {
        if (query.ranking() == null) {
            return List.of();
        }
        var facets = query.facets();
        var conditions = new ArrayList<Filter>();
        for (int level = 0; level < query.ranking().depth(); level++) {
            var facet = facets.get(level);
            var isRangedLeaf = level == facets.size() - 1 && query.ranges() != null && !query.ranges().isEmpty();
            if (HTTP_STATUS_CODE_GROUP.equals(facet) || isRangedLeaf) {
                continue;
            }
            conditions.add(
                new Filter(
                    FilterSpec.Name.valueOf(facet),
                    io.gravitee.apim.core.observability.model.FilterOperator.IN,
                    List.copyOf(query.ranking().keysAt(level))
                )
            );
        }
        return conditions;
    }

    private static TimeRange toTimeRange(AnalyticsRequestPipeline.PreparedScope scope) {
        return new TimeRange(scope.from(), scope.to());
    }

    private static List<Filter> translateFilters(AnalyticsRequestPipeline.PreparedScope scope) {
        var filters = scope.filters().stream().map(ObservabilityAnalyticsDataPortAdapter::translateFilter).collect(Collectors.toList());
        if (scope.trafficScope() != null) {
            filters.add(
                new Filter(
                    FilterSpec.Name.TRAFFIC_SCOPE,
                    io.gravitee.apim.core.observability.model.FilterOperator.IN,
                    new TrafficScope(scope.trafficScope().apiIds(), scope.trafficScope().entrypointIds())
                )
            );
        }
        return List.copyOf(filters);
    }

    private static Filter translateFilter(FilterCondition condition) {
        var name = FilterSpec.Name.valueOf(condition.name());
        var operator = io.gravitee.apim.core.observability.model.FilterOperator.valueOf(condition.operator().name());
        Object value = (condition.operator() == FilterOperator.IN || condition.operator() == FilterOperator.NOT_IN)
            ? condition.values()
            : (condition.values() != null && !condition.values().isEmpty() ? condition.values().getFirst() : null);
        return new Filter(name, operator, value);
    }

    // A metric's own conditions are already in its query's scope, one clause each: the engine's per-metric
    // filters are applied on HTTP measures only and collide when one metric appears twice.
    private static List<MetricMeasuresRequest> toApimMetrics(List<AnalyticsMetricQuery> metrics) {
        if (metrics == null) {
            return List.of();
        }
        return metrics
            .stream()
            .map(m ->
                new MetricMeasuresRequest(
                    toApimMetricName(m.metricName()),
                    m
                        .measures()
                        .stream()
                        .map(measure -> toApimMeasure(m.metricName(), measure))
                        .toList()
                )
            )
            .toList();
    }

    private static List<FacetMetricMeasuresRequest> toApimFacetMetrics(List<AnalyticsFacetMetricQuery> metrics) {
        if (metrics == null) {
            return List.of();
        }
        return metrics
            .stream()
            .map(m ->
                new FacetMetricMeasuresRequest(
                    toApimMetricName(m.metricName()),
                    m
                        .measures()
                        .stream()
                        .map(measure -> toApimMeasure(m.metricName(), measure))
                        .toList(),
                    toApimSorts(m.metricName(), m.sorts())
                )
            )
            .toList();
    }

    private static List<FacetMetricMeasuresRequest.Sort> toApimSorts(String metricName, List<AnalyticsSortSpec> sorts) {
        if (sorts == null) {
            return List.of();
        }
        return sorts
            .stream()
            .map(s -> new FacetMetricMeasuresRequest.Sort(toApimMeasure(metricName, s.measure()), toApimSortOrder(s.order())))
            .toList();
    }

    private static List<FacetSpec.Name> toApimFacetNames(List<String> facets) {
        if (facets == null) {
            return List.of();
        }
        return facets.stream().map(ObservabilityAnalyticsDataPortAdapter::toApimFacetName).toList();
    }

    private static MetricSpec.Name toApimMetricName(String metric) {
        return toEnum(MetricSpec.Name.class, metric).orElseThrow(() -> InvalidObservabilityQueryException.unknownMetric(metric));
    }

    private static MetricSpec.Measure toApimMeasure(String metric, String measure) {
        return toEnum(MetricSpec.Measure.class, measure).orElseThrow(() ->
            InvalidObservabilityQueryException.unknownMeasure(metric, measure)
        );
    }

    private static FacetSpec.Name toApimFacetName(String facet) {
        return toEnum(FacetSpec.Name.class, facet).orElseThrow(() -> InvalidObservabilityQueryException.unknownFacet(facet));
    }

    // Case-insensitive like the filter operators: "desc" is unambiguous, only an unknown order is refused.
    private static FacetMetricMeasuresRequest.Sort.Order toApimSortOrder(String order) {
        return toEnum(FacetMetricMeasuresRequest.Sort.Order.class, order == null ? null : order.toUpperCase(Locale.ROOT)).orElseThrow(() ->
            InvalidObservabilityQueryException.unknownSortOrder(order)
        );
    }

    private static <E extends Enum<E>> Optional<E> toEnum(Class<E> type, String name) {
        return Arrays.stream(type.getEnumConstants())
            .filter(constant -> constant.name().equals(name))
            .findFirst();
    }

    private static List<NumberRange> toApimRanges(List<AnalyticsNumberRange> ranges) {
        if (ranges == null) {
            return List.of();
        }
        return ranges
            .stream()
            .map(r -> new NumberRange(r.from(), r.to()))
            .toList();
    }

    // ---- Audit / Security ----

    private static AuditInfo currentAuditInfo(String organizationId, String environmentId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        AuditActor actor;
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails user) {
            actor = AuditActor.builder().userId(user.getUsername()).userSource(user.getSource()).userSourceId(user.getSourceId()).build();
        } else {
            String userId = (authentication != null && authentication.getPrincipal() != null)
                ? authentication.getPrincipal().toString()
                : "unknown";
            actor = AuditActor.builder().userId(userId).build();
        }
        return AuditInfo.builder().organizationId(organizationId).environmentId(environmentId).actor(actor).build();
    }
}
