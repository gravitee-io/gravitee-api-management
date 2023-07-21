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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.rest.api.model.permissions.RolePermission.*;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.param.Aggregation;
import io.gravitee.rest.api.management.rest.resource.param.AnalyticsParam;
import io.gravitee.rest.api.management.rest.resource.param.Range;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.analytics.Analytics;
import io.gravitee.rest.api.model.analytics.query.*;
import io.gravitee.rest.api.service.AnalyticsService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Platform Analytics")
public class PlatformAnalyticsResource extends AbstractResource {

    @Inject
    ApiService apiService;

    @Inject
    PermissionService permissionService;

    @Inject
    ApplicationService applicationService;

    @Inject
    private AnalyticsService analyticsService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get platform analytics",
        description = "User must have the MANAGEMENT_PLATFORM[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Platform analytics",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Analytics.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = ENVIRONMENT_PLATFORM, acls = READ) })
    public Response getPlatformAnalytics(@BeanParam AnalyticsParam analyticsParam) {
        analyticsParam.validate();

        // add filter by Apis or Applications
        String fieldName;
        Set<String> ids;

        if ("application".equals(analyticsParam.getField())) {
            fieldName = "application";
            ids = findApplicationIds();
        } else {
            fieldName = "api";
            ids = findApiIds();
        }

        if (ids.isEmpty()) {
            return Response.noContent().build();
        }
        String extraFilter = getExtraFilter(fieldName, ids);

        if (analyticsParam.getQuery() != null) {
            analyticsParam.setQuery(analyticsParam.getQuery().replaceAll("\\?", "1"));
        }

        Analytics analytics = null;
        switch (analyticsParam.getType()) {
            case DATE_HISTO:
                analytics = executeDateHisto(analyticsParam, extraFilter);
                break;
            case GROUP_BY:
                analytics = executeGroupBy(analyticsParam, extraFilter);
                break;
            case COUNT:
                analytics = executeCount(analyticsParam, extraFilter);
                break;
            case STATS:
                analytics = executeStats(analyticsParam, extraFilter);
                break;
        }

        return Response.ok(analytics).build();
    }

    @NotNull
    private Set<String> findApiIds() {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (isAdmin()) {
            return apiAuthorizationService.findIdsByEnvironment(executionContext);
        }
        return apiAuthorizationService
            .findIdsByUser(executionContext, getAuthenticatedUser(), true)
            .stream()
            .filter(appId -> permissionService.hasPermission(executionContext, API_ANALYTICS, appId, READ))
            .collect(Collectors.toSet());
    }

    @NotNull
    private Set<String> findApplicationIds() {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (isAdmin()) {
            return applicationService.findIdsByUser(executionContext, null);
        }
        return applicationService
            .findIdsByUser(executionContext, getAuthenticatedUser())
            .stream()
            .filter(appId -> permissionService.hasPermission(executionContext, APPLICATION_ANALYTICS, appId, READ))
            .collect(Collectors.toSet());
    }

    private Analytics executeStats(AnalyticsParam analyticsParam, String extraFilter) {
        final StatsQuery query = new StatsQuery();
        query.setFrom(analyticsParam.getFrom());
        query.setTo(analyticsParam.getTo());
        query.setInterval(analyticsParam.getInterval());
        query.setQuery(analyticsParam.getQuery());
        query.setField(analyticsParam.getField());
        addExtraFilter(query, extraFilter);
        return analyticsService.execute(query);
    }

    private Analytics executeCount(AnalyticsParam analyticsParam, String extraFilter) {
        CountQuery query = new CountQuery();
        query.setFrom(analyticsParam.getFrom());
        query.setTo(analyticsParam.getTo());
        query.setInterval(analyticsParam.getInterval());
        query.setQuery(analyticsParam.getQuery());
        addExtraFilter(query, extraFilter);
        return analyticsService.execute(query);
    }

    private Analytics executeDateHisto(AnalyticsParam analyticsParam, String extraFilter) {
        DateHistogramQuery query = new DateHistogramQuery();
        query.setFrom(analyticsParam.getFrom());
        query.setTo(analyticsParam.getTo());
        query.setInterval(analyticsParam.getInterval());
        query.setQuery(analyticsParam.getQuery());
        List<Aggregation> aggregations = analyticsParam.getAggregations();
        if (aggregations != null) {
            List<io.gravitee.rest.api.model.analytics.query.Aggregation> aggregationList = aggregations
                .stream()
                .map(
                    (Function<Aggregation, io.gravitee.rest.api.model.analytics.query.Aggregation>) aggregation ->
                        new io.gravitee.rest.api.model.analytics.query.Aggregation() {
                            @Override
                            public AggregationType type() {
                                return AggregationType.valueOf(aggregation.getType().name().toUpperCase());
                            }

                            @Override
                            public String field() {
                                return aggregation.getField();
                            }
                        }
                )
                .collect(Collectors.toList());

            query.setAggregations(aggregationList);
        }
        addExtraFilter(query, extraFilter);
        return analyticsService.execute(GraviteeContext.getExecutionContext(), query);
    }

    private Analytics executeGroupBy(AnalyticsParam analyticsParam, String extraFilter) {
        GroupByQuery query = new GroupByQuery();
        query.setFrom(analyticsParam.getFrom());
        query.setTo(analyticsParam.getTo());
        query.setInterval(analyticsParam.getInterval());
        query.setQuery(analyticsParam.getQuery());
        query.setField(analyticsParam.getField());

        if (analyticsParam.getOrder() != null) {
            final GroupByQuery.Order order = new GroupByQuery.Order();
            order.setField(analyticsParam.getOrder().getField());
            order.setType(analyticsParam.getOrder().getType());
            order.setOrder(analyticsParam.getOrder().isOrder());
            query.setOrder(order);
        }

        List<Range> ranges = analyticsParam.getRanges();
        if (ranges != null) {
            Map<Double, Double> rangeMap = ranges.stream().collect(Collectors.toMap(Range::getFrom, Range::getTo));

            query.setGroups(rangeMap);
        }
        addExtraFilter(query, extraFilter);
        return analyticsService.execute(GraviteeContext.getExecutionContext(), query);
    }

    private void addExtraFilter(AbstractQuery query, String extraFilter) {
        if (query.getQuery() == null || query.getQuery().isEmpty()) {
            query.setQuery(extraFilter);
        } else if (extraFilter != null && !extraFilter.isEmpty()) {
            query.setQuery(query.getQuery() + " AND " + extraFilter);
        } else {
            query.setQuery(query.getQuery());
        }
    }

    private String getExtraFilter(String fieldName, Set<String> ids) {
        if (ids != null && !ids.isEmpty()) {
            return fieldName + ":(" + ids.stream().collect(Collectors.joining(" OR ")) + ")";
        }
        return null;
    }
}
