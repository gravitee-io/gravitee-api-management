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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchStatsResponseAdapterTest {

    @Test
    void should_return_empty_stats_when_aggregations_null() {
        var response = new SearchResponse();
        var result = SearchStatsResponseAdapter.adapt(response);
        assertThat(result).hasValueSatisfying(agg -> assertThat(agg).isEqualTo(new StatsAggregate(0L, 0.0, 0.0, 0.0, 0.0)));
    }

    @Test
    void should_return_empty_stats_when_aggregations_empty() {
        var response = new SearchResponse();
        response.setAggregations(java.util.Map.of());
        var result = SearchStatsResponseAdapter.adapt(response);
        assertThat(result).hasValueSatisfying(agg -> assertThat(agg).isEqualTo(new StatsAggregate(0L, 0.0, 0.0, 0.0, 0.0)));
    }

    @Test
    void should_return_stats_from_aggregation() {
        var agg = new Aggregation();
        agg.setCount(100f);
        agg.setMin(5f);
        agg.setMax(500f);
        agg.setAvg(42.5f);
        agg.setSum(4250f);

        var response = new SearchResponse();
        response.setAggregations(java.util.Map.of("stats_field", agg));

        var result = SearchStatsResponseAdapter.adapt(response);

        assertThat(result)
            .hasValueSatisfying(s -> {
                assertThat(s.count()).isEqualTo(100L);
                assertThat(s.min()).isEqualTo(5.0);
                assertThat(s.max()).isEqualTo(500.0);
                assertThat(s.avg()).isEqualTo(42.5);
                assertThat(s.sum()).isEqualTo(4250.0);
            });
    }

    @Test
    void should_coerce_null_min_max_avg_sum_to_zero_for_empty_range() {
        var agg = new Aggregation();
        agg.setCount(0f);
        agg.setMin(null);
        agg.setMax(null);
        agg.setAvg(null);
        agg.setSum(null);

        var response = new SearchResponse();
        response.setAggregations(java.util.Map.of("stats_field", agg));

        var result = SearchStatsResponseAdapter.adapt(response);

        assertThat(result)
            .hasValueSatisfying(s -> {
                assertThat(s.count()).isZero();
                assertThat(s.min()).isEqualTo(0.0);
                assertThat(s.max()).isEqualTo(0.0);
                assertThat(s.avg()).isEqualTo(0.0);
                assertThat(s.sum()).isEqualTo(0.0);
            });
    }

    @Test
    void should_return_empty_stats_when_stats_agg_missing() {
        var otherAgg = new Aggregation();
        otherAgg.setCount(10f);
        var response = new SearchResponse();
        response.setAggregations(java.util.Map.of("other_agg", otherAgg));

        var result = SearchStatsResponseAdapter.adapt(response);

        assertThat(result).hasValueSatisfying(agg -> assertThat(agg).isEqualTo(new StatsAggregate(0L, 0.0, 0.0, 0.0, 0.0)));
    }
}
