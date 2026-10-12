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
package io.gravitee.gamma.rest.core.observability.analytics.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SeriesRankingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<AnalyticsFacetMetricQuery> REQUEST_COUNT = List.of(metric("HTTP_REQUESTS", "COUNT"));

    @Test
    void should_keep_the_top_values_by_the_plotted_measure() {
        var facets = facets(
            """
            {"metrics": [{"metric": "HTTP_REQUESTS", "buckets": [
              {"key": "a", "measures": [{"name": "COUNT", "value": 1}]},
              {"key": "b", "measures": [{"name": "COUNT", "value": 30}]},
              {"key": "c", "measures": [{"name": "COUNT", "value": 20}]},
              {"key": "d", "measures": [{"name": "COUNT", "value": 10}]}
            ]}]}
            """
        );

        var ranking = SeriesRanking.rank(facets, REQUEST_COUNT, 1, 2);

        assertThat(ranking.keysAt(0)).containsExactly("b", "c");
        assertThat(ranking.includes("HTTP_REQUESTS", List.of("b"))).isTrue();
        assertThat(ranking.includes("HTTP_REQUESTS", List.of("d"))).isFalse();
    }

    @Test
    void should_rank_by_the_first_measure_the_metric_plots_whatever_order_the_engine_returned() {
        var facets = facets(
            """
            {"metrics": [{"metric": "HTTP_GATEWAY_RESPONSE_TIME", "buckets": [
              {"key": "slow", "measures": [{"name": "MAX", "value": 900}, {"name": "P95", "value": 300}]},
              {"key": "steady", "measures": [{"name": "MAX", "value": 400}, {"name": "P95", "value": 350}]}
            ]}]}
            """
        );

        var ranking = SeriesRanking.rank(facets, List.of(metric("HTTP_GATEWAY_RESPONSE_TIME", "P95", "MAX")), 1, 1);

        assertThat(ranking.keysAt(0)).containsExactly("steady");
    }

    @Test
    void should_break_a_tie_by_value_ascending() {
        var facets = facets(
            """
            {"metrics": [{"metric": "HTTP_REQUESTS", "buckets": [
              {"key": "top", "measures": [{"name": "COUNT", "value": 9}]},
              {"key": "z", "measures": [{"name": "COUNT", "value": 5}]},
              {"key": "m", "measures": [{"name": "COUNT", "value": 5}]}
            ]}]}
            """
        );

        var ranking = SeriesRanking.rank(facets, REQUEST_COUNT, 1, 2);

        assertThat(ranking.keysAt(0)).containsExactly("m", "top");
    }

    @Test
    void should_rank_a_value_without_the_measure_after_every_measured_value() {
        var facets = facets(
            """
            {"metrics": [{"metric": "HTTP_REQUESTS", "buckets": [
              {"key": "empty", "measures": [{"name": "COUNT", "value": null}]},
              {"key": "a", "measures": []},
              {"key": "z", "measures": [{"name": "COUNT", "value": 0}]}
            ]}]}
            """
        );

        var ranking = SeriesRanking.rank(facets, REQUEST_COUNT, 1, 2);

        assertThat(ranking.keysAt(0)).as("z first, then the unmeasured ones by key").containsExactly("a", "z");
    }

    @Test
    void should_rank_by_value_a_metric_that_plots_no_measure() {
        var facets = facets(
            """
            {"metrics": [{"metric": "HTTP_REQUESTS", "buckets": [
              {"key": "z", "measures": []},
              {"key": "m", "measures": []},
              {"key": "a", "measures": []}
            ]}]}
            """
        );

        var ranking = SeriesRanking.rank(facets, List.of(metric("HTTP_REQUESTS")), 1, 2);

        assertThat(ranking.keysAt(0)).containsExactly("a", "m");
    }

    @Test
    void should_rank_the_innermost_facet_within_each_outer_value() {
        var facets = facets(
            """
            {"metrics": [{"metric": "HTTP_REQUESTS", "buckets": [
              {"key": "api-1", "buckets": [
                {"key": "gpt", "measures": [{"name": "COUNT", "value": 9}]},
                {"key": "claude", "measures": [{"name": "COUNT", "value": 8}]},
                {"key": "mistral", "measures": [{"name": "COUNT", "value": 1}]}
              ]},
              {"key": "api-2", "buckets": [
                {"key": "mistral", "measures": [{"name": "COUNT", "value": 4}]},
                {"key": "gpt", "measures": [{"name": "COUNT", "value": 2}]}
              ]}
            ]}]}
            """
        );

        var ranking = SeriesRanking.rank(facets, REQUEST_COUNT, 2, 1);

        assertThat(ranking.includes("HTTP_REQUESTS", List.of("api-1", "gpt"))).isTrue();
        assertThat(ranking.includes("HTTP_REQUESTS", List.of("api-2", "mistral"))).isTrue();
        assertThat(ranking.includes("HTTP_REQUESTS", List.of("api-1", "mistral"))).as("top 1 of api-1 only").isFalse();
        assertThat(ranking.includes("HTTP_REQUESTS", List.of("api-2"))).as("a prefix of a ranked series").isTrue();
        assertThat(ranking.keysAt(0)).containsExactly("api-1", "api-2");
        assertThat(ranking.keysAt(1)).containsExactly("gpt", "mistral");
    }

    @Test
    void should_rank_at_the_depth_the_engine_broke_the_values_down_to() {
        // Single-facet engines (native, event metrics, authz) group by the first facet only.
        var facets = facets(
            """
            {"metrics": [{"metric": "NATIVE_MESSAGES_PRODUCED", "buckets": [
              {"key": "topic-a", "buckets": null, "measures": [{"name": "SUM", "value": 1}]},
              {"key": "topic-b", "buckets": null, "measures": [{"name": "SUM", "value": 9}]}
            ]}]}
            """
        );

        var ranking = SeriesRanking.rank(facets, List.of(metric("NATIVE_MESSAGES_PRODUCED", "SUM")), 2, 1);

        assertThat(ranking.depth()).isEqualTo(1);
        assertThat(ranking.keysAt(0)).containsExactly("topic-b");
        assertThat(ranking.keysAt(1)).isEmpty();
    }

    @Test
    void should_rank_each_metric_on_its_own() {
        var facets = facets(
            """
            {"metrics": [
              {"metric": "HTTP_REQUESTS", "buckets": [
                {"key": "busy", "measures": [{"name": "COUNT", "value": 100}]},
                {"key": "slow", "measures": [{"name": "COUNT", "value": 2}]}
              ]},
              {"metric": "HTTP_GATEWAY_LATENCY", "buckets": [
                {"key": "busy", "measures": [{"name": "AVG", "value": 10}]},
                {"key": "slow", "measures": [{"name": "AVG", "value": 900}]}
              ]}
            ]}
            """
        );

        var ranking = SeriesRanking.rank(facets, List.of(metric("HTTP_REQUESTS", "COUNT"), metric("HTTP_GATEWAY_LATENCY", "AVG")), 1, 1);

        assertThat(ranking.includes("HTTP_REQUESTS", List.of("busy"))).isTrue();
        assertThat(ranking.includes("HTTP_REQUESTS", List.of("slow"))).isFalse();
        assertThat(ranking.includes("HTTP_GATEWAY_LATENCY", List.of("slow"))).isTrue();
        assertThat(ranking.includes("HTTP_GATEWAY_LATENCY", List.of("busy"))).isFalse();
        assertThat(ranking.keysAt(0)).containsExactly("busy", "slow");
    }

    @Test
    void should_be_empty_when_no_value_occurs_over_the_range() {
        var ranking = SeriesRanking.rank(
            facets(
                """
                {"metrics": [{"metric": "HTTP_REQUESTS", "buckets": []}]}
                """
            ),
            REQUEST_COUNT,
            1,
            5
        );

        assertThat(ranking.isEmpty()).isTrue();
        assertThat(ranking.includes("HTTP_REQUESTS", List.of())).isFalse();
    }

    private static AnalyticsFacetMetricQuery metric(String name, String... measures) {
        return new AnalyticsFacetMetricQuery(name, List.of(measures), List.of(), List.of());
    }

    private static JsonNode facets(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid facets fixture", e);
        }
    }
}
