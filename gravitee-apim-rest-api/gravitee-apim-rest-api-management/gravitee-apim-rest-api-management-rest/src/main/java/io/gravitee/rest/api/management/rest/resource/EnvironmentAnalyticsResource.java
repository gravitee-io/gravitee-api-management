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

import static io.gravitee.rest.api.model.permissions.RolePermission.API_ANALYTICS;
import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_ANALYTICS;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.management.rest.resource.param.Aggregation;
import io.gravitee.rest.api.management.rest.resource.param.AnalyticsParam;
import io.gravitee.rest.api.management.rest.resource.param.Range;
import io.gravitee.rest.api.model.analytics.Analytics;
import io.gravitee.rest.api.model.analytics.HistogramAnalytics;
import io.gravitee.rest.api.model.analytics.HitsAnalytics;
import io.gravitee.rest.api.model.analytics.TopHitsAnalytics;
import io.gravitee.rest.api.model.analytics.query.AggregationType;
import io.gravitee.rest.api.model.analytics.query.CountQuery;
import io.gravitee.rest.api.model.analytics.query.DateHistogramQuery;
import io.gravitee.rest.api.model.analytics.query.GroupByQuery;
import io.gravitee.rest.api.model.analytics.query.StatsAnalytics;
import io.gravitee.rest.api.model.analytics.query.StatsQuery;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.application.ApplicationExcludeFilter;
import io.gravitee.rest.api.model.application.ApplicationQuery;
import io.gravitee.rest.api.model.common.PageableImpl;
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
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Environment Analytics")
public class EnvironmentAnalyticsResource extends AbstractResource {

    public static final String API_FIELD = "api";
    public static final String UNKNOWN_ID = "1";
    public static final String APPLICATION_FIELD = "application";
    public static final String STATE_FIELD = "state";
    public static final String LIFECYCLE_STATE_FIELD = "lifecycle_state";

    @Inject
    ApiService apiService;

    @Inject
    io.gravitee.rest.api.service.v4.ApiService apiServiceV4;

    @Inject
    PermissionService permissionService;

    @Inject
    ApplicationService applicationService;

