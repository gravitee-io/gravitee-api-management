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

import io.gravitee.apim.core.analytics.use_case.SearchAverageConnectionDurationUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchAverageMessagesPerRequestAnalyticsUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchRequestsCountAnalyticsUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchResponseStatusRangesUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchResponseTimeUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiAnalyticsMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsAverageConnectionDurationResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsAverageMessagesPerRequestResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsOverPeriodResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsOverPeriodResponseTimeRange;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsRequestsCountResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponseStatusRangesResponse;
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
import jakarta.ws.rs.core.MediaType;

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

    @Path("/requests-count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_ANALYTICS, acls = { RolePermissionAction.READ }) })
    public ApiAnalyticsRequestsCountResponse getApiAnalyticsRequestCount() {
        var request = new SearchRequestsCountAnalyticsUseCase.Input(apiId, GraviteeContext.getCurrentEnvironment());

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
    public ApiAnalyticsAverageMessagesPerRequestResponse getAverageMessagesPerRequest() {
        var request = new SearchAverageMessagesPerRequestAnalyticsUseCase.Input(apiId, GraviteeContext.getCurrentEnvironment());

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
    public ApiAnalyticsAverageConnectionDurationResponse getAverageConnectionDuration() {
        var request = new SearchAverageConnectionDurationUseCase.Input(apiId, GraviteeContext.getCurrentEnvironment());

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
    public ApiAnalyticsResponseStatusRangesResponse getResponseStatusRanges() {
        var request = new SearchResponseStatusRangesUseCase.Input(apiId, GraviteeContext.getCurrentEnvironment());

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
    public ApiAnalyticsOverPeriodResponse getResponseTimeOverTime() {
        var request = new SearchResponseTimeUseCase.Input(apiId, GraviteeContext.getCurrentEnvironment());

        return searchResponseTimeUseCase
            .execute(GraviteeContext.getExecutionContext(), request)
            .map(out ->
                new ApiAnalyticsOverPeriodResponse()
                    .timeRange(
                        new ApiAnalyticsOverPeriodResponseTimeRange()
                            .from(out.from().toEpochMilli())
                            .to(out.to().toEpochMilli())
                            .interval(out.interval().toMillis())
                    )
                    .data(out.data())
            )
            .blockingGet();
    }
}
