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
package io.gravitee.apim.core.analytics_engine.query_service;

import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.GroupedMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.GroupedMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface AnalyticsEngineQueryService {
    Set<MetricSpec.Name> metrics();

    MeasuresResponse searchMeasures(ExecutionContext context, MeasuresRequest request);

    FacetsResponse searchFacets(ExecutionContext context, FacetsRequest request);

    TimeSeriesResponse searchTimeSeries(ExecutionContext context, TimeSeriesRequest request);

    /** Whether {@link #searchGroupedMeasures} is implemented for the metrics of this engine. */
    default boolean supportsGroupedMeasures() {
        return false;
    }

    /** The same measures computed once per group of documents, in a single request; see {@link GroupedMeasuresRequest}. */
    default GroupedMeasuresResponse searchGroupedMeasures(ExecutionContext context, GroupedMeasuresRequest request) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not compute measures per group");
    }
}
