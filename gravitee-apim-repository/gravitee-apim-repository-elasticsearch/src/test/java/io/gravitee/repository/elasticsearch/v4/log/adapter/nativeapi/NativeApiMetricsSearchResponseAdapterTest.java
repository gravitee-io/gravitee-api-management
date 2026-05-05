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
package io.gravitee.repository.elasticsearch.v4.log.adapter.nativeapi;

import static io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys.CONNECTION_STATUS;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import java.util.List;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeApiMetricsSearchResponseAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returns_empty_list_response_when_no_hits() {
        var response = NativeApiMetricsSearchResponseAdapter.adapt(new SearchResponse());

        assertThat(response.data()).isEmpty();
        assertThat(response.total()).isZero();
    }

    @Test
    @SneakyThrows
    void adapt_additional_metrics_for_successful_connection() {
        var source = objectMapper.readTree(
            """
            {
              "@timestamp": "2026-01-01T00:00:00.000Z",
              "api-id": "api-1",
              "request-id": "request-42",
              "application-id": "app-1",
              "plan-id": "plan-1",
              "entrypoint-id": "native-kafka",
              "subscription-id": "sub-1",
              "remote-address": "10.0.0.1",
              "local-address": "10.0.0.2",
              "host": "broker.example.com",
              "additional-metrics": { "keyword_native-kafka_connection-status": "CONNECTED" }
            }
            """
        );
        var response = new SearchResponse();
        var hits = new SearchHits();
        var hit = new SearchHit();
        hit.setSource(source);
        hits.setHits(List.of(hit));
        hits.setTotal(new TotalHits(7L));
        response.setSearchHits(hits);

        var result = NativeApiMetricsSearchResponseAdapter.adapt(response);

        assertThat(result.total()).isEqualTo(7);
        assertThat(result.data()).hasSize(1);
        var first = result.data().getFirst();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(first.getApiId()).isEqualTo("api-1");
            soft.assertThat(first.getRequestId()).isEqualTo("request-42");
            soft.assertThat(first.getApplicationId()).isEqualTo("app-1");
            soft.assertThat(first.getPlanId()).isEqualTo("plan-1");
            soft.assertThat(first.getEntrypointId()).isEqualTo("native-kafka");
            soft.assertThat(first.getSubscriptionId()).isEqualTo("sub-1");
            soft.assertThat(first.getRemoteAddress()).isEqualTo("10.0.0.1");
            soft.assertThat(first.getLocalAddress()).isEqualTo("10.0.0.2");
            soft.assertThat(first.getHost()).isEqualTo("broker.example.com");
            soft.assertThat(first.getAdditionalMetrics()).containsEntry(CONNECTION_STATUS, "CONNECTED");
        });
    }
}
