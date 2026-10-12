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
package io.gravitee.apim.rest.api.mcp.infra;

import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.FilterValuesPage;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetFilterValuesUseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.rest.api.mcp.core.port.AnalyticsQueries;
import java.time.Instant;

/**
 * The analytics engine through its compute use cases, which restrict every query to the APIs the caller can see.
 */
public class ComputeAnalyticsQueries implements AnalyticsQueries {

    private final ComputeMeasuresUseCase measures;
    private final ComputeFacetsUseCase facets;
    private final ComputeTimeSeriesUseCase timeSeries;
    private final GetFilterValuesUseCase filterValues;

    public ComputeAnalyticsQueries(
        ComputeMeasuresUseCase measures,
        ComputeFacetsUseCase facets,
        ComputeTimeSeriesUseCase timeSeries,
        GetFilterValuesUseCase filterValues
    ) {
        this.measures = measures;
        this.facets = facets;
        this.timeSeries = timeSeries;
        this.filterValues = filterValues;
    }

    @Override
    public MeasuresResponse measures(AuditInfo caller, MeasuresRequest request) {
        return ApimContext.run(caller, () -> measures.execute(new ComputeMeasuresUseCase.Input(caller, request))).response();
    }

    @Override
    public FacetsResponse facets(AuditInfo caller, FacetsRequest request) {
        return ApimContext.run(caller, () -> facets.execute(new ComputeFacetsUseCase.Input(caller, request))).response();
    }

    @Override
    public TimeSeriesResponse timeSeries(AuditInfo caller, TimeSeriesRequest request) {
        return ApimContext.run(caller, () -> timeSeries.execute(new ComputeTimeSeriesUseCase.Input(caller, request))).response();
    }

    @Override
    public FilterValuesPage filterValues(AuditInfo caller, String filterName, Instant from, Instant to, int perPage, String query) {
        var input = new GetFilterValuesUseCase.Input(caller, filterName, from, to, 1, perPage, query);
        return ApimContext.run(caller, () -> filterValues.execute(input)).valuesPage();
    }
}
