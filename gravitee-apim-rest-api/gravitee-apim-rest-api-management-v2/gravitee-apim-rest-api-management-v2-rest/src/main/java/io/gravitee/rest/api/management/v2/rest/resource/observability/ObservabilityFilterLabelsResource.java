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

import io.gravitee.apim.core.analytics_engine.use_case.ResolveFilterLabelsUseCase;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ResolveFilterLabelsRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ResolveFilterLabelsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ResolveFilterLabelsResponseEntry;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

public class ObservabilityFilterLabelsResource extends AbstractResource {

    @Inject
    ResolveFilterLabelsUseCase resolveFilterLabelsUseCase;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResolveFilterLabelsResponse resolveFilterLabels(ResolveFilterLabelsRequest request) {
        if (!canReadDashboards()) {
            throw new ForbiddenAccessException();
        }

        if (request == null || request.getEntries() == null || request.getEntries().isEmpty()) {
            return new ResolveFilterLabelsResponse().entries(List.of());
        }

        var useCaseEntries = request
            .getEntries()
            .stream()
            .map(e ->
                new ResolveFilterLabelsUseCase.Entry(
                    e.getFilterName() != null ? e.getFilterName().getValue() : null,
                    e.getIds() != null ? e.getIds() : List.of()
                )
            )
            .toList();

        var output = resolveFilterLabelsUseCase.execute(new ResolveFilterLabelsUseCase.Input(getAuditInfo(), useCaseEntries));

        var responseEntries = output
            .entries()
            .stream()
            .map(e -> new ResolveFilterLabelsResponseEntry().filterName(FilterName.fromValue(e.filterName())).labels(e.labels()))
            .toList();

        return new ResolveFilterLabelsResponse().entries(responseEntries);
    }
}
