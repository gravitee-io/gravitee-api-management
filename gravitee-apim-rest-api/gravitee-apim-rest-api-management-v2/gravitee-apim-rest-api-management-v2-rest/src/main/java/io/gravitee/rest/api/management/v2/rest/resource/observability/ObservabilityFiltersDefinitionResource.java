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

import io.gravitee.apim.core.analytics_engine.use_case.GetAnalyticsFilterDefinitionsUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.AnalyticsDefinitionMapper;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public class ObservabilityFiltersDefinitionResource extends AbstractResource {

    @Inject
    GetAnalyticsFilterDefinitionsUseCase getAnalyticsFilterDefinitions;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public FilterSpecsResponse getFilterDefinitions() {
        if (!canReadDashboards()) {
            throw new ForbiddenAccessException();
        }

        return AnalyticsDefinitionMapper.INSTANCE.toFilterSpecsResponse(getAnalyticsFilterDefinitions.execute().specs());
    }
}
