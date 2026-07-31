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
import io.gravitee.apim.core.observability.model.Signal;
import io.gravitee.rest.api.management.v2.rest.mapper.AnalyticsDefinitionMapper;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ObservabilityFiltersDefinitionResource extends AbstractResource {

    @Inject
    GetAnalyticsFilterDefinitionsUseCase getAnalyticsFilterDefinitions;

    /**
     * @param signals optional, repeatable. Narrows the catalog to the filters advertised for those signals, so a
     *                consumer only offers filters its own query path can honour. Omitting it returns the whole
     *                catalog, which is the historical behaviour.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public FilterSpecsResponse getFilterDefinitions(@QueryParam("signal") List<String> signals) {
        if (!canReadDashboards()) {
            throw new ForbiddenAccessException();
        }

        var input = new GetAnalyticsFilterDefinitionsUseCase.Input(parseSignals(signals));
        return AnalyticsDefinitionMapper.INSTANCE.toFilterSpecsResponse(getAnalyticsFilterDefinitions.execute(input).specs());
    }

    private static Set<Signal> parseSignals(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        Set<Signal> result = EnumSet.noneOf(Signal.class);
        for (String token : raw) {
            if (token == null || token.isBlank()) {
                continue;
            }
            try {
                result.add(Signal.valueOf(token.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unknown signal value '" + token + "'", e);
            }
        }
        return result;
    }
}
