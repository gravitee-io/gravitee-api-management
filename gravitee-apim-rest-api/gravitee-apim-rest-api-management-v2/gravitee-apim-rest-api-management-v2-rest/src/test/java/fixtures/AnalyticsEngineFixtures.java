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
package fixtures;

import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.*;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AnalyticsEngineFixtures {

    public static final String API_ID = "faf12066-2195-423c-ade4-ac5bd2bf8d4f";

    public static final Supplier<MeasuresRequest> v = () -> new MeasuresRequest().timeRange(timeRange());

    public static MeasuresRequest aCountMeasureRequest(Filter... filter) {
        var filters = Arrays.asList(filter);
        return new MeasuresRequest().timeRange(timeRange()).filters(filters).metrics(List.of(messageCount(), httpRequestCount()));
    }

    public static FacetsRequest aRequestCountFacetRequest(Filter... filters) {
        var metric = new FacetMetricRequest().name(MetricName.HTTP_REQUESTS).measures(List.of(MeasureName.COUNT));

        return new FacetsRequest()
            .timeRange(timeRange())
            .filters(Arrays.asList(filters))
            .by(List.of(FacetName.HTTP_STATUS))
            .ranges(List.of(new NumberRange().from(100).to(199), new NumberRange().from(200).to(299)))
            .metrics(List.of(metric));
    }

    public static TimeSeriesRequest aRequestCountTimeSeries(Filter... filters) {
        var metric = new FacetMetricRequest().name(MetricName.HTTP_REQUESTS).measures(List.of(MeasureName.COUNT));

        return new TimeSeriesRequest()
            .timeRange(timeRange())
            .interval(new CustomInterval(3600000L))
            .filters(Arrays.asList(filters))
            .ranges(List.of(new NumberRange().from(100).to(199), new NumberRange().from(200).to(299)))
            .metrics(List.of(metric));
    }

    public static MetricRequest httpRequestCount() {
        var metricName = MetricName.HTTP_REQUESTS;
        var measures = List.of(MeasureName.COUNT);
        return new MetricRequest().name(metricName).measures(measures);
    }

    public static MetricRequest messageCount() {
        var metricName = MetricName.MESSAGES;
        var measures = List.of(MeasureName.COUNT);
        return new MetricRequest().name(metricName).measures(measures);
    }

    public static TimeRange timeRange() {
        var from = OffsetDateTime.parse("2025-01-01T00:00:00Z");
        var to = OffsetDateTime.parse("2025-01-02T00:00:00Z");
        return new TimeRange().from(from).to(to);
    }
}
