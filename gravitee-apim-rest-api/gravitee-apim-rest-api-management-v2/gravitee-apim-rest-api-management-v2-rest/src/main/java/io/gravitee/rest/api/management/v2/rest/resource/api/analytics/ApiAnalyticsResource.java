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
package io.gravitee.rest.api.management.v2.rest.resource.api.analytics;

import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.analytics.use_case.SearchAverageConnectionDurationUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchAverageMessagesPerRequestAnalyticsUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchRequestsCountAnalyticsUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchResponseStatusOverTimeUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchResponseStatusRangesUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchResponseTimeUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiAnalyticsMapper;
import io.gravitee.rest.api.management.v2.rest.model.AnalyticTimeRange;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsAverageConnectionDurationResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsAverageMessagesPerRequestResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsOverPeriodResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsRequestsCountResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponseStatusOvertimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponseStatusRangesResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApiAnalyticsResource extends AbstractResource {

    @PathParam("apiId")
    private String apiId;

    @Inject
    private SearchRequestsCountAnalyticsUseCase searchRequestsCountAnalyticsUseCase;

    @Inject
    private SearchAverageMessagesPerRequestAnalyticsUseCase searchAverageMessagesPerRequestAnalyticsUseCase;

    @Inject
    private SearchAverageConnectionDurationUseCase searchAverageConnectionDurationUseCase;

    @Inject
    private SearchResponseStatusRangesUseCase searchResponseStatusRangesUseCase;

    @Inject
    private SearchResponseTimeUseCase searchResponseTimeUseCase;

    @Inject
    private SearchResponseStatusOverTimeUseCase searchResponseStatusOverTimeUseCase;

    @Inject
    private AnalyticsQueryService analyticsQueryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public Response getApiV4Analytics(
        @QueryParam("type") String type,
        @QueryParam("from") Long from,
        @QueryParam("to") Long to,
        @QueryParam("field") String field,
        @QueryParam("interval") Long interval,
        @QueryParam("size") Integer size,
        @QueryParam("order") String order
    ) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("Query parameter 'type' is required");
        }
        if (from == null || to == null) {
            throw new BadRequestException("Query parameters 'from' and 'to' are required");
        }
        if (from >= to) {
            throw new BadRequestException("'from' must be less than 'to'");
        }
        var ctx = GraviteeContext.getExecutionContext();
        var typeUpper = type.toUpperCase();

        switch (typeUpper) {
            case "COUNT" -> {
                var result = analyticsQueryService.searchV4AnalyticsCount(ctx, apiId, from, to);
                var body = new HashMap<String, Object>();
                body.put("type", "COUNT");
                body.put("count", result.map(io.gravitee.rest.api.model.v4.analytics.V4AnalyticsCount::getCount).orElse(0L));
                return Response.ok(body).build();
            }
            case "STATS" -> {
                if (field == null || field.isBlank()) {
                    throw new BadRequestException("Query parameter 'field' is required for type STATS");
                }
                var result = analyticsQueryService.searchV4AnalyticsStats(ctx, apiId, from, to, field);
                var body = new HashMap<String, Object>();
                body.put("type", "STATS");
                if (result.isPresent()) {
                    var s = result.get();
                    body.put("count", s.getCount());
                    body.put("min", s.getMin());
                    body.put("max", s.getMax());
                    body.put("avg", s.getAvg());
                    body.put("sum", s.getSum());
                } else {
                    body.put("count", 0L);
                    body.put("min", 0.0);
                    body.put("max", 0.0);
                    body.put("avg", 0.0);
                    body.put("sum", 0.0);
                }
                return Response.ok(body).build();
            }
            case "GROUP_BY" -> {
                if (field == null || field.isBlank()) {
                    throw new BadRequestException("Query parameter 'field' is required for type GROUP_BY");
                }
                int sizeVal = size != null && size > 0 ? size : 10;
                var result = analyticsQueryService.searchV4AnalyticsGroupBy(ctx, apiId, from, to, field, sizeVal, order);
                var body = new HashMap<String, Object>();
                body.put("type", "GROUP_BY");
                body.put("values", result.map(io.gravitee.rest.api.model.v4.analytics.V4AnalyticsGroupBy::getValues).orElse(Map.of()));
                body.put("metadata", result.map(io.gravitee.rest.api.model.v4.analytics.V4AnalyticsGroupBy::getMetadata).orElse(Map.of()));
                return Response.ok(body).build();
            }
            case "DATE_HISTO" -> {
                if (field == null || field.isBlank()) {
                    throw new BadRequestException("Query parameter 'field' is required for type DATE_HISTO");
                }
                if (interval == null || interval < 1000 || interval > 1_000_000_000) {
                    throw new BadRequestException(
                        "Query parameter 'interval' is required for type DATE_HISTO (milliseconds, 1000-1000000000)"
                    );
                }
                var result = analyticsQueryService.searchV4AnalyticsDateHisto(ctx, apiId, from, to, field, interval);
                var body = new HashMap<String, Object>();
                body.put("type", "DATE_HISTO");
                body.put(
                    "timestamp",
                    result.map(io.gravitee.rest.api.model.v4.analytics.V4AnalyticsDateHisto::getTimestamp).orElse(List.of())
                );
                body.put(
                    "values",
                    result
                        .map(d ->
                            d
                                .getValues()
                                .stream()
                                .map(v -> {
                                    Map<String, Object> m = new HashMap<>();
                                    m.put("field", v.getField());
                                    m.put("buckets", v.getBuckets());
                                    m.put("metadata", v.getMetadata());
                                    return m;
                                })
                                .collect(Collectors.toList())
                        )
                        .orElse(List.of())
                );
                return Response.ok(body).build();
            }
            default -> throw new BadRequestException("Query parameter 'type' must be one of: COUNT, STATS, GROUP_BY, DATE_HISTO");
        }
    }

    @Path("/requests-count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public ApiAnalyticsRequestsCountResponse getApiAnalyticsRequestCount(@QueryParam("from") Long from, @QueryParam("to") Long to) {
        var end = Optional.ofNullable(to).map(Instant::ofEpochMilli);
        var start = Optional.ofNullable(from).map(Instant::ofEpochMilli);

        var request = new SearchRequestsCountAnalyticsUseCase.Input(apiId, GraviteeContext.getCurrentEnvironment(), start, end);

        return searchRequestsCountAnalyticsUseCase
            .execute(GraviteeContext.getExecutionContext(), request)
            .requestsCount()
            .map(ApiAnalyticsMapper.INSTANCE::map)
            .orElseThrow(() -> new NotFoundException("No requests count found for api: " + apiId));
    }

    @Path("/average-messages-per-request")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public ApiAnalyticsAverageMessagesPerRequestResponse getAverageMessagesPerRequest(
        @QueryParam("from") Long from,
        @QueryParam("to") Long to
    ) {
        var end = Optional.ofNullable(to).map(Instant::ofEpochMilli);
        var start = Optional.ofNullable(from).map(Instant::ofEpochMilli);

        var request = new SearchAverageMessagesPerRequestAnalyticsUseCase.Input(apiId, GraviteeContext.getCurrentEnvironment(), start, end);

        return searchAverageMessagesPerRequestAnalyticsUseCase
            .execute(GraviteeContext.getExecutionContext(), request)
            .averageMessagesPerRequest()
            .map(ApiAnalyticsMapper.INSTANCE::map)
            .orElseThrow(() -> new NotFoundException("No average message per request found for api: " + apiId));
    }

    @Path("/average-connection-duration")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public ApiAnalyticsAverageConnectionDurationResponse getAverageConnectionDuration(
        @QueryParam("from") Long from,
        @QueryParam("to") Long to
    ) {
        var end = Optional.ofNullable(to).map(Instant::ofEpochMilli);
        var start = Optional.ofNullable(from).map(Instant::ofEpochMilli);

        var request = new SearchAverageConnectionDurationUseCase.Input(apiId, GraviteeContext.getCurrentEnvironment(), start, end);

        return searchAverageConnectionDurationUseCase
            .execute(GraviteeContext.getExecutionContext(), request)
            .averageConnectionDuration()
            .map(ApiAnalyticsMapper.INSTANCE::map)
            .orElseThrow(() -> new NotFoundException("No connection duration found for api: " + apiId));
    }

    @Path("/response-status-ranges")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public ApiAnalyticsResponseStatusRangesResponse getResponseStatusRanges(@QueryParam("from") Long from, @QueryParam("to") Long to) {
        var request = new SearchResponseStatusRangesUseCase.Input(apiId, GraviteeContext.getCurrentEnvironment(), from, to);

        return searchResponseStatusRangesUseCase
            .execute(GraviteeContext.getExecutionContext(), request)
            .responseStatusRanges()
            .map(ApiAnalyticsMapper.INSTANCE::map)
            .orElseThrow(() -> new NotFoundException("No response status ranges found for api: " + apiId));
    }

    @Path("/response-time-over-time")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public ApiAnalyticsOverPeriodResponse getResponseTimeOverTime(@QueryParam("from") Long from, @QueryParam("to") Long to) {
        Instant end = to != null ? Instant.ofEpochMilli(to) : Instant.now();
        Instant start = from != null ? Instant.ofEpochMilli(from) : end.minus(Duration.ofDays(1));
        var request = new SearchResponseTimeUseCase.Input(apiId, GraviteeContext.getCurrentEnvironment(), start, end);

        return searchResponseTimeUseCase
            .execute(GraviteeContext.getExecutionContext(), request)
            .map(out ->
                new ApiAnalyticsOverPeriodResponse()
                    .timeRange(
                        new AnalyticTimeRange()
                            .from(out.from().toEpochMilli())
                            .to(out.to().toEpochMilli())
                            .interval(out.interval().toMillis())
                    )
                    .data(out.data())
            )
            .blockingGet();
    }

    @Path("/response-status-overtime")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public ApiAnalyticsResponseStatusOvertimeResponse getResponseStatusOvertime(@QueryParam("from") Long from, @QueryParam("to") Long to) {
        Instant end = to != null ? Instant.ofEpochMilli(to) : Instant.now();
        Instant start = from != null ? Instant.ofEpochMilli(from) : end.minus(Duration.ofDays(1));
        var request = new SearchResponseStatusOverTimeUseCase.Input(apiId, GraviteeContext.getCurrentEnvironment(), start, end);

        var result = searchResponseStatusOverTimeUseCase.execute(GraviteeContext.getExecutionContext(), request).responseStatusOvertime();

        if (result == null) {
            return new ApiAnalyticsResponseStatusOvertimeResponse();
        }

        return ApiAnalyticsMapper.INSTANCE.map(result);
    }
}
