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
            "custom.basket",
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
    void should_tell_the_reserved_custom_form() {
        assertThat(keys.field("additional-metrics.double_custom_basket-value").isReserved()).isTrue();
        assertThat(keys.field("additional-metrics.double_customer_basket").isReserved()).isFalse();
        assertThat(keys.field("additional-metrics.keyword_my_custom_tag").isReserved()).isFalse();
    }
}
