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

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageConnectionDurationQueryAdapter.AVG_ENDED_REQUEST_DURATION_MS;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageConnectionDurationQueryAdapter.ENTRYPOINTS_AGG;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import java.util.Collections;
import java.util.Map;
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
class SearchAverageConnectionDurationResponseAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_return_empty_result_if_no_aggregation() {
        final SearchResponse searchResponse = new SearchResponse();

        assertThat(SearchAverageConnectionDurationResponseAdapter.adapt(searchResponse)).isEmpty();
    }

    @Test
    void should_return_empty_result_if_no_entrypoints_aggregation() {
        final SearchResponse searchResponse = new SearchResponse();
        searchResponse.setAggregations(Map.of());

        assertThat(SearchAverageConnectionDurationResponseAdapter.adapt(searchResponse)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideSearchData")
    void should_build_search_average_connection_duration(Map<String, Double> buckets, double expectedCount) {
        final SearchResponse searchResponse = new SearchResponse();
        final Aggregation aggregation = new Aggregation();
        searchResponse.setAggregations(Map.of(ENTRYPOINTS_AGG, aggregation));

        aggregation.setBuckets(
            buckets
                .entrySet()
                .stream()
                .map(bucket ->
                    (JsonNode) objectMapper
                        .createObjectNode()
                        .put("key", bucket.getKey())
                        .set(AVG_ENDED_REQUEST_DURATION_MS, objectMapper.createObjectNode().put("value", bucket.getValue()))
                )
                .toList()
        );

        assertThat(SearchAverageConnectionDurationResponseAdapter.adapt(searchResponse)).hasValueSatisfying(countAggregate -> {
            assertThat(countAggregate.getAverage()).isEqualTo(expectedCount);
            assertThat(countAggregate.getAverageBy()).containsAllEntriesOf(buckets);
        });
    }

    @Test
    void should_return_empty_result_if_bucket_is_empty() {
        final SearchResponse searchResponse = new SearchResponse();
        final Aggregation aggregation = new Aggregation();
        searchResponse.setAggregations(Map.of(ENTRYPOINTS_AGG, aggregation));
        aggregation.setBuckets(Collections.emptyList());

        assertThat(SearchAverageConnectionDurationResponseAdapter.adapt(searchResponse)).isEmpty();
    }

    private static Stream<Arguments> provideSearchData() {
        return Stream.of(
            Arguments.of(Map.of("http-get", 1.0), 1.0),
            Arguments.of(Map.of("sse", 0.0), 0.0),
            Arguments.of(Map.of("http-get", 11.0, "http-post", 200.0, "websocket", 5.0, "the-unknown-endpoint", 10000.0), 2554.0)
        );
    }
}
