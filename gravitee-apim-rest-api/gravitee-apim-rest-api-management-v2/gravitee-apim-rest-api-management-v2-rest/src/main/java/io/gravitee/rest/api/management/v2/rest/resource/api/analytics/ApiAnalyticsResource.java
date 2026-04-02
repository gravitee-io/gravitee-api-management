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

import io.gravitee.apim.core.analytics.model.AnalyticsType;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsUseCase;
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
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsCountResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsDateHistoResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsGroupByResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsOverPeriodResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsRequestsCountResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponseStatusOvertimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponseStatusRangesResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsStatsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
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
import java.util.Optional;

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
    private SearchApiAnalyticsUseCase searchApiAnalyticsUseCase;

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

    /**
     * Unified analytics endpoint supporting four query types.
     *
     * <p>Required parameters:
     * <ul>
     *   <li>{@code type}     – COUNT | STATS | GROUP_BY | DATE_HISTO</li>
     *   <li>{@code from}     – start of the time window (epoch ms)</li>
     *   <li>{@code to}       – end of the time window (epoch ms)</li>
     *   <li>{@code field}    – metric field name (required for STATS and GROUP_BY)</li>
     *   <li>{@code interval} – bucket interval in ms (required for DATE_HISTO)</li>
     *   <li>{@code size}     – top-N limit for GROUP_BY (default 10)</li>
     * </ul>
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public Response getApiAnalytics(
        @QueryParam("type") String type,
        @QueryParam("from") Long from,
        @QueryParam("to") Long to,
        @QueryParam("field") String field,
        @QueryParam("interval") Long interval,
        @QueryParam("size") @DefaultValue("10") int size
    ) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("Query parameter 'type' is required. Allowed values: COUNT, STATS, GROUP_BY, DATE_HISTO");
        }

        AnalyticsType analyticsType;
        try {
            analyticsType = AnalyticsType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                "Invalid value for query parameter 'type': '" + type + "'. Allowed values: COUNT, STATS, GROUP_BY, DATE_HISTO"
            );
        }

        if ((analyticsType == AnalyticsType.STATS || analyticsType == AnalyticsType.GROUP_BY) && (field == null || field.isBlank())) {
            throw new BadRequestException("Query parameter 'field' is required when type is " + analyticsType);
        }

        if (analyticsType == AnalyticsType.DATE_HISTO && interval == null) {
            throw new BadRequestException("Query parameter 'interval' is required when type is DATE_HISTO");
        }

        var start = Optional.ofNullable(from).map(Instant::ofEpochMilli);
        var end = Optional.ofNullable(to).map(Instant::ofEpochMilli);

        var input = new SearchApiAnalyticsUseCase.Input(
            apiId,
            GraviteeContext.getCurrentEnvironment(),
            analyticsType,
            start,
            end,
            Optional.ofNullable(field),
            Optional.ofNullable(interval),
            size
        );

        var output = searchApiAnalyticsUseCase.execute(GraviteeContext.getExecutionContext(), input);

        Object responseBody =
            switch (output) {
                case SearchApiAnalyticsUseCase.Output.Count c -> ApiAnalyticsCountResponse.builder().count(c.count()).build();
                case SearchApiAnalyticsUseCase.Output.Stats s -> ApiAnalyticsStatsResponse
                    .builder()
                    .count(s.count())
                    .min(s.min())
                    .max(s.max())
                    .avg(s.avg())
                    .sum(s.sum())
                    .build();
                case SearchApiAnalyticsUseCase.Output.GroupBy g -> ApiAnalyticsGroupByResponse
                    .builder()
                    .values(g.values())
                    .metadata(g.metadata())
                    .build();
                case SearchApiAnalyticsUseCase.Output.DateHisto h -> ApiAnalyticsDateHistoResponse
                    .builder()
                    .timestamp(h.timestamps())
                    .values(
                        h
                            .buckets()
                            .stream()
                            .map(b ->
                                ApiAnalyticsDateHistoResponse.Bucket
                                    .builder()
                                    .field(b.field())
                                    .buckets(b.buckets())
                                    .metadata(b.metadata())
                                    .build()
                            )
                            .toList()
                    )
                    .build();
            };

        return Response.ok(responseBody).build();
    }
}
