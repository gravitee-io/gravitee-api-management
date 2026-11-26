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
package io.gravitee.repository.elasticsearch.v4.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.engine.api.query.TrafficScope;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import io.gravitee.repository.log.v4.model.analytics.FilterValuesQuery;
import io.gravitee.repository.log.v4.model.analytics.FilterValuesResult;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * The values a filter offers for what the user typed: those starting with it, whatever their case, one rule for every
 * keyword field the values are read from. Over {@code freemarker-v4-analytics/v4-metrics.ftl}: the
 * "filter-values-prefix-api-001" block (custom key {@code card-tier}), the "additional-metrics-api-001" block (custom
 * and additional fraud tiers), the "f1608475-…" calls (entrypoints, error keys) and the "mcp-api-001" calls (MCP method).
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics")
class AnalyticsElasticsearchRepositoryFilterValuesTest extends AbstractElasticsearchRepositoryTest {

    private static final String CARD_TIER_API = "filter-values-prefix-api-001";
    private static final String FRAUD_TIER_API = "additional-metrics-api-001";
    private static final String MESSAGE_API = "f1608475-dd77-4603-a084-75dd775603e9";
    private static final String MCP_API = "mcp-api-001";

    private final QueryContext queryContext = new QueryContext("org#1", "env#1");

    @Autowired
    private AnalyticsElasticsearchRepository analyticsRepository;

    @Nested
    class Custom_key {

        private static final String FIELD = "custom.card-tier";

        @Test
        void should_offer_the_values_starting_with_what_was_typed_in_their_order() {
            assertThat(values(FIELD, CARD_TIER_API, "gol")).containsExactly("gold", "golden");
        }

        @Test
        void should_match_a_stored_value_whatever_the_case_typed() {
            assertThat(values(FIELD, CARD_TIER_API, "plat")).containsExactly("Platinum");
            assertThat(values(FIELD, CARD_TIER_API, "PLAT")).containsExactly("Platinum");
        }

        @Test
        void should_still_offer_a_value_typed_in_full() {
            assertThat(values(FIELD, CARD_TIER_API, "gold")).containsExactly("gold", "golden");
            assertThat(values(FIELD, CARD_TIER_API, "golden")).containsExactly("golden");
        }

        @Test
        void should_count_only_the_values_starting_with_what_was_typed() {
            assertThat(search(query(FIELD, CARD_TIER_API, "gol").size(10)).totalCount()).isEqualTo(2);
        }

        @Test
        void should_offer_no_value_containing_what_was_typed_elsewhere_than_at_its_start() {
            assertThat(values(FIELD, CARD_TIER_API, "old")).isEmpty();
            assertThat(values(FIELD, CARD_TIER_API, "tinum")).isEmpty();
        }

        @Test
        void should_offer_no_value_longer_than_the_stored_one() {
            assertThat(values(FIELD, CARD_TIER_API, "goldenrod")).isEmpty();
        }

        @Test
        void should_read_a_wildcard_typed_as_a_literal_character() {
            assertThat(values(FIELD, CARD_TIER_API, "*")).isEmpty();
            assertThat(values(FIELD, CARD_TIER_API, "go*")).isEmpty();
        }

        @Test
        void should_offer_every_value_when_nothing_was_typed() {
            assertThat(values(FIELD, CARD_TIER_API, null)).containsExactly("Platinum", "gold", "golden");
            assertThat(values(FIELD, CARD_TIER_API, " ")).containsExactly("Platinum", "gold", "golden");
        }

        @Test
        void should_keep_the_values_of_the_custom_root_apart_from_the_additional_metrics() {
            // "spoofed" is stored under custom.keyword_fraud-check_tier, never under the additional metric of that name.
            assertThat(values("additional-metrics.keyword_fraud-check_tier", FRAUD_TIER_API, "sp")).isEmpty();
            assertThat(values("custom.fraud-tier", FRAUD_TIER_API, "GO")).containsExactly("gold");
        }
    }

    @Nested
    class Keyword_additional_metric {

        private static final String FIELD = "additional-metrics.keyword_fraud-check_tier";

        @Test
        void should_offer_the_values_starting_with_what_was_typed_whatever_its_case() {
            assertThat(values(FIELD, FRAUD_TIER_API, "HI")).containsExactly("high");
        }

        @Test
        void should_offer_no_value_containing_what_was_typed_elsewhere_than_at_its_start() {
            assertThat(values(FIELD, FRAUD_TIER_API, "igh")).isEmpty();
        }
    }

    @Nested
    class Built_in_keyword {

        @Test
        void should_offer_the_entrypoints_starting_with_what_was_typed_whatever_its_case() {
            assertThat(values("entrypoint-id", MESSAGE_API, "WEB")).containsExactly("webhook", "websocket");
        }

        @Test
        void should_match_a_value_stored_in_upper_case_from_a_lower_case_start() {
            assertThat(values("error-key", MESSAGE_API, "no_end")).containsExactly("NO_ENDPOINT_FOUND");
        }

        @Test
        void should_offer_no_entrypoint_containing_what_was_typed_elsewhere_than_at_its_start() {
            assertThat(values("entrypoint-id", MESSAGE_API, "socket")).isEmpty();
        }

        @Test
        void should_still_offer_an_entrypoint_typed_in_full() {
            assertThat(values("entrypoint-id", MESSAGE_API, "sse")).containsExactly("sse");
        }

        @Test
        void should_offer_the_mcp_methods_starting_with_what_was_typed_whatever_its_case() {
            assertThat(values("additional-metrics.keyword_mcp-proxy_method", MCP_API, "TOOLS")).containsExactly("tools/call");
            assertThat(values("additional-metrics.keyword_mcp-proxy_method", MCP_API, "call")).isEmpty();
        }
    }

