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

import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsCountUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsDateHistoUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsGroupByUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsStatsUseCase;
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
import io.gravitee.rest.api.management.v2.rest.model.ApiUnifiedAnalyticsQueryType;
import io.gravitee.rest.api.management.v2.rest.model.ApiUnifiedAnalyticsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
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
    private SearchApiAnalyticsCountUseCase searchApiAnalyticsCountUseCase;

    @Inject
    private SearchApiAnalyticsStatsUseCase searchApiAnalyticsStatsUseCase;

    @Inject
    private SearchApiAnalyticsGroupByUseCase searchApiAnalyticsGroupByUseCase;

    @Inject
    private SearchApiAnalyticsDateHistoUseCase searchApiAnalyticsDateHistoUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public ApiUnifiedAnalyticsResponse getApiUnifiedAnalytics(
        @QueryParam("type") String typeParam,
        @QueryParam("from") Long from,
        @QueryParam("to") Long to,
        @QueryParam("field") String field,
        @QueryParam("interval") Long interval,
        @QueryParam("size") Integer size,
        @QueryParam("order") String order
    ) {
        final ApiUnifiedAnalyticsQueryType queryType = ApiUnifiedAnalyticsSupport.parseQueryType(typeParam);
        ApiUnifiedAnalyticsSupport.validateTimeRangeMillis(from, to);
        final Instant start = Instant.ofEpochMilli(from);
        final Instant end = Instant.ofEpochMilli(to);
        final String env = GraviteeContext.getCurrentEnvironment();

        return switch (queryType) {
            case COUNT -> ApiUnifiedAnalyticsSupport.toCountResponse(
                searchApiAnalyticsCountUseCase
                    .execute(
                        GraviteeContext.getExecutionContext(),
                        new SearchApiAnalyticsCountUseCase.Input(apiId, env, Optional.of(start), Optional.of(end))
                    )
                    .aggregate()
            );
            case STATS -> ApiUnifiedAnalyticsSupport.toStatsResponse(
                searchApiAnalyticsStatsUseCase
                    .execute(
                        GraviteeContext.getExecutionContext(),
                        new SearchApiAnalyticsStatsUseCase.Input(
                            apiId,
                            env,
                            Optional.of(start),
                            Optional.of(end),
                            Optional.of(ApiUnifiedAnalyticsSupport.requireField(field, "STATS"))
                        )
                    )
                    .aggregate()
            );
            case GROUP_BY -> ApiUnifiedAnalyticsSupport.toGroupByResponse(
                searchApiAnalyticsGroupByUseCase
                    .execute(
                        GraviteeContext.getExecutionContext(),
                        new SearchApiAnalyticsGroupByUseCase.Input(
                            apiId,
                            env,
                            Optional.of(start),
                            Optional.of(end),
                            Optional.of(ApiUnifiedAnalyticsSupport.requireField(field, "GROUP_BY")),
                            Optional.of(ApiUnifiedAnalyticsSupport.resolveGroupBySize(size)),
                            ApiUnifiedAnalyticsSupport.parseGroupByOrder(order)
                        )
                    )
                    .aggregate()
            );
            case DATE_HISTO -> {
                ApiUnifiedAnalyticsSupport.validateDateHistogram(from, to, interval);
                yield ApiUnifiedAnalyticsSupport.toDateHistoResponse(
                    searchApiAnalyticsDateHistoUseCase
                        .execute(
                            GraviteeContext.getExecutionContext(),
                            new SearchApiAnalyticsDateHistoUseCase.Input(
                                apiId,
                                env,
                                Optional.of(start),
                                Optional.of(end),
                                ApiUnifiedAnalyticsSupport.optionalField(field),
                                Optional.of(Duration.ofMillis(interval))
                            )
                        )
                        .aggregate()
                );
            }
        };
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
