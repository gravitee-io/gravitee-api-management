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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchRequestsCountResponseAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Aggregation buildAggregation(Map<String, Long> buckets) {
        Aggregation agg = new Aggregation();
        agg.setBuckets(
            buckets
                .entrySet()
                .stream()
                .map(bucket -> (JsonNode) objectMapper.createObjectNode().put("key", bucket.getKey()).put("doc_count", bucket.getValue()))
                .toList()
        );
        return agg;
    }

    @Test
    void should_return_empty_result_if_no_aggregation() {
        final SearchResponse searchResponse = new SearchResponse();

        assertThat(SearchRequestsCountResponseAdapter.adapt(searchResponse)).isEmpty();
    }

    @Test
    void should_return_empty_result_if_no_entrypoints_aggregation() {
        final SearchResponse searchResponse = new SearchResponse();
        searchResponse.setAggregations(Map.of());

        assertThat(SearchRequestsCountResponseAdapter.adapt(searchResponse)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideSearchDataWithRanges")
    void should_build_count_response_from_status_ranges(Map<String, Long> buckets, long expectedCount) {
        final SearchResponse searchResponse = new SearchResponse();
        Aggregation statusRangesAggregation = buildAggregation(buckets);
        Aggregation entrypointsAggregation = buildAggregation(Map.of("http-post", 7L));

        searchResponse.setAggregations(Map.of("all_apis_status_ranges", statusRangesAggregation, "entrypoints", entrypointsAggregation));

        assertThat(SearchRequestsCountResponseAdapter.adapt(searchResponse))
            .hasValueSatisfying(countAggregate -> {
                assertThat(countAggregate.getTotal()).isEqualTo(expectedCount);
            });
    }

    private static Stream<Arguments> provideSearchDataWithRanges() {
        return Stream.of(Arguments.of(Map.of("100.0-600.0", 14L), 14L), Arguments.of(Map.of(), 0L));
    }

    @ParameterizedTest
    @MethodSource("provideSearchDataWithEntryPoints")
    void should_build_search_requests_count_response(Map<String, Long> buckets) {
        final SearchResponse searchResponse = new SearchResponse();
        Aggregation statusRangesAggregation = buildAggregation(Map.of("100.0-600.0", 14L));
        Aggregation entrypointsAggregation = buildAggregation(buckets);

        searchResponse.setAggregations(Map.of("all_apis_status_ranges", statusRangesAggregation, "entrypoints", entrypointsAggregation));

        assertThat(SearchRequestsCountResponseAdapter.adapt(searchResponse))
            .hasValueSatisfying(countAggregate -> {
                assertThat(countAggregate.getCountBy()).containsAllEntriesOf(buckets);
            });
    }

    private static Stream<Arguments> provideSearchDataWithEntryPoints() {
        return Stream.of(
            Arguments.of(Map.of("http-get", 1L)),
            Arguments.of(Map.of()),
            Arguments.of(Map.of("http-get", 11L, "http-post", 200L, "websocket", 5L, "the-unknown-endpoint", 10000L))
        );
    }
}
