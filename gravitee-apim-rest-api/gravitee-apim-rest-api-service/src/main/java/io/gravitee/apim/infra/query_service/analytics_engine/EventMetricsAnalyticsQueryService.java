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
import io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name;
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
 * Serves the native Kafka <b>event metrics</b> family, read from the {@code event-metrics} data
 * stream. Distinct from {@link NativeApiAnalyticsQueryService}, which serves
 * {@code NATIVE_CONNECTIONS_SUMMARY} from the connection documents of {@code v4-metrics}: same API
 * type, two different indices, hence two query services. Metrics are routed to the right one by name
 * by {@code AnalyticsQueryContextProvider}, so a single dashboard can mix both families.
 *
 * <p>Unlike the connection family, all three query kinds are supported — the event metrics are plain
 * numeric fields, so measures (KPI tiles) work as well as facets and time series.
 *
 * @author GraviteeSource Team
 */
@Service
public class EventMetricsAnalyticsQueryService implements AnalyticsEngineQueryService {

    private final AnalyticsRepository analyticsRepository;

    public EventMetricsAnalyticsQueryService(@Lazy AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public Set<Name> metrics() {
        return Set.of(
            Name.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM,
            Name.NATIVE_MESSAGES_PRODUCED_UPSTREAM,
            Name.NATIVE_MESSAGES_CONSUMED_DOWNSTREAM,
            Name.NATIVE_MESSAGES_CONSUMED_UPSTREAM,
            Name.NATIVE_BYTES_PRODUCED_DOWNSTREAM,
            Name.NATIVE_BYTES_PRODUCED_UPSTREAM,
            Name.NATIVE_BYTES_CONSUMED_DOWNSTREAM,
            Name.NATIVE_BYTES_CONSUMED_UPSTREAM,
            Name.NATIVE_ACTIVE_CONNECTIONS_DOWNSTREAM,
            Name.NATIVE_ACTIVE_CONNECTIONS_UPSTREAM,
            Name.NATIVE_AUTHENTICATIONS_SUCCESS_DOWNSTREAM,
            Name.NATIVE_AUTHENTICATIONS_SUCCESS_UPSTREAM,
            Name.NATIVE_AUTHENTICATIONS_FAILURE_DOWNSTREAM,
            Name.NATIVE_AUTHENTICATIONS_FAILURE_UPSTREAM,
            Name.NATIVE_OPERATIONS_RECEIVED,
            Name.NATIVE_OPERATIONS_FORWARDED,
            Name.NATIVE_OPERATIONS_ANSWERED,
            Name.NATIVE_OPERATIONS_COMPLETED,
            Name.NATIVE_OPERATION_GATEWAY_REQUEST_DURATION,
            Name.NATIVE_OPERATION_BROKER_DURATION,
            Name.NATIVE_OPERATION_GATEWAY_RESPONSE_DURATION
        );
    }

    @Override
    public MeasuresResponse searchMeasures(ExecutionContext context, MeasuresRequest request) {
        var query = AnalyticsMeasuresAdapter.INSTANCE.fromRequest(request);
        var result = analyticsRepository.searchEventMetricsMeasures(context.getQueryContext(), query);
        return AnalyticsMeasuresAdapter.INSTANCE.fromResult(result);
    }

    @Override
    public FacetsResponse searchFacets(ExecutionContext context, FacetsRequest request) {
        var query = AnalyticsMeasuresAdapter.INSTANCE.fromRequest(request);
        var result = analyticsRepository.searchEventMetricsFacets(context.getQueryContext(), query);
        return AnalyticsMeasuresAdapter.INSTANCE.fromResult(result);
    }

    @Override
    public TimeSeriesResponse searchTimeSeries(ExecutionContext context, TimeSeriesRequest request) {
        var query = AnalyticsMeasuresAdapter.INSTANCE.fromRequest(request);
        var result = analyticsRepository.searchEventMetricsTimeSeries(context.getQueryContext(), query);
        return AnalyticsMeasuresAdapter.INSTANCE.fromResult(result);
    }
}