    @Inject
    private AnalyticsService analyticsService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get environment analytics")
    @ApiResponse(
        responseCode = "200",
        description = "Environment analytics",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Analytics.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getPlatformAnalytics(@BeanParam AnalyticsParam analyticsParam) {
        analyticsParam.validate();

        Analytics analytics = null;

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        if (analyticsParam.getQuery() != null) {
            analyticsParam.setQuery(analyticsParam.getQuery().replaceAll("\\?", "1"));
        }

        switch (analyticsParam.getType()) {
            case DATE_HISTO:
                analytics = executeDateHisto(executionContext, analyticsParam);
                break;
            case GROUP_BY:
                analytics = executeGroupBy(executionContext, analyticsParam);
                break;
            case COUNT:
                analytics = executeCount(executionContext, analyticsParam);
                break;
            case STATS:
                analytics = executeStats(executionContext, analyticsParam);
                break;
        }

        return Response.ok(analytics).build();
    }

    private Analytics executeStats(ExecutionContext executionContext, AnalyticsParam analyticsParam) {
        Map<String, Set<String>> terms;
        try {
            terms = buildTerms(executionContext, analyticsParam);
        } catch (FieldFilterEmptyException e) {
            return new StatsAnalytics();
        }

        final StatsQuery query = new StatsQuery();
        query.setFrom(analyticsParam.getFrom());
        query.setTo(analyticsParam.getTo());
        query.setInterval(analyticsParam.getInterval());
        query.setQuery(analyticsParam.getQuery());
        query.setField(analyticsParam.getField());
        query.setTerms(terms);
        return analyticsService.execute(executionContext, query);
    }

    private Analytics executeCount(final ExecutionContext executionContext, AnalyticsParam analyticsParam) {
        switch (analyticsParam.getField()) {
            case API_FIELD:
                return buildCountStat(
                    // FIXME: This should use a count method instead of a search one
                    apiServiceV4.findAll(executionContext, getAuthenticatedUser(), isAdmin(), new PageableImpl(1, 1)).getTotalElements()
                );
            case APPLICATION_FIELD:
                if (isAdmin()) {
                    ApplicationQuery applicationQuery = new ApplicationQuery();
                    applicationQuery.setStatus(ApplicationStatus.ACTIVE.name());
                    return buildCountStat(applicationService.searchIds(executionContext, applicationQuery, null).size());
                } else {
                    return buildCountStat(applicationService.findIdsByUser(executionContext, getAuthenticatedUser()).size());
                }
            default:
                Map<String, Set<String>> terms;
                try {
                    terms = buildTerms(executionContext, analyticsParam);
                } catch (FieldFilterEmptyException e) {
                    return new HitsAnalytics();
                }

                CountQuery query = new CountQuery();
                query.setFrom(analyticsParam.getFrom());
                query.setTo(analyticsParam.getTo());
                query.setInterval(analyticsParam.getInterval());
                query.setQuery(analyticsParam.getQuery());
                query.setTerms(terms);
                return analyticsService.execute(executionContext, query);
        }
    }

    private StatsAnalytics buildCountStat(float countValue) {
        StatsAnalytics stats = new StatsAnalytics();
        stats.setCount(countValue);
        return stats;
    }

    private Analytics executeDateHisto(final ExecutionContext executionContext, AnalyticsParam analyticsParam) {
        Map<String, Set<String>> terms;
        try {
            terms = buildTerms(executionContext, analyticsParam);
        } catch (FieldFilterEmptyException e) {
            return new HistogramAnalytics();
        }

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
        query.setTerms(terms);
        return analyticsService.execute(executionContext, query);
    }

    /**
     * @param executionContext
     * @param analyticsParam
     * @return
     * @throws FieldFilterEmptyException: if user is not Admin and filter based on ids field is empty
     */
    private Map<String, Set<String>> buildTerms(ExecutionContext executionContext, AnalyticsParam analyticsParam)
        throws FieldFilterEmptyException {
        String fieldName;
        Set<String> ids;
        // add filter by Apis or Applications
        if (isAdmin()) {
            if (APPLICATION_FIELD.equalsIgnoreCase(analyticsParam.getField())) {
                fieldName = APPLICATION_FIELD;
                ApplicationQuery applicationQuery = new ApplicationQuery();
                applicationQuery.setStatus(ApplicationStatus.ACTIVE.name());
                applicationQuery.setExcludeFilters(List.of(ApplicationExcludeFilter.OWNER));
                ids = applicationService.searchIds(executionContext, applicationQuery, null);
                ids.add(UNKNOWN_ID);
            } else {
                fieldName = API_FIELD;
                ids = apiAuthorizationService.findIdsByEnvironment(executionContext.getEnvironmentId());
                ids.add(UNKNOWN_ID);
            }
        } else {
            if (APPLICATION_FIELD.equalsIgnoreCase(analyticsParam.getField())) {
                fieldName = APPLICATION_FIELD;
                ids = applicationService
                    .findIdsByUser(executionContext, getAuthenticatedUser())
                    .stream()
                    .filter(appId -> permissionService.hasPermission(executionContext, APPLICATION_ANALYTICS, appId, READ))
                    .collect(Collectors.toSet());
            } else {
                fieldName = API_FIELD;
                ids = apiAuthorizationService
                    .findIdsByUser(executionContext, getAuthenticatedUser(), true)
                    .stream()
                    .filter(apiId -> permissionService.hasPermission(executionContext, API_ANALYTICS, apiId, READ))
                    .collect(Collectors.toSet());
            }
        }
        if (ids.isEmpty()) {
            throw new FieldFilterEmptyException();
        } else {
            return Map.of(fieldName, ids);
        }
    }

    private Analytics executeGroupBy(final ExecutionContext executionContext, AnalyticsParam analyticsParam) {
        switch (analyticsParam.getField()) {
            case STATE_FIELD: {
                return getTopHitsAnalytics(executionContext, api -> api.getState().name());
            }
            case LIFECYCLE_STATE_FIELD: {
                return getTopHitsAnalytics(executionContext, api -> api.getLifecycleState().name());
            }
            default:
                Map<String, Set<String>> terms;
                try {
                    terms = buildTerms(executionContext, analyticsParam);
                } catch (FieldFilterEmptyException e) {
                    return new TopHitsAnalytics();
                }

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
                query.setTerms(terms);
                return analyticsService.execute(executionContext, query);
        }
    }

    @NotNull
    private TopHitsAnalytics getTopHitsAnalytics(final ExecutionContext executionContext, Function<ApiEntity, String> groupingByFunction) {
        Set<ApiEntity> apis = isAdmin()
            ? new HashSet<>(apiService.search(executionContext, new ApiQuery()))
            : apiService.findByUser(executionContext, getAuthenticatedUser(), new ApiQuery(), true);
        Map<String, Long> collect = apis.stream().collect(Collectors.groupingBy(groupingByFunction, Collectors.counting()));
        TopHitsAnalytics topHitsAnalytics = new TopHitsAnalytics();
        topHitsAnalytics.setValues(collect);
        return topHitsAnalytics;
    }

    private class FieldFilterEmptyException extends Throwable {}
}
