/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import io.gravitee.rest.api.management.rest.resource.param.Aggregation;
import io.gravitee.rest.api.management.rest.resource.param.AnalyticsParam;
import io.gravitee.rest.api.management.rest.resource.param.Range;
import io.gravitee.rest.api.model.analytics.Analytics;
import io.gravitee.rest.api.model.analytics.HistogramAnalytics;
import io.gravitee.rest.api.model.analytics.TopHitsAnalytics;
import io.gravitee.rest.api.model.analytics.query.AbstractQuery;
import io.gravitee.rest.api.model.analytics.query.AggregationType;
import io.gravitee.rest.api.model.analytics.query.CountQuery;
import io.gravitee.rest.api.model.analytics.query.DateHistogramQuery;
import io.gravitee.rest.api.model.analytics.query.GroupByQuery;
import io.gravitee.rest.api.model.analytics.query.StatsAnalytics;
import io.gravitee.rest.api.model.analytics.query.StatsQuery;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.service.AnalyticsService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.PermissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.HashSet;
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
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Environment Analytics" })
public class EnvironmentAnalyticsResource extends AbstractResource {

    public static final String API_FIELD = "api";
    public static final String APPLICATION_FIELD = "application";
    public static final String STATE_FIELD = "state";
    public static final String LIFECYCLE_STATE_FIELD = "lifecycle_state";

    @Inject
    private AnalyticsService analyticsService;

    @Inject
    ApiService apiService;

    @Inject
    PermissionService permissionService;

    @Inject
    ApplicationService applicationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get environment analytics")
    @ApiResponses(
        { @ApiResponse(code = 200, message = "Environment analytics"), @ApiResponse(code = 500, message = "Internal server error") }
    )
    public Response getPlatformAnalytics(@BeanParam AnalyticsParam analyticsParam) {
        analyticsParam.validate();

        Analytics analytics = null;

        // add filter by Apis or Applications
        String extraFilter = null;
        if (!isAdmin()) {
            String fieldName;
            List<String> ids;
            if (APPLICATION_FIELD.equals(analyticsParam.getField())) {
                fieldName = APPLICATION_FIELD;
                ids =
                    applicationService
                        .findByUser(getAuthenticatedUser())
                        .stream()
                        .map(ApplicationListItem::getId)
                        .filter(appId -> permissionService.hasPermission(APPLICATION_ANALYTICS, appId, READ))
                        .collect(Collectors.toList());
            } else {
                fieldName = API_FIELD;
                ids =
                    apiService
                        .findByUser(getAuthenticatedUser(), null, false)
                        .stream()
                        .map(ApiEntity::getId)
                        .filter(apiId -> permissionService.hasPermission(API_ANALYTICS, apiId, READ))
                        .collect(Collectors.toList());
            }

            extraFilter = getExtraFilter(fieldName, ids);
        }

        if (analyticsParam.getQuery() != null) {
            analyticsParam.setQuery(analyticsParam.getQuery().replaceAll("\\?", "1"));
        }

        switch (analyticsParam.getTypeParam().getValue()) {
            case DATE_HISTO:
                analytics = !isAdmin() && extraFilter == null ? new HistogramAnalytics() : executeDateHisto(analyticsParam, extraFilter);
                break;
            case GROUP_BY:
                analytics = !isAdmin() && extraFilter == null ? new TopHitsAnalytics() : executeGroupBy(analyticsParam, extraFilter);
                break;
            case COUNT:
                analytics = !isAdmin() && extraFilter == null ? new StatsAnalytics() : executeCount(analyticsParam, extraFilter);
                break;
            case STATS:
                analytics = !isAdmin() && extraFilter == null ? new StatsAnalytics() : executeStats(analyticsParam, extraFilter);
                break;
        }

        return Response.ok(analytics).build();
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

        switch (analyticsParam.getField()) {
            case API_FIELD:
                if (isAdmin()) {
                    return buildCountStat(apiService.search(new ApiQuery()).size());
                } else {
                    return buildCountStat(apiService.findByUser(getAuthenticatedUser(), new ApiQuery(), false).size());
                }
            case APPLICATION_FIELD:
                if (isAdmin()) {
                    return buildCountStat(applicationService.findAll().size());
                } else {
                    return buildCountStat(applicationService.findByUser(getAuthenticatedUser()).size());
                }
            default:
                return analyticsService.execute(query);
        }
    }

    private StatsAnalytics buildCountStat(float countValue) {
        StatsAnalytics stats = new StatsAnalytics();
        stats.setCount(countValue);
        return stats;
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
        return analyticsService.execute(query);
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

        switch (analyticsParam.getField()) {
            case STATE_FIELD:
                {
                    return getTopHitsAnalytics(api -> api.getState().name());
                }
            case LIFECYCLE_STATE_FIELD:
                {
                    return getTopHitsAnalytics(api -> api.getLifecycleState().name());
                }
            default:
                return analyticsService.execute(query);
        }
    }

    @NotNull
    private TopHitsAnalytics getTopHitsAnalytics(Function<ApiEntity, String> groupingByFunction) {
        Set<ApiEntity> apis = isAdmin()
            ? new HashSet<>(apiService.search(new ApiQuery()))
            : apiService.findByUser(getAuthenticatedUser(), new ApiQuery(), false);
        Map<String, Long> collect = apis.stream().collect(Collectors.groupingBy(groupingByFunction, Collectors.counting()));
        TopHitsAnalytics topHitsAnalytics = new TopHitsAnalytics();
        topHitsAnalytics.setValues(collect);
        return topHitsAnalytics;
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

    private String getExtraFilter(String fieldName, List<String> ids) {
        if (ids != null && !ids.isEmpty()) {
            return fieldName + ":(" + String.join(" OR ", ids) + ")";
        }
        return null;
    }
}