    @Nested
    class Ip_field {

        @Test
        void should_offer_a_full_address_typed() {
            assertThat(values("remote-address", MESSAGE_API, "127.0.0.1")).containsExactly("127.0.0.1");
        }

        @Test
        void should_offer_no_value_for_the_start_of_an_address() {
            assertThat(values("remote-address", MESSAGE_API, "127")).isEmpty();
            assertThat(values("remote-address", MESSAGE_API, "127.0.0.")).isEmpty();
        }

        @Test
        void should_offer_every_address_when_nothing_was_typed() {
            assertThat(values("remote-address", MESSAGE_API, null)).containsExactly("127.0.0.1");
        }
    }

    /** The connector of a message call is stamped on the message documents only, never on the request ones. */
    @Nested
    class Message_connector {

        private static final String HTTP_GET_API = "4a6895d5-a1bc-4041-a895-d5a1bce041ae";

        @Test
        void should_offer_the_connectors_of_the_message_calls() {
            assertThat(values("connector-id", MESSAGE_API, null)).containsExactly("http-get", "kafka", "mqtt5", "sse", "websocket");
            assertThat(values("connector-type", MESSAGE_API, null)).containsExactly("endpoint", "entrypoint");
        }

        @Test
        void should_offer_the_connectors_starting_with_what_was_typed() {
            assertThat(values("connector-id", MESSAGE_API, "WEB")).containsExactly("websocket");
            assertThat(values("connector-id", MESSAGE_API, "socket")).isEmpty();
        }

        @Test
        void should_offer_only_the_connectors_of_the_readable_apis() {
            assertThat(values("connector-id", HTTP_GET_API, null)).containsExactly("http-get", "mock");
            assertThat(search(query("connector-id", null, null).apiIds(Set.of()).size(10)).values()).isEmpty();
        }

        @Test
        void should_offer_only_the_connectors_of_the_calls_in_the_period() {
            var yesterday = TimeProvider.now().truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(1));

            var result = search(
                query("connector-id", MESSAGE_API, null)
                    .from(yesterday.toEpochMilli())
                    .to(yesterday.plus(Duration.ofDays(1)).minusMillis(1).toEpochMilli())
                    .size(10)
            );

            assertThat(result.values()).containsExactly("http-get", "kafka");
        }

        /** Only the API arm of a traffic scope applies: a message of another API through a scoped entrypoint stays out. */
        @Test
        void should_offer_only_the_connectors_of_the_apis_a_traffic_scope_names() {
            var result = search(
                query("connector-id", null, null)
                    .apiIds(Set.of(HTTP_GET_API, "stamped-api-001"))
                    .trafficScope(new TrafficScope(Set.of(HTTP_GET_API), Set.of("http-post")))
                    .size(10)
            );

            assertThat(result.values()).containsExactly("http-get", "mock");
        }

        /** Message documents carry no API product: a request scoped to one is offered no connector (fail-closed). */
        @Test
        void should_offer_no_connector_to_a_request_scoped_to_an_api_product() {
            assertThat(search(query("connector-id", MESSAGE_API, null).apiProductId("product-1").size(10)).values()).isEmpty();
        }
    }

    @Nested
    class Constraints_kept_with_a_prefix {

        @Test
        void should_page_through_the_matching_values() {
            var first = search(query("entrypoint-id", MESSAGE_API, "HTTP-").size(1));
            var second = search(query("entrypoint-id", MESSAGE_API, "HTTP-").size(1).afterKey(first.afterKey()));
            var afterTheLast = search(query("entrypoint-id", MESSAGE_API, "HTTP-").size(1).afterKey(second.afterKey()));

            assertThat(first.values()).containsExactly("http-get");
            assertThat(second.values()).containsExactly("http-post");
            assertThat(afterTheLast.values()).isEmpty();
        }

        @Test
        void should_offer_only_the_requested_values_among_those_matching() {
            var result = search(query("entrypoint-id", MESSAGE_API, "http").size(10).values(Set.of("http-post", "sse")));

            assertThat(result.values()).containsExactly("http-post");
        }

        @Test
        void should_offer_only_the_values_of_the_readable_apis() {
            assertThat(search(query("custom.card-tier", null, "gol").apiIds(Set.of(FRAUD_TIER_API)).size(10)).values()).isEmpty();
            assertThat(search(query("custom.card-tier", null, "gol").apiIds(Set.of()).size(10)).values()).isEmpty();
        }

        @Test
        void should_offer_only_the_values_of_the_calls_in_the_period() {
            var now = TimeProvider.now();

            var lastTwoDays = query("custom.card-tier", CARD_TIER_API, "gol")
                .from(now.minus(Duration.ofDays(2)).toEpochMilli())
                .to(now.toEpochMilli())
                .size(10);
            var lastTwentyDays = query("custom.card-tier", CARD_TIER_API, "gol")
                .from(now.minus(Duration.ofDays(20)).toEpochMilli())
                .to(now.toEpochMilli())
                .size(10);

            assertThat(search(lastTwoDays).values()).isEmpty();
            assertThat(search(lastTwentyDays).values()).containsExactly("gold", "golden");
        }
    }

    private List<String> values(String field, String apiId, String typed) {
        return search(query(field, apiId, typed).size(10)).values();
    }

    private static FilterValuesQuery.FilterValuesQueryBuilder query(String field, String apiId, String typed) {
        var builder = FilterValuesQuery.builder().esFieldName(field).searchPattern(typed);
        return apiId == null ? builder : builder.apiIds(Set.of(apiId));
    }

    private FilterValuesResult search(FilterValuesQuery.FilterValuesQueryBuilder query) {
        return analyticsRepository.searchFilterValues(queryContext, query.build());
    }
}
