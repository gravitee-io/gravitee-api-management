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

import static io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys.BROKER_ID;
import static io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys.CLIENT_ID;
import static io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys.CONNECTION_DURATION_MS;
import static io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys.CONNECTION_STATUS;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import io.gravitee.repository.elasticsearch.AbstractAdapterTest;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeApiMetricsFindResponseAdapterTest extends AbstractAdapterTest {

    @Nested
    class EmptyResponses {

        @Test
        void returns_empty_optional_when_search_hits_is_null() {
            assertThat(NativeApiMetricsFindResponseAdapter.adapt(new SearchResponse())).isEmpty();
        }

        @Test
        void returns_empty_optional_when_search_hits_has_no_hits() {
            var response = new SearchResponse();
            var hits = new SearchHits();
            hits.setTotal(new TotalHits(0L));
            hits.setHits(List.of());
            response.setSearchHits(hits);

            assertThat(NativeApiMetricsFindResponseAdapter.adapt(response)).isEmpty();
        }

        @Test
        void returns_empty_optional_when_first_hit_has_null_source() {
            var response = new SearchResponse();
            var hits = new SearchHits();
            hits.setTotal(new TotalHits(1L));
            hits.setHits(List.of(new SearchHit()));
            response.setSearchHits(hits);

            assertThat(NativeApiMetricsFindResponseAdapter.adapt(response)).isEmpty();
        }

        @Test
        void returns_empty_optional_when_search_hits_list_is_null() {
            var response = new SearchResponse();
            var hits = new SearchHits();
            hits.setTotal(new TotalHits(0L));
            hits.setHits(null);
            response.setSearchHits(hits);

            assertThat(NativeApiMetricsFindResponseAdapter.adapt(response)).isEmpty();
        }
    }

    @Nested
    class ConnectedEvent {

        @Test
        void parses_top_level_identity_fields() {
            var metrics = NativeApiMetricsFindResponseAdapter.adapt(buildSearchHit("native-connection-connected.json")).orElseThrow();

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(metrics.getApiId()).isEqualTo("kafka-api-001");
                soft.assertThat(metrics.getRequestId()).isEqualTo("conn-connected-001");
                soft.assertThat(metrics.getTransactionId()).isEqualTo("conn-connected-001");
                soft.assertThat(metrics.getApplicationId()).isEqualTo("kafka-app-001");
                soft.assertThat(metrics.getPlanId()).isEqualTo("kafka-plan-001");
                soft.assertThat(metrics.getSubscriptionId()).isEqualTo("kafka-sub-A");
                soft.assertThat(metrics.getClientIdentifier()).isEqualTo("client-fp-A");
                soft.assertThat(metrics.getEntrypointId()).isEqualTo("native-kafka");
                soft.assertThat(metrics.getGateway()).isEqualTo("2c99d50d-d318-42d3-99d5-0dd31862d3d2");
                soft.assertThat(metrics.getRemoteAddress()).isEqualTo("192.168.1.10");
                soft.assertThat(metrics.getLocalAddress()).isEqualTo("10.0.2.208");
                soft.assertThat(metrics.getHost()).isEqualTo("broker.kafka.local:9092");
                soft.assertThat(metrics.getTimestamp()).isEqualTo("2026-04-29T10:00:00.000Z");
            });
        }

        @Test
        void leaves_error_fields_null_for_a_successful_connection() {
            var metrics = NativeApiMetricsFindResponseAdapter.adapt(buildSearchHit("native-connection-connected.json")).orElseThrow();

            assertThat(metrics.getErrorKey()).isNull();
            assertThat(metrics.getMessage()).isNull();
        }

        @Test
        void exposes_only_client_id_broker_id_and_status_in_additional_metrics() {
            var metrics = NativeApiMetricsFindResponseAdapter.adapt(buildSearchHit("native-connection-connected.json")).orElseThrow();

            assertThat(metrics.getAdditionalMetrics())
                .containsEntry(CLIENT_ID, "consumer-app-1-A")
                .containsEntry(BROKER_ID, "broker-1")
                .containsEntry(CONNECTION_STATUS, "CONNECTED")
                .doesNotContainKey(CONNECTION_DURATION_MS);
        }
    }

    @Nested
    class DisconnectedEvent {

        @Test
        void parses_top_level_error_fields_for_a_terminated_connection() {
            var metrics = NativeApiMetricsFindResponseAdapter.adapt(buildSearchHit("native-connection-disconnected.json")).orElseThrow();

            assertThat(metrics.getRequestId()).isEqualTo("conn-disconnect-001");
            assertThat(metrics.getErrorKey()).isEqualTo("KAFKA_CONSUMER_LEFT_GROUP");
            assertThat(metrics.getMessage()).isEqualTo("Consumer left the group gracefully after 30 minutes");
        }

        @Test
        void exposes_client_id_broker_id_status_and_duration_in_additional_metrics() {
            var metrics = NativeApiMetricsFindResponseAdapter.adapt(buildSearchHit("native-connection-disconnected.json")).orElseThrow();

            assertThat(metrics.getAdditionalMetrics())
                .containsEntry(CLIENT_ID, "consumer-app-1-A")
                .containsEntry(BROKER_ID, "broker-1")
                .containsEntry(CONNECTION_STATUS, "DISCONNECTED")
                .containsEntry(CONNECTION_DURATION_MS, 1800000);
        }
    }

    @Nested
    class ConnectionErrorEvent {

        @Test
        void parses_top_level_error_fields_for_a_failed_connection_attempt() {
            var metrics = NativeApiMetricsFindResponseAdapter.adapt(buildSearchHit("native-connection-error.json")).orElseThrow();

            assertThat(metrics.getRequestId()).isEqualTo("conn-error-001");
            assertThat(metrics.getErrorKey()).isEqualTo("KAFKA_AUTH_FAILED");
            assertThat(metrics.getMessage()).isEqualTo("SASL authentication failed: Invalid credentials");
        }

        @Test
        void omits_broker_id_and_duration_when_connection_never_established() {
            var metrics = NativeApiMetricsFindResponseAdapter.adapt(buildSearchHit("native-connection-error.json")).orElseThrow();

            assertThat(metrics.getAdditionalMetrics())
                .containsEntry(CLIENT_ID, "consumer-app-1-B")
                .containsEntry(CONNECTION_STATUS, "CONNECTION_ERROR")
                .doesNotContainKey(BROKER_ID)
                .doesNotContainKey(CONNECTION_DURATION_MS);
        }
    }
}
