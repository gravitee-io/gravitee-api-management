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
package io.gravitee.rest.api.management.v2.rest.resource.analytics.computation;

import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.AnalyticsMeasuresMapper;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetsRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeSeriesRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeSeriesResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AnalyticsComputationResource extends AbstractResource {

    @Inject
    ComputeMeasuresUseCase computeMeasuresUseCase;

    @Inject
    ComputeFacetsUseCase computeFacetsUseCase;

    @Inject
    ComputeTimeSeriesUseCase computeTimeSeriesUseCase;

    @POST
    @Path("/measures")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public MeasuresResponse computeMeasures(@Valid MeasuresRequest request) {
        var input = new ComputeMeasuresUseCase.Input(getAuditInfo(), AnalyticsMeasuresMapper.INSTANCE.fromRequestEntity(request));
        var output = computeMeasuresUseCase.execute(input);
        return AnalyticsMeasuresMapper.INSTANCE.fromResponseModel(output.response());
    }

    @POST
    @Path("/facets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public FacetsResponse computeFacets(@Valid FacetsRequest request) {
        var input = new ComputeFacetsUseCase.Input(getAuditInfo(), AnalyticsMeasuresMapper.INSTANCE.fromRequestEntity(request));
        var output = computeFacetsUseCase.execute(input);
        return AnalyticsMeasuresMapper.INSTANCE.fromResponseModel(output.response());
    }

    @POST
    @Path("/time-series")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TimeSeriesResponse computeFacets(@Valid TimeSeriesRequest request) {
        var input = new ComputeTimeSeriesUseCase.Input(getAuditInfo(), AnalyticsMeasuresMapper.INSTANCE.fromRequestEntity(request));
        var output = computeTimeSeriesUseCase.execute(input);
        return AnalyticsMeasuresMapper.INSTANCE.fromResponseModel(output.response());
    }
}
