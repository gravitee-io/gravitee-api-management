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
package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchStatsQueryAdapter.FIELD_STATS_AGG;

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchStatsResponseAdapter {

    public static Optional<StatsAggregate> adapt(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }
        final var statsAggregation = aggregations.get(FIELD_STATS_AGG);
        if (statsAggregation == null || statsAggregation.getCount() == null) {
            return Optional.empty();
        }

        return Optional.of(
            StatsAggregate
                .builder()
                .count(statsAggregation.getCount().longValue())
                .min(statsAggregation.getMin() != null ? statsAggregation.getMin().doubleValue() : 0.0)
                .max(statsAggregation.getMax() != null ? statsAggregation.getMax().doubleValue() : 0.0)
                .avg(statsAggregation.getAvg() != null ? statsAggregation.getAvg().doubleValue() : 0.0)
                .sum(statsAggregation.getSum() != null ? statsAggregation.getSum().doubleValue() : 0.0)
                .build()
        );
    }
}
