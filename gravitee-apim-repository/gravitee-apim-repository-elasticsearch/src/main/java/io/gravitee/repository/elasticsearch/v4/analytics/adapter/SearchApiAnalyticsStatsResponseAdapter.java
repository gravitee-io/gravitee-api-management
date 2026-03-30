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

import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsStatsAggregate;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchApiAnalyticsStatsResponseAdapter {

    public static Optional<ApiAnalyticsStatsAggregate> adapt(SearchResponse response) {
        if (response == null || response.getAggregations() == null || response.getAggregations().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
            ApiAnalyticsStatsAggregate
                .builder()
                .count(getAggregationLongValue(response, "count"))
                .min(getAggregationDoubleValue(response, "min"))
                .max(getAggregationDoubleValue(response, "max"))
                .avg(getAggregationDoubleValue(response, "avg"))
                .sum(getAggregationDoubleValue(response, "sum"))
                .build()
        );
    }

    private static long getAggregationLongValue(SearchResponse response, String aggName) {
        var agg = response.getAggregations().get(aggName);
        if (agg == null || agg.getValue() == null) {
            return 0L;
        }
        return agg.getValue().longValue();
    }

    private static Double getAggregationDoubleValue(SearchResponse response, String aggName) {
        var agg = response.getAggregations().get(aggName);
        if (agg == null || agg.getValue() == null) {
            return null;
        }
        return agg.getValue().doubleValue();
    }
}
