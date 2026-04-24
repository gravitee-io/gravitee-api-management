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
package io.gravitee.rest.api.portal.rest.resource.analytics;

import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.portal.rest.mapper.PortalAnalyticsMeasuresMapper;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetsRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetsResponse;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMeasuresRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMeasuresResponse;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeSeriesRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeSeriesResponse;
import io.gravitee.rest.api.portal.rest.resource.AbstractResource;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * @author GraviteeSource Team
 */
public class PortalAnalyticsComputationResource extends AbstractResource {

    @Inject
    private ComputeMeasuresUseCase computeMeasuresUseCase;

    @Inject
    private ComputeFacetsUseCase computeFacetsUseCase;

    @Inject
    private ComputeTimeSeriesUseCase computeTimeSeriesUseCase;

    @POST
    @Path("measures")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public AnalyticsMeasuresResponse computeMeasures(@Valid AnalyticsMeasuresRequest request) {
        var input = new ComputeMeasuresUseCase.Input(getAuditInfo(), PortalAnalyticsMeasuresMapper.INSTANCE.toCoreRequest(request));
        var output = computeMeasuresUseCase.execute(input);
        return PortalAnalyticsMeasuresMapper.INSTANCE.toPortalResponse(output.response());
    }

    @POST
    @Path("facets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public AnalyticsFacetsResponse computeFacets(@Valid AnalyticsFacetsRequest request) {
        var input = new ComputeFacetsUseCase.Input(getAuditInfo(), PortalAnalyticsMeasuresMapper.INSTANCE.toCoreRequest(request));
        var output = computeFacetsUseCase.execute(input);
        return PortalAnalyticsMeasuresMapper.INSTANCE.toPortalResponse(output.response());
    }

    @POST
    @Path("time-series")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public AnalyticsTimeSeriesResponse computeTimeSeries(@Valid AnalyticsTimeSeriesRequest request) {
        var input = new ComputeTimeSeriesUseCase.Input(getAuditInfo(), PortalAnalyticsMeasuresMapper.INSTANCE.toCoreRequest(request));
        var output = computeTimeSeriesUseCase.execute(input);
        return PortalAnalyticsMeasuresMapper.INSTANCE.toPortalResponse(output.response());
    }
}
