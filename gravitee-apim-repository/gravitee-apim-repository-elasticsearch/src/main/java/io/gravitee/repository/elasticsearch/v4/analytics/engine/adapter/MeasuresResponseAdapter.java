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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MetricMeasuresResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MeasuresResponseAdapter extends AbstractResponseAdapter {

    public MeasuresResult adapt(SearchResponse esResponse, MeasuresQuery query) {
        return lookupForAggregations(esResponse)
            .map(aggregations -> toMeasuresResult(aggregations, query))
            .orElseGet(() -> empty(query));
    }

    private MeasuresResult toMeasuresResult(Map<String, Aggregation> aggregations, MeasuresQuery query) {
        var metricAndMeasures = AggregationAdapter.toMetricsAndMeasures(aggregations, query);
        var metricResults = new ArrayList<MetricMeasuresResult>();

        for (var entry : metricAndMeasures.entrySet()) {
            metricResults.add(new MetricMeasuresResult(entry.getKey(), entry.getValue()));
        }

        return new MeasuresResult(metricResults);
    }

    private MeasuresResult empty(MeasuresQuery query) {
        return new MeasuresResult(emptyMetrics(query));
    }

    private List<MetricMeasuresResult> emptyMetrics(MeasuresQuery query) {
        return query.metrics().stream().map(this::emptyMetric).toList();
    }

    private MetricMeasuresResult emptyMetric(MetricMeasuresQuery query) {
        return new MetricMeasuresResult(query.metric(), emptyMeasures(query));
    }

    private Map<Measure, Number> emptyMeasures(MetricMeasuresQuery query) {
        return query.measures().stream().collect(Collectors.toMap(measure -> measure, measure -> 0));
    }
}
