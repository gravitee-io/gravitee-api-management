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

    @Test
    void should_deserialize_time_series_request_with_by_clause() throws Exception {
        var json = """
            {
              "interval": 60000,
              "by": ["API"],
              "metrics": [{"name": "HTTP_REQUESTS", "measures": ["COUNT"]}],
              "facetSize": 5
            }
            """;

        var request = objectMapper.readValue(json, AnalyticsTimeSeriesRequestDto.class);

        assertThat(request.by()).containsExactly("API");
        assertThat(request.facetSize()).isEqualTo(5);
    }
}
