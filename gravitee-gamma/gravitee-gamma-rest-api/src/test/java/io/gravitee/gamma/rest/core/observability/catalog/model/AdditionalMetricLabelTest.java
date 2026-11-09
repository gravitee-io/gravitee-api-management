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

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The cases of the observability library's {@code deriveAdditionalMetricLabel} on a key of no known namespace, so
 * both derive the same label there, then the server's own namespace rule. The server labels fields only, typed by
 * the grammar: the library's cases of a key with no type prefix have no field here.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricLabelTest {

    @ParameterizedTest(name = "{0} -> {2}")
    @CsvSource(
        delimiter = '|',
        value = {
            "keyword_LLM_modelName | KEYWORD | LLM modelName",
            "long_my-policy_score | LONG | My policy score",
            "long_ | LONG | long_",
            "json_fraud-signals | JSON | Fraud signals",
            "keyword_fraud-check_tier | KEYWORD | Fraud check tier",
            "keyword_action | KEYWORD | Action",
            "double__checkout--basket__value_ | DOUBLE | Checkout basket value",
            "bool_-_ | BOOL | bool_-_",
        }
    )
    void should_derive_the_label_of_an_unknown_namespace_as_the_library_does(
        String rawKey,
        AdditionalMetricValueType valueType,
        String label
    ) {
        assertThat(AdditionalMetricLabel.derive(new AdditionalMetricField(rawKey, valueType))).isEqualTo(label);
    }

    /** A namespace naming one API kind is the group the key is listed under: the label leaves it out. */
    @ParameterizedTest(name = "{0} -> {2}")
    @CsvSource(
        delimiter = '|',
        value = {
            "long_llm-proxy_tokens-cached | LONG | Tokens cached",
            "long_llm-proxy_tokens-received | LONG | Tokens received",
            "keyword_mcp-proxy_endpoint-protocol-version | KEYWORD | Endpoint protocol version",
            "keyword_mcp-proxy_tools/call | KEYWORD | Tools/call",
            "long_native-kafka_messages-in | LONG | Messages in",
            "bool_pii_detected | BOOL | Detected",
            "long_edge_tokens_in | LONG | Tokens in",
        }
    )
    void should_leave_out_a_namespace_naming_one_api_kind(String rawKey, AdditionalMetricValueType valueType, String label) {
        assertThat(AdditionalMetricLabel.derive(new AdditionalMetricField(rawKey, valueType))).isEqualTo(label);
    }

    /**
     * Two namespaces of one kind leave the same name: the labels collide, by choice (OBS-193). The raw key tells them
     * apart on the wire and in the Settings table, and a declared label wins.
     */
    @Test
    void should_let_two_namespaces_of_one_kind_derive_the_same_label() {
        assertThat(AdditionalMetricLabel.derive(new AdditionalMetricField("long_pii_hits", AdditionalMetricValueType.LONG))).isEqualTo(
            "Hits"
        );
        assertThat(
            AdditionalMetricLabel.derive(new AdditionalMetricField("long_semantic-caching_hits", AdditionalMetricValueType.LONG))
        ).isEqualTo("Hits");
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @CsvSource(
        delimiter = '|',
        value = {
            "keyword_edge_model | KEYWORD | Edge model",
            "long_llm-proxy_tokens-cached | LONG | Llm proxy tokens cached",
            "long_ | LONG | long_",
        }
    )
    void should_derive_from_the_whole_key_when_asked_to_keep_the_namespace(
        String rawKey,
        AdditionalMetricValueType valueType,
        String label
    ) {
        assertThat(AdditionalMetricLabel.deriveFromWholeKey(new AdditionalMetricField(rawKey, valueType))).isEqualTo(label);
    }

    /** A namespace of several kinds names no group of its own: it stays, as does a namespace the map does not know. */
    @ParameterizedTest(name = "{0} -> {2}")
    @CsvSource(
        delimiter = '|',
        value = {
            "long_failover_count | LONG | Failover count",
            "long_quota_remaining | LONG | Quota remaining",
            "double_custom_basket-value | DOUBLE | Custom basket value",
            "keyword_llm-proxy | KEYWORD | Llm proxy",
            "long_llm-proxy_ | LONG | Llm proxy",
            "long_llm-proxy_- | LONG | Llm proxy",
        }
    )
    void should_keep_any_other_namespace_and_the_whole_name_when_nothing_would_be_left(
        String rawKey,
        AdditionalMetricValueType valueType,
        String label
    ) {
        assertThat(AdditionalMetricLabel.derive(new AdditionalMetricField(rawKey, valueType))).isEqualTo(label);
    }
}
