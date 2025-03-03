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

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchResponseStatusRangesQueryAdapter.STATUS_RANGES;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchResponseStatusRangesResponseAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_return_empty_result_if_no_aggregation() {
        final SearchResponse searchResponse = new SearchResponse();

        assertThat(SearchResponseStatusRangesResponseAdapter.adapt(searchResponse)).isEmpty();
    }

    @Test
    void should_return_empty_result_if_no_entrypoints_aggregation() {
        final SearchResponse searchResponse = new SearchResponse();
        searchResponse.setAggregations(Map.of());

        assertThat(SearchResponseStatusRangesResponseAdapter.adapt(searchResponse)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideSearchData")
    void should_build_search_requests_count_response(String[] statusRanges) {
        final SearchResponse searchResponse = new SearchResponse();
        final Aggregation aggregation = new Aggregation();
        searchResponse.setAggregations(Map.of(STATUS_RANGES, aggregation));

        aggregation.setBuckets(Arrays.stream(statusRanges).map(this::provideBucket).toList());

        assertThat(SearchResponseStatusRangesResponseAdapter.adapt(searchResponse))
            .hasValueSatisfying(response -> {
                assertThat(response.getStatusRangesCountByEntrypoint().keySet()).containsExactly("all");

                assertThat(response.getStatusRangesCountByEntrypoint().get("all").keySet()).containsExactlyInAnyOrder(statusRanges);
            });
    }

    private JsonNode provideBucket(String entrypoint) {
        var result = objectMapper.createObjectNode();
        result
            .put("key", entrypoint)
            .putObject("status_ranges")
            .putArray("buckets")
            .addObject()
            .put("key", "100.0-200.0")
            .put("doc_count", 1);
        return result;
    }

    private static Stream<Arguments> provideSearchData() {
        return Stream.of(
            Arguments.of((Object) new String[] {}),
            Arguments.of((Object) new String[] { "http-get" }),
            Arguments.of((Object) new String[] { "http-get", "http-post" })
        );
    }
}
