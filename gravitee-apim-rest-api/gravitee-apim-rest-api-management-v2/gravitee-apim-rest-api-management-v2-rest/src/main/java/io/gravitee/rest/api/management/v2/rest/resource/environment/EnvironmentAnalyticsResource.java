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

import io.gravitee.apim.core.analytics.model.EnvironmentAnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.use_case.SearchEnvironmentResponseStatusRangesUseCase;
import io.gravitee.apim.core.analytics.use_case.SearchEnvironmentTopHitsApisCountUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.EnvironmentAnalyticsMapper;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsResponseStatusRangesResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsTopHitsApisResponse;
import io.gravitee.rest.api.management.v2.rest.resource.environment.param.TimeRangeParam;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

public class EnvironmentAnalyticsResource {

    @Inject
    SearchEnvironmentResponseStatusRangesUseCase searchEnvironmentResponseStatusRangesUseCase;

    @Inject
    SearchEnvironmentTopHitsApisCountUseCase searchEnvironmentTopHitsApisCountUseCase;

    @Path("/response-status-ranges")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public EnvironmentAnalyticsResponseStatusRangesResponse getResponseStatusRanges(@BeanParam @Valid TimeRangeParam timeRangeParam) {
        var params = EnvironmentAnalyticsQueryParameters.builder().from(timeRangeParam.getFrom()).to(timeRangeParam.getTo()).build();
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
        var params = EnvironmentAnalyticsQueryParameters.builder().from(timeRangeParam.getFrom()).to(timeRangeParam.getTo()).build();
        var input = new SearchEnvironmentTopHitsApisCountUseCase.Input(GraviteeContext.getExecutionContext(), params);

        return searchEnvironmentTopHitsApisCountUseCase
            .execute(input)
            .topHitsApis()
            .map(EnvironmentAnalyticsMapper.INSTANCE::map)
            .orElse(EnvironmentAnalyticsTopHitsApisResponse.builder().data(List.of()).build());
    }
}
