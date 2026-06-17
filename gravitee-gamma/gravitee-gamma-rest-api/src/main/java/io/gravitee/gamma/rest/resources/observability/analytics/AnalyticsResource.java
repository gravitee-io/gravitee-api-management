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
package io.gravitee.gamma.rest.resources.observability.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityFacetsUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityMeasuresUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityTimeSeriesUseCase;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.resources.observability.analytics.dto.AnalyticsFacetsRequestDto;
import io.gravitee.gamma.rest.resources.observability.analytics.dto.AnalyticsMeasuresRequestDto;
import io.gravitee.gamma.rest.resources.observability.analytics.dto.AnalyticsTimeSeriesRequestDto;
import io.gravitee.gamma.rest.resources.observability.analytics.dto.FacetMetricQueryDto;
import io.gravitee.gamma.rest.resources.observability.analytics.dto.MetricQueryDto;
import io.gravitee.gamma.rest.resources.observability.analytics.dto.NumberRangeDto;
import io.gravitee.gamma.rest.resources.observability.logs.dto.FilterConditionDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import java.util.List;

/**
 * Analytics computation endpoints under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/observability/analytics}.
 *
 * <p>Consumes the unified Gamma filter vocabulary ({@code FilterCondition[]}) validated against the
 * {@code ANALYTICS} signal and delegates to the proven APIM analytics engine. Server-side RBAC
 * scoping ensures the caller only sees metrics for APIs they can read.
 *
 * @author GraviteeSource Team
 */
public class AnalyticsResource {

    @Inject
    private ComputeObservabilityMeasuresUseCase computeMeasuresUseCase;

    @Inject
    private ComputeObservabilityFacetsUseCase computeFacetsUseCase;

    @Inject
    private ComputeObservabilityTimeSeriesUseCase computeTimeSeriesUseCase;

    @Inject
    private PermissionService permissionService;

    @POST
    @Path("/measures")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonNode computeMeasures(AnalyticsMeasuresRequestDto request) {
        checkReadObservabilityPermission();

        var ctx = GraviteeContext.getExecutionContext();
        var output = computeMeasuresUseCase.execute(
            new ComputeObservabilityMeasuresUseCase.Input(
                ctx.getOrganizationId(),
                ctx.getEnvironmentId(),
                toFilters(request != null ? request.filters() : null),
                request != null && request.timeRange() != null ? request.timeRange().from() : null,
                request != null && request.timeRange() != null ? request.timeRange().to() : null,
                toMetrics(request != null ? request.metrics() : null)
            )
        );
        return output.response();
    }

    @POST
    @Path("/facets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonNode computeFacets(AnalyticsFacetsRequestDto request) {
        checkReadObservabilityPermission();

        var ctx = GraviteeContext.getExecutionContext();
        var output = computeFacetsUseCase.execute(
            new ComputeObservabilityFacetsUseCase.Input(
                ctx.getOrganizationId(),
                ctx.getEnvironmentId(),
                toFilters(request != null ? request.filters() : null),
                request != null && request.timeRange() != null ? request.timeRange().from() : null,
                request != null && request.timeRange() != null ? request.timeRange().to() : null,
                toFacetMetrics(request != null ? request.metrics() : null),
                request != null && request.by() != null ? request.by() : List.of(),
                request != null ? request.limit() : null,
                toRanges(request != null ? request.ranges() : null)
            )
        );
        return output.response();
    }

    @POST
    @Path("/time-series")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public JsonNode computeTimeSeries(AnalyticsTimeSeriesRequestDto request) {
        checkReadObservabilityPermission();

        var ctx = GraviteeContext.getExecutionContext();
        var output = computeTimeSeriesUseCase.execute(
            new ComputeObservabilityTimeSeriesUseCase.Input(
                ctx.getOrganizationId(),
                ctx.getEnvironmentId(),
                toFilters(request != null ? request.filters() : null),
                request != null && request.timeRange() != null ? request.timeRange().from() : null,
                request != null && request.timeRange() != null ? request.timeRange().to() : null,
                request != null ? request.interval() : null,
                toFacetMetrics(request != null ? request.metrics() : null),
                request != null && request.by() != null ? request.by() : List.of(),
                request != null ? request.facetSize() : null,
                toRanges(request != null ? request.ranges() : null)
            )
        );
        return output.response();
    }

    private static List<FilterCondition> toFilters(List<FilterConditionDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream().map(FilterConditionDto::toCore).toList();
    }

    private static List<io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsMetricQuery> toMetrics(
        List<MetricQueryDto> dtos
    ) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream().map(MetricQueryDto::toCore).toList();
    }

    private static List<io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsFacetMetricQuery> toFacetMetrics(
        List<FacetMetricQueryDto> dtos
    ) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream().map(FacetMetricQueryDto::toCore).toList();
    }

    private static List<io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsNumberRange> toRanges(
        List<NumberRangeDto> dtos
    ) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream().map(NumberRangeDto::toCore).toList();
    }

    private void checkReadObservabilityPermission() {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        String environmentId = executionContext.getEnvironmentId();
        boolean allowed =
            permissionService.hasPermission(
                executionContext,
                RolePermission.ENVIRONMENT_DASHBOARD,
                environmentId,
                RolePermissionAction.READ
            ) ||
            permissionService.hasPermission(executionContext, RolePermission.ENVIRONMENT_API, environmentId, RolePermissionAction.READ);
        if (!allowed) {
            throw new ForbiddenAccessException();
        }
    }
}
