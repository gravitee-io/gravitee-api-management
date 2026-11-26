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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.rest.core.observability.catalog.domain_service.MetricDefinitionKeyDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.exception.InvalidMetricDefinitionException;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Names are read by the platform's parser through the grammar port: these cases pin what Gamma accepts and refuses, and
 * the technical code of each refusal.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricFieldTest {

    private final MetricDefinitionKeyDomainService keys = new MetricDefinitionFixture().keys;

    @Test
    void should_read_the_raw_key_and_value_type_of_a_wire_name() {
        var field = keys.field("additional-metrics.keyword_mcp-proxy_tools/call");

        assertThat(field.rawKey()).isEqualTo("keyword_mcp-proxy_tools/call");
        assertThat(field.valueType()).isEqualTo(AdditionalMetricValueType.KEYWORD);
        assertThat(field.name()).isEqualTo("additional-metrics.keyword_mcp-proxy_tools/call");
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "additional-metrics.long_a",
            "additional-metrics.double_checkout_basket-value",
            "additional-metrics.long_a.b.c",
            "additional-metrics.bool_Cache_HIT",
            "additional-metrics.json_fraud-signals",
            "additional-metrics.string_a/b/c",
        }
    )
    void should_accept_the_grammar(String name) {
        assertThat(keys.field(name).name()).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "additional-metrics.long_",
            "additional-metrics.long_a\"b",
            "additional-metrics.long_a..b",
            "additional-metrics.long_a.",
            "additional-metrics.long_.a",
            "additional-metrics.long_a b",
            "additional-metrics.long_é",
            "long_failover_count",
            "custom.",
            "custom..a",
            "custom.a.",
            "custom.a\"b",
            "custom.a*b",
            "HTTP_STATUS",
            "",
        }
    )
    void should_refuse_a_name_outside_the_grammar(String name) {
        assertThatThrownBy(() -> keys.field(name))
            .isInstanceOf(InvalidMetricDefinitionException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.metric_definition.invalid_name");
    }

    @Test
    void should_refuse_an_unknown_type_prefix() {
        assertThatThrownBy(() -> keys.field("additional-metrics.float_score"))
            .isInstanceOf(InvalidMetricDefinitionException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.metric_definition.unknown_type_prefix");
    }

    @Test
    void should_accept_256_characters_and_refuse_257() {
        String prefix = "additional-metrics.long_";
        String atLimit = prefix + "a".repeat(256 - prefix.length());

        assertThat(keys.field(atLimit).name()).hasSize(256);
        assertThatThrownBy(() -> keys.field(atLimit + "a"))
            .extracting("technicalCode")
            .isEqualTo("observability.metric_definition.invalid_name");
    }

    @Test
    void should_tell_a_key_naming_a_custom_metric_after_its_type_prefix() {
        assertThat(keys.field("additional-metrics.keyword_custom.fraud-tier").namesCustomMetric()).isTrue();
        assertThat(keys.field("additional-metrics.long_custom.retries").namesCustomMetric()).isTrue();
        assertThat(keys.field("additional-metrics.keyword_customer.tier").namesCustomMetric()).isFalse();
        assertThat(keys.field("additional-metrics.double_custom_basket-value").namesCustomMetric()).isFalse();
        assertThat(
            new AdditionalMetricField(
                AdditionalMetricField.Root.CUSTOM,
                "keyword_custom.x",
                AdditionalMetricValueType.KEYWORD
            ).namesCustomMetric()
        ).isFalse();
    }

    @Test
    void should_tell_the_reserved_custom_form() {
        assertThat(keys.field("additional-metrics.double_custom_basket-value").isReserved()).isTrue();
        assertThat(keys.field("additional-metrics.double_customer_basket").isReserved()).isFalse();
        assertThat(keys.field("additional-metrics.keyword_my_custom_tag").isReserved()).isFalse();
    }

    /** The reserved typed family lives in the additional-metrics map (OBS-172): under custom. the spelling is a plain key. */
    @Test
    void should_not_reserve_a_custom_key_spelt_like_the_typed_form() {
        assertThat(keys.field("custom.keyword_custom_x").isReserved()).isFalse();
        assertThat(keys.writableField("custom.keyword_custom_x").name()).isEqualTo("custom.keyword_custom_x");
    }

    @Test
    void should_read_a_custom_name_as_a_keyword_of_the_custom_root() {
        var field = keys.field("custom.fraud-tier");

        assertThat(field.root()).isEqualTo(AdditionalMetricField.Root.CUSTOM);
        assertThat(field.rawKey()).isEqualTo("fraud-tier");
        assertThat(field.servedRawKey()).isEqualTo("custom.fraud-tier");
        assertThat(field.valueType()).isEqualTo(AdditionalMetricValueType.KEYWORD);
        assertThat(field.takesUnit()).isFalse();
        assertThat(keys.field("additional-metrics.keyword_tier").takesUnit()).isTrue();
    }

    @Test
    void should_accept_a_custom_name_of_256_characters_and_refuse_257_with_the_custom_rule() {
        String atLimit = "custom." + "a".repeat(256);

        assertThat(keys.field(atLimit).name()).hasSize(263);
        assertThatThrownBy(() -> keys.field(atLimit + "a"))
            .hasMessageContaining("is not a valid field name: a custom metric is named custom.<name>")
            .extracting("technicalCode")
            .isEqualTo("observability.metric_definition.invalid_name");
    }

    @Test
    void should_name_both_roots_when_a_name_has_neither() {
        assertThatThrownBy(() -> keys.field("HTTP_STATUS")).hasMessage(
            "'HTTP_STATUS' is not an additional or custom metric field: it must start with 'additional-metrics.' or 'custom.'"
        );
    }
}
