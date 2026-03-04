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

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchStatsResponseAdapter {

    private static final String STATS_AGG = "stats_field";

    public static Optional<StatsAggregate> adapt(SearchResponse response) {
        if (response == null || response.getAggregations() == null || response.getAggregations().isEmpty()) {
            return Optional.of(emptyStats());
        }

        Aggregation agg = response.getAggregations().get(STATS_AGG);
        if (agg == null) {
            return Optional.of(emptyStats());
        }

        long count = agg.getCount() != null ? agg.getCount().longValue() : 0L;
        double min = safeDouble(agg.getMin());
        double max = safeDouble(agg.getMax());
        double avg = safeDouble(agg.getAvg());
        double sum = safeDouble(agg.getSum());

        return Optional.of(new StatsAggregate(count, min, max, avg, sum));
    }

    private static double safeDouble(Number value) {
        if (value == null || Double.isNaN(value.doubleValue())) {
            return 0.0;
        }
        return value.doubleValue();
    }

    private static StatsAggregate emptyStats() {
        return new StatsAggregate(0L, 0.0, 0.0, 0.0, 0.0);
    }
}
