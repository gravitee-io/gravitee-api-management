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
package io.gravitee.gamma.rest.resources.observability.analytics.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsRequestDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_deserialize_facets_request_with_by_clause() throws Exception {
        var json = """
            {
              "filters": [],
              "by": ["MCP_PROXY_PROMPT"],
              "metrics": [{"name": "HTTP_REQUESTS", "measures": ["COUNT"]}],
              "limit": 5
            }
            """;

        var request = objectMapper.readValue(json, AnalyticsFacetsRequestDto.class);

        assertThat(request.by()).containsExactly("MCP_PROXY_PROMPT");
        assertThat(request.limit()).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = { "limit", "facetSize" })
    void should_deserialize_time_series_request_with_by_clause_and_limit(String limitField) throws Exception {
        var json = """
            {
              "interval": 60000,
              "by": ["API"],
              "metrics": [{"name": "HTTP_REQUESTS", "measures": ["COUNT"]}],
              "%s": 5
            }
            """.formatted(limitField);

        var request = objectMapper.readValue(json, AnalyticsTimeSeriesRequestDto.class);

        assertThat(request.by()).containsExactly("API");
        assertThat(request.seriesLimit()).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = { "{\"limit\": 5, \"facetSize\": 9}", "{\"facetSize\": 9, \"limit\": 5}" })
    void should_take_limit_over_facet_size_when_a_time_series_request_sends_both(String json) throws Exception {
        var request = objectMapper.readValue(json, AnalyticsTimeSeriesRequestDto.class);

        assertThat(request.seriesLimit()).isEqualTo(5);
    }
}
