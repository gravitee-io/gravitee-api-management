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

import io.gravitee.apim.core.analytics.model.StatusRangesQueryParameters;
import io.gravitee.apim.core.analytics.use_case.SearchEnvironmentResponseStatusRangesUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.EnvironmentAnalyticsMapper;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentAnalyticsResponseStatusRangesResponse;
import io.gravitee.rest.api.management.v2.rest.resource.environment.param.TimeRangeParam;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public class EnvironmentAnalyticsResource {

    @Inject
    SearchEnvironmentResponseStatusRangesUseCase searchEnvironmentResponseStatusRangesUseCase;

    @Path("/response-status-ranges")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public EnvironmentAnalyticsResponseStatusRangesResponse getResponseStatusRanges(@BeanParam @Valid TimeRangeParam timeRangeParam) {
        var params = StatusRangesQueryParameters.builder().from(timeRangeParam.getFrom()).to(timeRangeParam.getTo()).build();
        var input = new SearchEnvironmentResponseStatusRangesUseCase.Input(GraviteeContext.getExecutionContext(), params);

        return searchEnvironmentResponseStatusRangesUseCase
            .execute(input)
            .responseStatusRanges()
            .map(EnvironmentAnalyticsMapper.INSTANCE::map)
            .orElseThrow(() -> new NotFoundException("No response status ranges found for environment"));
    }
}
