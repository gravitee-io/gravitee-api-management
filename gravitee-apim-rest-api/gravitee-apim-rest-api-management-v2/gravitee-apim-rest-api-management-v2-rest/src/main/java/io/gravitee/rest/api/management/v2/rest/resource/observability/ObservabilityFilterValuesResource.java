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
package io.gravitee.rest.api.management.v2.rest.resource.observability;

import io.gravitee.apim.core.analytics_engine.use_case.GetFilterValuesUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.AnalyticsDefinitionMapper;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterValuesResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;

public class ObservabilityFilterValuesResource extends AbstractResource {

    @Inject
    GetFilterValuesUseCase getFilterValuesUseCase;

    @PathParam("filterName")
    String filterName;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public FilterValuesResponse getFilterValues(
        @QueryParam("from") Long from,
        @QueryParam("to") Long to,
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("perPage") @DefaultValue("10") int perPage,
        @QueryParam("query") String query
    ) {
        if (!canReadDashboards()) {
            throw new ForbiddenAccessException();
        }

        var input = new GetFilterValuesUseCase.Input(
            getAuditInfo(),
            filterName,
            from != null ? Instant.ofEpochMilli(from) : null,
            to != null ? Instant.ofEpochMilli(to) : null,
            page,
            perPage,
            query
        );

        var output = getFilterValuesUseCase.execute(input);

        return AnalyticsDefinitionMapper.INSTANCE.toFilterValuesResponse(output.valuesPage(), page, perPage);
    }
}
