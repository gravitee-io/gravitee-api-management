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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField.Root;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricLastSeen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricDiscoveryAdapterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    class Candidates {

        @ParameterizedTest
        @CsvSource(
            {
                "keyword_tier,keyword",
                "long_count,long",
                "int_count,integer",
                "double_score,double",
                "bool_hit,boolean",
                "string_model,text",
                "json_signals,text",
            }
        )
        void should_keep_a_key_stored_under_the_type_its_prefix_declares(String rawKey, String esType) {
            var candidates = AdditionalMetricDiscoveryAdapter.candidates(Map.of("additional-metrics." + rawKey, Set.of(esType)));

            assertThat(candidates.names()).containsExactly("additional-metrics." + rawKey);
            assertThat(candidates.truncated()).isFalse();
        }

        @Test
        void should_drop_a_key_stored_under_another_type_than_its_prefix_declares() {
            var candidates = AdditionalMetricDiscoveryAdapter.candidates(
                Map.of(
                    "additional-metrics.long_written-as-float",
                    Set.of("float"),
                    "additional-metrics.long_mixed",
                    Set.of("long", "float"),
                    "additional-metrics.int_count",
                    Set.of("long")
                )
            );

            assertThat(candidates.names()).isEmpty();
        }

        @Test
        void should_drop_objects_unknown_prefixes_and_fields_outside_both_maps_and_keep_the_typed_custom_form() {
            var candidates = AdditionalMetricDiscoveryAdapter.candidates(
                Map.of(
                    "additional-metrics",
                    Set.of("object"),
                    "additional-metrics.long_a",
                    Set.of("object"),
                    "additional-metrics.long_a.b",
                    Set.of("long"),
                    "additional-metrics.double_custom_basket-value",
                    Set.of("double"),
                    "additional-metrics.keyword_custom_tier",
                    Set.of("keyword"),
                    "additional-metrics.foo_bar",
                    Set.of("text"),
                    "additional-metrics.foo_bar.keyword",
                    Set.of("keyword"),
                    "api-id",
                    Set.of("keyword")
                )
            );

            assertThat(candidates.names()).containsExactly(
                "additional-metrics.double_custom_basket-value",
                "additional-metrics.keyword_custom_tier",
                "additional-metrics.long_a.b"
            );
        }

        @Test
        void should_keep_a_custom_key_stored_as_a_keyword_only() {
            var candidates = AdditionalMetricDiscoveryAdapter.candidates(
                Map.of(
                    "custom.fraud-tier",
                    Set.of("keyword"),
                    "custom.Merchant ID",
                    Set.of("keyword"),
                    "custom.a",
                    Set.of("object"),
                    "custom.a.b",
                    Set.of("keyword"),
                    "custom.signup-date",
                    Set.of("date"),
                    "custom.mixed",
                    Set.of("keyword", "date"),
                    "custom.tier*",
                    Set.of("keyword"),
                    "custom",
                    Set.of("object")
                )
            );

            assertThat(candidates.names()).containsExactly("custom.Merchant ID", "custom.a.b", "custom.fraud-tier");
        }

        @Test
        void should_drop_a_key_holding_a_star_which_an_exists_query_reads_as_a_pattern() {
            var candidates = AdditionalMetricDiscoveryAdapter.candidates(
                Map.of("additional-metrics.keyword_a*", Set.of("keyword"), "custom.*", Set.of("keyword"))
            );

            assertThat(candidates.names()).isEmpty();
        }

        @Test
        void should_keep_a_custom_key_apart_from_the_additional_key_of_the_same_spelling() {
            var candidates = AdditionalMetricDiscoveryAdapter.candidates(
                Map.of("additional-metrics.keyword_tier", Set.of("keyword"), "custom.keyword_tier", Set.of("keyword"))
            );

            assertThat(candidates.names()).containsExactly("additional-metrics.keyword_tier", "custom.keyword_tier");
        }

        /** E2E finding A5: an environment's late key used to be cut by another environment's keys sorting first. */
        @Test
        void should_probe_every_key_of_the_mapping_below_the_safety_cap_without_saying_it_is_truncated() {
            var typesByField = keys("additional-metrics.long_key-%04d", "long", 1_500);
            typesByField.putAll(keys("custom.key-%04d", "keyword", 1_500));
            typesByField.put("custom.zzz-late", Set.of("keyword"));

            var candidates = AdditionalMetricDiscoveryAdapter.candidates(typesByField);

            assertThat(candidates.names()).hasSize(3_001).contains("custom.zzz-late");
            assertThat(candidates.truncated()).isFalse();
        }

        /** A deadline cuts the probes batch by batch: alternating the maps keeps one from starving the other. */
        @Test
        void should_alternate_the_two_maps_each_sorted_by_wire_name() {
            var candidates = AdditionalMetricDiscoveryAdapter.candidates(
                Map.of(
                    "additional-metrics.long_c",
                    Set.of("long"),
                    "additional-metrics.long_a",
                    Set.of("long"),
                    "additional-metrics.long_b",
                    Set.of("long"),
                    "custom.y",
                    Set.of("keyword"),
                    "custom.x",
                    Set.of("keyword")
                )
            );

            assertThat(candidates.names()).containsExactly(
                "additional-metrics.long_a",
                "custom.x",
                "additional-metrics.long_b",
                "custom.y",
                "additional-metrics.long_c"
            );
        }

        @Test
        void should_probe_every_key_without_saying_truncated_at_exactly_the_safety_cap() {
            var typesByField = keys("additional-metrics.long_key-%05d", "long", AdditionalMetricDiscoveryAdapter.MAX_PROBED_KEYS);

            var candidates = AdditionalMetricDiscoveryAdapter.candidates(typesByField);

            assertThat(candidates.names()).hasSize(AdditionalMetricDiscoveryAdapter.MAX_PROBED_KEYS);
            assertThat(candidates.truncated()).isFalse();
        }

        @Test
        void should_probe_at_most_the_safety_cap_and_say_so_keeping_both_maps() {
            var typesByField = keys("additional-metrics.long_key-%05d", "long", AdditionalMetricDiscoveryAdapter.MAX_PROBED_KEYS);
            typesByField.putAll(keys("custom.key-%05d", "keyword", 10));

            var candidates = AdditionalMetricDiscoveryAdapter.candidates(typesByField);

            assertThat(candidates.names()).hasSize(AdditionalMetricDiscoveryAdapter.MAX_PROBED_KEYS);
            assertThat(candidates.names())
                .filteredOn(name -> name.startsWith("custom."))
                .hasSize(10);
            assertThat(candidates.truncated()).isTrue();
        }

        private static Map<String, Set<String>> keys(String pattern, String esType, int count) {
            var typesByField = new HashMap<String, Set<String>>();
            IntStream.range(0, count).forEach(i -> typesByField.put(pattern.formatted(i), Set.of(esType)));
            return typesByField;
        }
    }

    @Nested
    class Query {

        @Test
        void should_count_nothing_and_probe_each_key_on_the_environment_documents_of_the_window() {
            var from = Instant.parse("2026-09-29T00:00:00Z");
            var to = Instant.parse("2026-10-29T00:00:00Z");

            var query = new JsonObject(
                AdditionalMetricDiscoveryAdapter.query(
                    "env-1",
                    from,
                    to,
                    List.of(
                        "additional-metrics.long_failover_count",
                        "additional-metrics.keyword_mcp-proxy_tools/call",
                        "custom.fraud-tier"
                    ),
                    Duration.ofMillis(1500)
                )
            );

            assertThat(query.getInteger("size")).isZero();
            assertThat(query.getBoolean("track_total_hits")).isFalse();
            assertThat(query.getString("timeout")).isEqualTo("1500ms");
            assertThat(query.getJsonObject("query").getJsonObject("bool").getJsonArray("filter")).isEqualTo(
                JsonArray.of(
                    JsonObject.of("term", JsonObject.of("env-id", "env-1")),
                    JsonObject.of("range", JsonObject.of("@timestamp", JsonObject.of("gte", from.toEpochMilli(), "lte", to.toEpochMilli())))
                )
            );
            assertThat(query.getJsonObject("aggs")).isEqualTo(
                JsonObject.of(
                    "keys",
                    JsonObject.of(
                        "filters",
                        JsonObject.of(
                            "filters",
                            JsonArray.of(
                                JsonObject.of("exists", JsonObject.of("field", "additional-metrics.long_failover_count")),
                                JsonObject.of("exists", JsonObject.of("field", "additional-metrics.keyword_mcp-proxy_tools/call")),
                                JsonObject.of("exists", JsonObject.of("field", "custom.fraud-tier"))
                            )
                        ),
                        "aggs",
                        JsonObject.of("last_seen", JsonObject.of("max", JsonObject.of("field", "@timestamp")))
                    )
                )
            );
        }
    }

    @Nested
    class Response {

        @Test
        void should_keep_the_keys_carried_by_a_document_with_the_time_of_the_latest() throws Exception {
            var response = MAPPER.readValue(
                """
                {
                  "timed_out": false,
                  "aggregations": {
                    "keys": {
                      "buckets": [
                        { "doc_count": 2, "last_seen": { "value": 1793174700000, "value_as_string": "2026-10-28T08:05:00.000Z" } },
                        { "doc_count": 0, "last_seen": { "value": null } },
                        { "doc_count": 1, "last_seen": { "value": 1793174400000, "value_as_string": "2026-10-28T08:00:00.000Z" } },
                        { "doc_count": 3, "last_seen": { "value": 1793174100000, "value_as_string": "2026-10-28T07:55:00.000Z" } }
                      ]
                    }
                  }
                }
                """,
                SearchResponse.class
            );

            var found = AdditionalMetricDiscoveryAdapter.found(
                List.of(
                    "additional-metrics.long_failover_count",
                    "additional-metrics.keyword_never",
                    "additional-metrics.bool_cache_hit",
                    "custom.fraud-tier"
                ),
                response
            );

            assertThat(found).containsExactly(
                new AdditionalMetricLastSeen("long_failover_count", Instant.parse("2026-10-28T08:05:00Z")),
                new AdditionalMetricLastSeen("bool_cache_hit", Instant.parse("2026-10-28T08:00:00Z")),
                new AdditionalMetricLastSeen(Root.CUSTOM, "fraud-tier", Instant.parse("2026-10-28T07:55:00Z"))
            );
        }

        @Test
        void should_find_nothing_in_a_response_without_aggregations() {
            assertThat(
                AdditionalMetricDiscoveryAdapter.found(List.of("additional-metrics.long_failover_count"), new SearchResponse())
            ).isEmpty();
        }
    }

    @Test
    void should_order_most_recently_seen_first_then_by_wire_name() {
        var t1 = Instant.parse("2026-10-28T08:00:00Z");
        var t2 = Instant.parse("2026-10-28T09:00:00Z");

        var ordered = AdditionalMetricDiscoveryAdapter.mostRecentFirst(
            List.of(
                new AdditionalMetricLastSeen("long_b", t1),
                new AdditionalMetricLastSeen("long_c", t2),
                new AdditionalMetricLastSeen("long_a", t1),
                new AdditionalMetricLastSeen(Root.CUSTOM, "a", t1)
            )
        );

        assertThat(ordered)
            .extracting(AdditionalMetricLastSeen::name)
            .containsExactly("additional-metrics.long_c", "additional-metrics.long_a", "additional-metrics.long_b", "custom.a");
    }
}
