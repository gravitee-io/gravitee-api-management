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

import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.vertx.core.json.JsonObject;
import java.util.List;

/**
 * Native-Kafka counterpart of {@link HTTPMeasuresQueryAdapter}: the connection count over the {@code v4-metrics}
 * index, the same COUNT leaf {@link NativeFacetsQueryAdapter} puts under its buckets, with the per-metric filters
 * in the {@code <METRIC>#__FILTER__} envelope the response side unwraps. Native metrics only support COUNT.
 *
 * @author GraviteeSource Team
 */
public class NativeMeasuresQueryAdapter {

    private final FieldResolver fieldResolver = new NativeApiFieldResolver();

    private final FilterAdapter filterAdapter = new FilterAdapter(fieldResolver);

    private final BoolQueryAdapter boolAdapter = new BoolQueryAdapter(filterAdapter);

    private final NativeFacetsQueryAdapter facetsQueryAdapter = new NativeFacetsQueryAdapter();

    public String adapt(MeasuresQuery query) {
        return json(query).toString();
    }

    private JsonObject json(MeasuresQuery query) {
        return new JsonObject().put("size", 0).put("query", boolAdapter.adaptForNative(query)).put("aggs", adaptMetrics(query.metrics()));
    }

    JsonObject adaptMetrics(List<MetricMeasuresQuery> metrics) {
        var aggs = new JsonObject();
        for (var metric : metrics) {
            var measureAggs = facetsQueryAdapter.adaptMeasures(metric);
            if (metric.filters() != null && !metric.filters().isEmpty()) {
                var filterName = AggregationAdapter.adaptName(metric.metric(), AggregationAdapter.FILTER_AGG_SUFFIX);
                aggs.put(
                    filterName,
                    new JsonObject().put("filter", filterAdapter.adaptMetricFilters(metric.filters())).put("aggs", measureAggs)
                );
            } else {
                aggs.mergeIn(measureAggs);
            }
        }
        return aggs;
    }
}
