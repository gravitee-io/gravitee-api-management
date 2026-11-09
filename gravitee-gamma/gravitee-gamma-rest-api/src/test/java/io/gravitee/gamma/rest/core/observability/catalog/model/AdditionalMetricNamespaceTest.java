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
package io.gravitee.gamma.rest.core.observability.catalog.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.BuiltInAdditionalMetricKeys;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pins the interim namespace map (OBS-194 deletes it) and what the catalog reads from a key's namespace. The map
 * must know every namespace the platform's built-ins write under: a namespace they use is a Gravitee one.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricNamespaceTest {

    private static final Set<ApiType> HTTP_FAMILY = Set.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MESSAGE, ApiType.MCP, ApiType.A2A);

    @Test
    void should_tie_every_gravitee_namespace_to_the_kinds_its_writers_serve() {
        assertThat(AdditionalMetricNamespace.GRAVITEE_API_TYPES).containsExactlyInAnyOrderEntriesOf(
            Map.ofEntries(
                Map.entry("llm-proxy", Set.of(ApiType.LLM)),
                Map.entry("pii", Set.of(ApiType.LLM)),
                Map.entry("semantic-caching", Set.of(ApiType.LLM)),
                Map.entry("mcp-proxy", Set.of(ApiType.MCP)),
                Map.entry("native-kafka", Set.of(ApiType.NATIVE)),
                Map.entry("webhook", Set.of(ApiType.MESSAGE)),
                Map.entry("edge", Set.of(ApiType.EDGE)),
                Map.entry("authz", Set.of(ApiType.AUTHZ)),
                Map.entry("failover", HTTP_FAMILY),
                Map.entry("quota", HTTP_FAMILY)
            )
        );
    }

    /** The keys the platform reads under a built-in name are Gravitee's: their namespaces must all be in the map. */
    @Test
    void should_know_every_namespace_the_built_in_keys_are_written_under() {
        var builtInNamespaces = BuiltInAdditionalMetricKeys.all()
            .keySet()
            .stream()
            .map(rawKey -> AdditionalMetricNamespace.segmentOf(new AdditionalMetricField(rawKey, valueTypeOf(rawKey))))
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());

        assertThat(builtInNamespaces).isNotEmpty().contains("llm-proxy", "mcp-proxy", "authz", "edge", "native-kafka");
        assertThat(AdditionalMetricNamespace.GRAVITEE_API_TYPES.keySet()).containsAll(builtInNamespaces);
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @CsvSource(
        delimiter = '|',
        nullValues = "null",
        value = {
            "long_llm-proxy_tokens-cached | LONG | llm-proxy",
            "keyword_mcp-proxy_tools/call | KEYWORD | mcp-proxy",
            "long_failover_count | LONG | failover",
            "long_edge_tokens_in | LONG | edge",
            "keyword_fraud-check_tier | KEYWORD | fraud-check",
            "keyword_action | KEYWORD | null",
            "long_failover_ | LONG | null",
            "double__checkout--basket__value_ | DOUBLE | null",
            "long_ | LONG | null",
        }
    )
    void should_read_the_segment_between_the_type_prefix_and_the_name(String rawKey, AdditionalMetricValueType valueType, String segment) {
        assertThat(AdditionalMetricNamespace.segmentOf(new AdditionalMetricField(rawKey, valueType))).isEqualTo(
            Optional.ofNullable(segment)
        );
    }

    @Test
    void should_serve_a_gravitee_namespace_with_its_kinds() {
        var field = new AdditionalMetricField("long_llm-proxy_tokens-cached", AdditionalMetricValueType.LONG);

        assertThat(field.namespace()).contains("llm-proxy");
        assertThat(field.apiTypes()).containsExactly(ApiType.LLM);
        assertThat(new AdditionalMetricField("long_quota_remaining", AdditionalMetricValueType.LONG).apiTypes()).isEqualTo(HTTP_FAMILY);
    }

    @Test
    void should_serve_the_custom_namespace_of_a_user_key_with_no_kind() {
        var field = new AdditionalMetricField("double_custom_basket-value", AdditionalMetricValueType.DOUBLE);

        assertThat(field.isReserved()).isTrue();
        assertThat(field.namespace()).contains("custom");
        assertThat(field.apiTypes()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "keyword_fraud-check_tier, KEYWORD", "keyword_action, KEYWORD", "long_failover_, LONG" })
    void should_serve_no_namespace_and_no_kind_for_a_namespace_the_map_does_not_know(String rawKey, AdditionalMetricValueType valueType) {
        var field = new AdditionalMetricField(rawKey, valueType);

        assertThat(field.namespace()).isEmpty();
        assertThat(field.apiTypes()).isEmpty();
    }

    private static AdditionalMetricValueType valueTypeOf(String rawKey) {
        return Arrays.stream(AdditionalMetricValueType.values())
            .filter(valueType -> rawKey.startsWith(valueType.prefix()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No type prefix on " + rawKey));
    }
}
