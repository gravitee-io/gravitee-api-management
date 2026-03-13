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
package io.gravitee.apim.infra.query_service.analytics_engine;

import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.infra.adapter.AnalyticsMeasuresAdapter;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
public class MessageDataPlaneQueryService implements AnalyticsEngineQueryService {

    private final AnalyticsRepository analyticsRepository;

    public MessageDataPlaneQueryService(@Lazy AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public Set<MetricSpec.Name> metrics() {
        return Set.of(
            MetricSpec.Name.MESSAGE_PAYLOAD_SIZE,
            MetricSpec.Name.MESSAGE_ERRORS,
            MetricSpec.Name.MESSAGES,
            MetricSpec.Name.MESSAGE_GATEWAY_LATENCY
        );
    }

    @Override
    public MeasuresResponse searchMeasures(ExecutionContext context, MeasuresRequest request) {
        var query = AnalyticsMeasuresAdapter.INSTANCE.fromRequest(request);
        var result = analyticsRepository.searchMessageMeasures(context.getQueryContext(), query);
        return AnalyticsMeasuresAdapter.INSTANCE.fromResult(result);
    }

    @Override
    public FacetsResponse searchFacets(ExecutionContext context, FacetsRequest request) {
        throw new UnsupportedOperationException("Facets are not supported for message analytics");
    }

    @Override
    public TimeSeriesResponse searchTimeSeries(ExecutionContext context, TimeSeriesRequest request) {
        throw new UnsupportedOperationException("Time series are not supported for message analytics");
    }
}
