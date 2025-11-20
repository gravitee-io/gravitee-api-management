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

import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MetricFacetsResult;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class FacetsResponseAdapter {

    public FacetsResult adapt(SearchResponse esResponse, FacetsQuery query) {
        if (esResponse == null) {
            log.debug("Returning empty response because the esResponse is null");
            return empty(query);
        }
        if (esResponse.getTimedOut()) {
            log.debug("Returning empty response because the esResponse timed out");
            return empty(query);
        }

        var aggregations = esResponse.getAggregations();

        if (aggregations == null || aggregations.isEmpty()) {
            log.debug("Returning empty response because the esResponse does not contain aggregations");
            return empty(query);
        }

        var facets = new ArrayList<>(query.facets());

        if (facets.isEmpty()) {
            log.debug("Returning empty response because the query does not contain facets");
            return empty(query);
        }

        return new FacetsResult(AggregationAdapter.toMetricsAndBuckets(aggregations, query));
    }

    private FacetsResult empty(FacetsQuery query) {
        return new FacetsResult(emptyMetrics(query));
    }

    private List<MetricFacetsResult> emptyMetrics(FacetsQuery query) {
        return query.metrics().stream().map(this::emptyBuckets).toList();
    }

    private MetricFacetsResult emptyBuckets(MetricMeasuresQuery query) {
        return new MetricFacetsResult(query.metric(), List.of());
    }
}
