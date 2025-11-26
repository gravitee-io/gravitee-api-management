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
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MetricFacetsResult;
import java.util.List;
import java.util.Map;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FacetsResponseAdapter extends AbstractResponseAdapter {

    public FacetsResult adapt(SearchResponse esResponse, FacetsQuery query) {
        return lookupForAggregations(esResponse)
            .map(aggregations -> toFacetsResult(aggregations, query))
            .orElseGet(() -> empty(query));
    }

    private FacetsResult toFacetsResult(Map<String, Aggregation> aggregations, FacetsQuery query) {
        return new FacetsResult(AggregationAdapter.toMetricsAndBuckets(aggregations, query));
    }

    FacetsResult empty(FacetsQuery query) {
        return new FacetsResult(emptyMetrics(query));
    }

    private List<MetricFacetsResult> emptyMetrics(FacetsQuery query) {
        return query.metrics().stream().map(this::emptyBuckets).toList();
    }

    private MetricFacetsResult emptyBuckets(MetricMeasuresQuery query) {
        return new MetricFacetsResult(query.metric(), List.of());
    }
}
