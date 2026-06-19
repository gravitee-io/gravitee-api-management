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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
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
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.AnalyticsRequestPipeline;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Adapter bridging the Gamma analytics port to the APIM analytics compute use cases. Translates
 * Gamma-native types to the analytics-engine model, builds the {@link AuditInfo} from the ambient
 * request context, and serializes APIM responses to {@link JsonNode} for framework independence.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class ObservabilityAnalyticsDataPortAdapter implements ObservabilityAnalyticsDataPort {

    private final ComputeMeasuresUseCase computeMeasuresUseCase;
    private final ComputeFacetsUseCase computeFacetsUseCase;
    private final ComputeTimeSeriesUseCase computeTimeSeriesUseCase;
    private final UserContextLoader userContextLoader;
    private final ObjectMapper objectMapper;

    @Override
    public List<AccessibleApi> loadAccessibleApis(String organizationId, String environmentId) {
        var auditInfo = currentAuditInfo(organizationId, environmentId);
        var userContext = userContextLoader.loadApis(new UserContext(auditInfo));
        return userContext
            .apis()
            .orElseGet(Collections::emptyList)
            .stream()
            .map(api -> new AccessibleApi(api.getId(), api.getName(), toGammaApiType(api.getType())))
            .toList();
    }

    @Override
    public JsonNode computeMeasures(MeasuresQuery query) {
        var auditInfo = currentAuditInfo(query.organizationId(), query.environmentId());
        var apimRequest = new MeasuresRequest(toTimeRange(query.scope()), translateFilters(query.scope()), toApimMetrics(query.metrics()));
        var response = computeMeasuresUseCase.execute(new ComputeMeasuresUseCase.Input(auditInfo, apimRequest)).response();
        return objectMapper.valueToTree(response);
    }

    @Override
    public JsonNode computeFacets(FacetsQuery query) {
        var auditInfo = currentAuditInfo(query.organizationId(), query.environmentId());
        var apimRequest = new FacetsRequest(
            toTimeRange(query.scope()),
            translateFilters(query.scope()),
            toApimFacetMetrics(query.metrics()),
            toApimFacetNames(query.facets()),
            query.limit(),
            toApimRanges(query.ranges())
        );
        var response = computeFacetsUseCase.execute(new ComputeFacetsUseCase.Input(auditInfo, apimRequest)).response();
        var node = (ObjectNode) objectMapper.valueToTree(response);
        AnalyticsBucketTypeEnricher.enrichFacetsResponse(node);
        return node;
    }

    @Override
    public JsonNode computeTimeSeries(TimeSeriesQuery query) {
        var auditInfo = currentAuditInfo(query.organizationId(), query.environmentId());
        var apimRequest = new TimeSeriesRequest(
            toTimeRange(query.scope()),
            query.interval(),
            translateFilters(query.scope()),
            toApimFacetMetrics(query.metrics()),
            toApimFacetNames(query.facets()),
            query.facetSize(),
            toApimRanges(query.ranges())
        );
        var response = computeTimeSeriesUseCase.execute(new ComputeTimeSeriesUseCase.Input(auditInfo, apimRequest)).response();
        var node = (ObjectNode) objectMapper.valueToTree(response);
        AnalyticsBucketTypeEnricher.enrichTimeSeriesResponse(node);
        return node;
    }

    @Override
    public JsonNode emptyMeasuresResponse() {
        return objectMapper.valueToTree(new MeasuresResponse(List.of()));
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

    private static TimeRange toTimeRange(AnalyticsRequestPipeline.PreparedScope scope) {
        return new TimeRange(scope.from(), scope.to());
    }

    private static List<Filter> translateFilters(AnalyticsRequestPipeline.PreparedScope scope) {
        return scope.filters().stream().map(ObservabilityAnalyticsDataPortAdapter::translateFilter).toList();
    }

    private static Filter translateFilter(FilterCondition condition) {
        var name = FilterSpec.Name.valueOf(condition.name());
        var operator = io.gravitee.apim.core.observability.model.FilterOperator.valueOf(condition.operator().name());
        Object value = (condition.operator() == FilterOperator.IN || condition.operator() == FilterOperator.NOT_IN)
            ? condition.values()
            : (condition.values() != null && !condition.values().isEmpty() ? condition.values().getFirst() : null);
        return new Filter(name, operator, value);
    }

    private static List<MetricMeasuresRequest> toApimMetrics(List<AnalyticsMetricQuery> metrics) {
        if (metrics == null) {
            return List.of();
        }
        return metrics
            .stream()
            .map(m ->
                new MetricMeasuresRequest(
                    MetricSpec.Name.valueOf(m.metricName()),
                    m.measures().stream().map(MetricSpec.Measure::valueOf).toList()
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
                    MetricSpec.Name.valueOf(m.metricName()),
                    m.measures().stream().map(MetricSpec.Measure::valueOf).toList(),
                    toApimSorts(m.sorts())
                )
            )
            .toList();
    }

    private static List<FacetMetricMeasuresRequest.Sort> toApimSorts(List<AnalyticsSortSpec> sorts) {
        if (sorts == null) {
            return List.of();
        }
        return sorts
            .stream()
            .map(s ->
                new FacetMetricMeasuresRequest.Sort(
                    MetricSpec.Measure.valueOf(s.measure()),
                    FacetMetricMeasuresRequest.Sort.Order.valueOf(s.order())
                )
            )
            .toList();
    }

    private static List<FacetSpec.Name> toApimFacetNames(List<String> facets) {
        if (facets == null) {
            return List.of();
        }
        return facets.stream().map(FacetSpec.Name::valueOf).toList();
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

    private static ApiType toGammaApiType(io.gravitee.definition.model.v4.ApiType definitionType) {
        if (definitionType == null) {
            return null;
        }
        return switch (definitionType) {
            case PROXY -> ApiType.HTTP_PROXY;
            case MESSAGE -> ApiType.MESSAGE;
            case LLM_PROXY -> ApiType.LLM;
            case MCP_PROXY -> ApiType.MCP;
            case NATIVE -> ApiType.NATIVE;
            case EDGE -> ApiType.EDGE;
            case A2A_PROXY, AUTHZ -> null;
        };
    }
}
