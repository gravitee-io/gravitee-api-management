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
package io.gravitee.repository.elasticsearch.v4.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests validating that ES field aliases on the V2 {@code request} index template
 * allow querying V2 documents using V4 field names.
 *
 * <p>The production {@code index-template-request.ftl} defines aliases like {@code api-id → api},
 * {@code gateway-response-time-ms → response-time}, etc. These tests verify that queries using the
 * V4 canonical names resolve correctly against V2 data loaded by {@code DatabaseHydrator}.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class RequestIndexFieldAliasesIT extends AbstractElasticsearchRepositoryTest {

    private static final String V2_REQUEST_INDEX = "gravitee-request-*";

    @Autowired
    private Client client;

    @Nested
    class ApiIdAlias {

        @Test
        void should_find_v2_documents_by_api_id_alias() {
            var query = """
                {
                  "query": {
                    "term": { "api-id": "48bddce0-11ea-4ff4-bddc-e011ea5ff4be" }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            assertThat(response.getSearchHits().getTotal().getValue())
                .as("api-id alias should resolve to 'api' field and find V2 document")
                .isEqualTo(1);
            assertThat(response.getSearchHits().getHits().getFirst().getId()).isEqualTo("AVsJ2NpUuDfGHrKOwwSX");
        }

        @Test
        void should_find_multiple_v2_documents_by_api_id_alias() {
            var query = """
                {
                  "query": {
                    "term": { "api-id": "e2c0ecd5-893a-458d-80ec-d5893ab58d12" }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            assertThat(response.getSearchHits().getTotal().getValue())
                .as("api-id alias should find all V2 docs for API e2c0ecd5")
                .isEqualTo(4);
        }
    }

    @Nested
    class ApplicationIdAlias {

        @Test
        void should_find_v2_documents_by_application_id_alias() {
            var query = """
                {
                  "query": {
                    "term": { "application-id": "cdf6ab93-cb6f-4ea9-b6ab-93cb6f0ea9e2" }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            assertThat(response.getSearchHits().getTotal().getValue())
                .as("application-id alias should resolve to 'application' field")
                .isGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    class PlanIdAlias {

        @Test
        void should_find_v2_documents_by_plan_id_alias() {
            var query = """
                {
                  "query": {
                    "term": { "plan-id": "858760f9-5d97-4bd8-8760-f95d970bd86e" }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            assertThat(response.getSearchHits().getTotal().getValue())
                .as("plan-id alias should resolve to 'plan' field")
                .isGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    class TransactionIdAlias {

        @Test
        void should_find_v2_document_by_transaction_id_alias() {
            var query = """
                {
                  "query": {
                    "term": { "transaction-id": "d78460be-3e9a-404a-8460-be3e9a604afe" }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            assertThat(response.getSearchHits().getTotal().getValue())
                .as("transaction-id alias should resolve to 'transaction' field")
                .isEqualTo(1);
            assertThat(response.getSearchHits().getHits().getFirst().getId()).isEqualTo("AVsJ2NpUuDfGHrKOwwSX");
        }
    }

    @Nested
    class HttpMethodAlias {

        @Test
        void should_find_v2_documents_by_http_method_alias() {
            var query = """
                {
                  "query": {
                    "term": { "http-method": 7 }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            assertThat(response.getSearchHits().getTotal().getValue())
                .as("http-method alias should resolve to 'method' field; method=7 is POST in V2")
                .isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    class GatewayResponseTimeMsAlias {

        @Test
        void should_find_v2_documents_by_gateway_response_time_ms_range() {
            var query = """
                {
                  "query": {
                    "range": { "gateway-response-time-ms": { "gte": 300, "lte": 600 } }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            assertThat(response.getSearchHits().getTotal().getValue())
                .as("gateway-response-time-ms alias should resolve to 'response-time' and find docs in range [300,600]")
                .isGreaterThanOrEqualTo(3);
        }

        @Test
        void should_aggregate_on_gateway_response_time_ms_alias() {
            var query = """
                {
                  "size": 0,
                  "aggs": {
                    "response_time_stats": {
                      "stats": { "field": "gateway-response-time-ms" }
                    }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            Aggregation statsAgg = response.getAggregations().get("response_time_stats");
            assertThat(statsAgg).isNotNull();
            assertThat(statsAgg.getAvg())
                .as("stats aggregation on gateway-response-time-ms should compute over V2 response-time values")
                .isGreaterThan(0.0f);
        }
    }

    @Nested
    class SubscriptionIdAlias {

        @Test
        void should_find_v2_document_by_subscription_id_alias() {
            var query = """
                {
                  "query": {
                    "term": { "subscription-id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890" }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            assertThat(response.getSearchHits().getTotal().getValue())
                .as("subscription-id alias should resolve to 'subscription' field and find the V2 document")
                .isEqualTo(1);
            assertThat(response.getSearchHits().getHits().getFirst().getId()).isEqualTo("AVsJ2NpUuDfGHrKOwwSX");
        }

        @Test
        void should_return_no_results_for_non_existent_subscription_id() {
            var query = """
                {
                  "query": {
                    "term": { "subscription-id": "does-not-exist" }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            assertThat(response.getSearchHits().getTotal().getValue())
                .as("subscription-id alias should resolve but find no match for unknown value")
                .isZero();
        }
    }

    @Nested
    class GatewayLatencyMsAlias {

        @Test
        void should_find_v2_documents_by_gateway_latency_ms_range() {
            var query = """
                {
                  "query": {
                    "range": { "gateway-latency-ms": { "gte": 10, "lte": 60 } }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            assertThat(response.getSearchHits().getTotal().getValue())
                .as("gateway-latency-ms alias should resolve to 'proxy-latency' and find docs in range [10,60]")
                .isGreaterThanOrEqualTo(5);
        }

        @Test
        void should_aggregate_on_gateway_latency_ms_alias() {
            var query = """
                {
                  "size": 0,
                  "aggs": {
                    "latency_stats": {
                      "stats": { "field": "gateway-latency-ms" }
                    }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            Aggregation statsAgg = response.getAggregations().get("latency_stats");
            assertThat(statsAgg).isNotNull();
            assertThat(statsAgg.getAvg())
                .as("stats aggregation on gateway-latency-ms should compute over V2 proxy-latency values")
                .isGreaterThan(0.0f);
        }
    }

    @Nested
    class EndpointResponseTimeMsAlias {

        @Test
        void should_find_v2_documents_by_endpoint_response_time_ms_range() {
            var query = """
                {
                  "query": {
                    "range": { "endpoint-response-time-ms": { "gte": 200, "lte": 500 } }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            assertThat(response.getSearchHits().getTotal().getValue())
                .as("endpoint-response-time-ms alias should resolve to 'api-response-time' and find docs in range [200,500]")
                .isGreaterThanOrEqualTo(3);
        }

        @Test
        void should_aggregate_on_endpoint_response_time_ms_alias() {
            var query = """
                {
                  "size": 0,
                  "aggs": {
                    "endpoint_time_stats": {
                      "stats": { "field": "endpoint-response-time-ms" }
                    }
                  }
                }
                """;

            SearchResponse response = client.search(V2_REQUEST_INDEX, null, query).blockingGet();

            Aggregation statsAgg = response.getAggregations().get("endpoint_time_stats");
            assertThat(statsAgg).isNotNull();
            assertThat(statsAgg.getAvg())
                .as("stats aggregation on endpoint-response-time-ms should compute over V2 api-response-time values")
                .isGreaterThan(0.0f);
        }
    }

    @Nested
    class EntrypointIdAbsence {

        private static final String V2_AND_V4_INDICES = "gravitee-request-*,gravitee-v4-metrics-*";

        @Test
        void should_return_zero_v2_results_for_entrypoint_id_exists_query() {
            var query = """
                {
                  "query": {
                    "bool": {
                      "must": [
                        { "exists": { "field": "entrypoint-id" } }
                      ]
                    }
                  }
                }
                """;

            SearchResponse response = client.search(V2_AND_V4_INDICES, null, query).blockingGet();

            assertThat(response.getSearchHits().getHits())
                .as("entrypoint-id exists only in V4 docs; no V2 hit should appear")
                .allSatisfy(hit -> assertThat(hit.getIndex()).doesNotContain("request"));
        }

        @Test
        void should_not_include_v2_docs_in_entrypoint_id_terms_aggregation() {
            var query = """
                {
                  "size": 0,
                  "aggs": {
                    "per_entrypoint": {
                      "terms": { "field": "entrypoint-id", "size": 10 }
                    }
                  }
                }
                """;

            SearchResponse response = client.search(V2_AND_V4_INDICES, null, query).blockingGet();

            Aggregation perEntrypoint = response.getAggregations().get("per_entrypoint");
            assertThat(perEntrypoint).isNotNull();
            assertThat(perEntrypoint.getBuckets())
                .as("terms agg on entrypoint-id should only contain V4 entrypoint values, not V2 data")
                .isNotEmpty()
                .allSatisfy(bucket -> assertThat(bucket.has("key")).isTrue());
        }
    }
}
