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
package io.gravitee.repository.elasticsearch.v4.log.adapter.message;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.repository.log.v4.model.message.MessageMetrics;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchMessageMetricsResponseAdapterTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_return_empty_list_when_hits_is_null() {
        // Arrange
        SearchHits hits = null;

        // Act
        List<MessageMetrics> result = SearchMessageMetricsResponseAdapter.adapt(hits);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_empty_list_when_hits_contain_no_elements() {
        // Arrange
        SearchHits hits = new SearchHits();

        // Act
        List<MessageMetrics> result = SearchMessageMetricsResponseAdapter.adapt(hits);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_map_hits_to_message_metrics() throws Exception {
        // Arrange
        JsonNode source1 = objectMapper.readTree(
            """
                {
                    "@timestamp": "2025-11-25T12:00:00Z",
                    "api-id": "api-1",
                    "count": 5
                }
            """
        );
        JsonNode source2 = objectMapper.readTree(
            """
                {
                    "@timestamp": "2025-11-25T13:00:00Z",
                    "api-id": "api-2",
                    "count": 7
                }
            """
        );

        SearchHit hit1 = new SearchHit();
        hit1.setSource(source1);

        SearchHit hit2 = new SearchHit();
        hit2.setSource(source2);

        SearchHits hits = new SearchHits();
        hits.setHits(List.of(hit1, hit2));

        // Act
        List<MessageMetrics> result = SearchMessageMetricsResponseAdapter.adapt(hits);

        // Assert
        assertThat(result).hasSize(2);

        assertThat(result)
            .first()
            .satisfies(metrics -> {
                assertThat(metrics.getTimestamp()).isEqualTo("2025-11-25T12:00:00Z");
                assertThat(metrics.getApiId()).isEqualTo("api-1");
                assertThat(metrics.getCount()).isEqualTo(5L);
            });

        MessageMetrics metrics2 = result.get(1);
        assertThat(metrics2.getTimestamp()).isEqualTo("2025-11-25T13:00:00Z");
        assertThat(metrics2.getApiId()).isEqualTo("api-2");
        assertThat(metrics2.getCount()).isEqualTo(7L);
    }

    @Test
    void should_handle_missing_fields_gracefully() throws Exception {
        // Arrange
        JsonNode source = objectMapper.readTree(
            """
                {}
            """
        );

        SearchHit hit = new SearchHit();
        hit.setSource(source);

        SearchHits hits = new SearchHits();
        hits.setHits(List.of(hit));

        // Act
        List<MessageMetrics> result = SearchMessageMetricsResponseAdapter.adapt(hits);

        // Assert
        assertThat(result)
            .hasSize(1)
            .first()
            .satisfies(metrics -> {
                assertThat(metrics.getTimestamp()).isNull();
                assertThat(metrics.getApiId()).isNull();
                assertThat(metrics.getCount()).isZero();
            });
    }

    @Test
    void should_map_all_message_metrics_fields() throws Exception {
        // Arrange
        JsonNode source = objectMapper.readTree(
            """
                {
                    "@timestamp": "2025-11-25T15:00:00Z",
                    "api-id": "api-4",
                    "api-name": "Test API",
                    "request-id": "req-123",
                    "client-identifier": "client-2",
                    "correlation-id": "corr-456",
                    "operation": "operation-B",
                    "connector-type": "HTTP",
                    "connector-id": "conn-789",
                    "gateway": "gateway-1",
                    "content-length": 1024,
                    "count": 15,
                    "error-count": 3,
                    "count-increment": 1,
                    "error-count-increment": 1,
                    "error": true,
                    "gateway-latency-ms": 50,
                    "custom": {
                        "key1": "value1",
                        "key2": "value2"
                    },
                    "additional-metrics": {
                        "metric1": "value1",
                        "metric2": 42
                    }
                }
            """
        );

        SearchHit hit = new SearchHit();
        hit.setSource(source);

        SearchHits hits = new SearchHits();
        hits.setHits(List.of(hit));

        // Act
        List<MessageMetrics> result = SearchMessageMetricsResponseAdapter.adapt(hits);

        // Assert
        assertThat(result)
            .hasSize(1)
            .first()
            .satisfies(metrics -> {
                assertThat(metrics.getTimestamp()).isEqualTo("2025-11-25T15:00:00Z");
                assertThat(metrics.getApiId()).isEqualTo("api-4");
                assertThat(metrics.getApiName()).isEqualTo("Test API");
                assertThat(metrics.getRequestId()).isEqualTo("req-123");
                assertThat(metrics.getClientIdentifier()).isEqualTo("client-2");
                assertThat(metrics.getCorrelationId()).isEqualTo("corr-456");
                assertThat(metrics.getOperation()).isEqualTo("operation-B");
                assertThat(metrics.getConnectorType()).isEqualTo("HTTP");
                assertThat(metrics.getConnectorId()).isEqualTo("conn-789");
                assertThat(metrics.getGateway()).isEqualTo("gateway-1");
                assertThat(metrics.getContentLength()).isEqualTo(1024L);
                assertThat(metrics.getCount()).isEqualTo(15L);
                assertThat(metrics.getErrorCount()).isEqualTo(3L);
                assertThat(metrics.getCountIncrement()).isEqualTo(1L);
                assertThat(metrics.getErrorCountIncrement()).isEqualTo(1L);
                assertThat(metrics.isError()).isTrue();
                assertThat(metrics.getGatewayLatencyMs()).isEqualTo(50L);
                assertThat(metrics.getCustom()).containsEntry("key1", "value1");
                assertThat(metrics.getCustom()).containsEntry("key2", "value2");
                assertThat(metrics.getAdditionalMetrics()).containsEntry("metric1", "value1");
                assertThat(metrics.getAdditionalMetrics()).containsEntry("metric2", 42);
            });
    }
}
