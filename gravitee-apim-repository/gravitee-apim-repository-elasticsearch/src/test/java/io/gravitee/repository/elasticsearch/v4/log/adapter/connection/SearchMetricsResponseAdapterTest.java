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
package io.gravitee.repository.elasticsearch.v4.log.adapter.connection;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import io.gravitee.repository.elasticsearch.AbstractAdapterTest;
import io.gravitee.repository.log.v4.model.connection.Metrics;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchMetricsResponseAdapterTest extends AbstractAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    private SearchResponse buildSearchHitWithIndex(String fileName, String index) {
        final SearchResponse searchResponse = new SearchResponse();
        final SearchHits searchHits = new SearchHits();
        final SearchHit searchHit = new SearchHit();

        final var jsonNode = objectMapper.readTree(loadFile("/hits/" + fileName));
        searchHit.setIndex(index);
        searchHit.setId("log-id");
        searchHit.setSource(jsonNode);
        searchHits.setHits(List.of(searchHit));
        searchHits.setTotal(new TotalHits(1L));
        searchResponse.setSearchHits(searchHits);
        return searchResponse;
    }

    @Test
    void should_return_empty_when_no_hit() {
        var result = SearchMetricsResponseAdapter.adapt(new SearchResponse());

        assertThat(result.total()).isEqualTo(0L);
        assertThat(result.data()).isEmpty();
    }

    @Nested
    class ApiProductId {

        @Test
        void should_map_api_product_id_from_v4_metrics_hit() {
            var searchResponse = buildSearchHit("api-proxy-v4-metrics-with-api-product.json");

            var result = SearchMetricsResponseAdapter.adapt(searchResponse);

            assertThat(result.data()).hasSize(1);
            Metrics metrics = result.data().getFirst();
            assertThat(metrics.getApiProductId()).isEqualTo("product-abc-123");
        }

        @Test
        void should_map_api_product_id_as_null_when_not_present_in_v4_metrics_hit() {
            var searchResponse = buildSearchHit("api-proxy-v4-metrics.json");

            var result = SearchMetricsResponseAdapter.adapt(searchResponse);

            assertThat(result.data()).hasSize(1);
            Metrics metrics = result.data().getFirst();
            assertThat(metrics.getApiProductId()).isNull();
        }

        @Test
        void should_not_map_api_product_id_for_v2_request_index_hit() {
            // The v2 request index path is selected when the ES index name contains "request".
            // API_PRODUCT_ID has no v2 counterpart, so the field must be null.
            var searchResponse = buildSearchHitWithIndex("api-proxy-v4-metrics.json", "gravitee-request-2025.01.01");

            var result = SearchMetricsResponseAdapter.adapt(searchResponse);

            assertThat(result.data()).hasSize(1);
            Metrics metrics = result.data().getFirst();
            assertThat(metrics.getApiProductId()).isNull();
        }
    }

    @Nested
    class BasicFieldMapping {

        @Test
        void should_map_core_fields_from_v4_metrics_hit() {
            var searchResponse = buildSearchHit("api-proxy-v4-metrics.json");

            var result = SearchMetricsResponseAdapter.adapt(searchResponse);

            assertThat(result.total()).isEqualTo(1L);
            Metrics metrics = result.data().getFirst();
            assertThat(metrics.getApiId()).isEqualTo("2ebe3deb-1859-4d5b-be3d-eb1859dd5b16");
            assertThat(metrics.getRequestId()).isEqualTo("39107cc9-b8bf-4f16-907c-c9b8bf8f16fb");
            assertThat(metrics.getTransactionId()).isEqualTo("39107cc9-b8bf-4f16-907c-c9b8bf8f16fb");
            assertThat(metrics.getClientIdentifier()).isEqualTo("b6cf5bd6081fb775fc3f89927e582d7e945f955c35b73a07879d75bb4ea96a01");
            assertThat(metrics.getStatus()).isEqualTo(202);
            assertThat(metrics.getGatewayResponseTime()).isEqualTo(19);
            assertThat(metrics.getEndpoint()).isEqualTo("https://api.gravitee.io/echo");
            // apiProductId is absent in this fixture — must map to null, not throw
            assertThat(metrics.getApiProductId()).isNull();
        }
    }
}
