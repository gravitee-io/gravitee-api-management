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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.use_case.SearchEnvironmentRequestResponseTimeUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchEnvironmentResponseStatusRangesUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchEnvironmentResponseTimeOverTimeUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchEnvironmentTopHitsApisCountUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.EnvironmentAnalyticsMapper;
import io.gravitee.rest.api.management.v2.rest.model.AnalyticTimeRange;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsOverPeriodResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsRequestResponseTimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsResponseStatusRangesResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsTopHitsApisResponse;
import io.gravitee.rest.api.management.v2.rest.resource.environment.param.TimeRangeParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class EnvironmentAnalyticsResource {

    @Inject
    SearchEnvironmentResponseStatusRangesUseCase searchEnvironmentResponseStatusRangesUseCase;

    @Inject
    SearchEnvironmentTopHitsApisCountUseCase searchEnvironmentTopHitsApisCountUseCase;

    @Inject
    SearchEnvironmentRequestResponseTimeUseCase searchEnvironmentRequestResponseTimeUseCase;

    @Inject
    SearchEnvironmentResponseTimeOvertTimeUseCase searchEnvironmentResponseTimeOvertTimeUseCase;

    @Path("/response-status-ranges")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public EnvironmentAnalyticsResponseStatusRangesResponse getResponseStatusRanges(@BeanParam @Valid TimeRangeParam timeRangeParam) {
        var params = AnalyticsQueryParameters.builder().from(timeRangeParam.getFrom()).to(timeRangeParam.getTo()).build();
        var input = new SearchEnvironmentResponseStatusRangesUseCase.Input(GraviteeContext.getExecutionContext(), params);

        return searchEnvironmentResponseStatusRangesUseCase
            .execute(input)
            .responseStatusRanges()
            .map(EnvironmentAnalyticsMapper.INSTANCE::map)
            .orElse(EnvironmentAnalyticsResponseStatusRangesResponse.builder().ranges(Map.of()).build());
    }

    @Path("/top-hits")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public EnvironmentAnalyticsTopHitsApisResponse getTopHitsApis(@BeanParam @Valid TimeRangeParam timeRangeParam) {
        var params = AnalyticsQueryParameters.builder().from(timeRangeParam.getFrom()).to(timeRangeParam.getTo()).build();
        var input = new SearchEnvironmentTopHitsApisCountUseCase.Input(GraviteeContext.getExecutionContext(), params);

        var topHitsApis = searchEnvironmentTopHitsApisCountUseCase.execute(input).topHitsApis();

        if (topHitsApis == null) {
            return EnvironmentAnalyticsTopHitsApisResponse.builder().build();
        }

        return EnvironmentAnalyticsMapper.INSTANCE.map(topHitsApis);
    }

    @Path("/request-response-time")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public EnvironmentAnalyticsRequestResponseTimeResponse getRequestResponseTime(@BeanParam @Valid TimeRangeParam timeRangeParam) {
        var params = AnalyticsQueryParameters.builder().from(timeRangeParam.getFrom()).to(timeRangeParam.getTo()).build();
        var input = new SearchEnvironmentRequestResponseTimeUseCase.Input(GraviteeContext.getExecutionContext(), params);

        return searchEnvironmentRequestResponseTimeUseCase
            .execute(input)
            .requestResponseTime()
            .map(EnvironmentAnalyticsMapper.INSTANCE::map)
            .orElse(EnvironmentAnalyticsRequestResponseTimeResponse.builder().build());
    }

    @Path("/response-time-over-time")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public EnvironmentAnalyticsOverPeriodResponse getResponseTimeOverTime(@QueryParam("from") Long from, @QueryParam("to") Long to) {
        Instant end = to != null ? Instant.ofEpochMilli(to) : Instant.now();
        Instant start = from != null ? Instant.ofEpochMilli(from) : end.minus(Duration.ofDays(1));
        var request = new SearchEnvironmentResponseTimeOverTimeUseCase.Input(GraviteeContext.getCurrentEnvironment(), start, end);

        return searchEnvironmentResponseTimeOverTimeUseCase
            .execute(GraviteeContext.getExecutionContext(), request)
            .map(out ->
                new EnvironmentAnalyticsOverPeriodResponse()
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
}
