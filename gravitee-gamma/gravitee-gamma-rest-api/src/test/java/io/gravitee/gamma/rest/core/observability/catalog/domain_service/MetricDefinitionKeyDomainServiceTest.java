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
package io.gravitee.gamma.rest.core.observability.catalog.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.MetricDefinitionNames;
import io.gravitee.gamma.rest.core.observability.catalog.port.service_provider.AdditionalMetricGrammarPort;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MetricDefinitionKeyDomainServiceTest {

    private final MetricDefinitionKeyDomainService keys = new MetricDefinitionFixture().keys;

    /** A custom name the grammar refuses without naming a rule is still a 400, never a 500. */
    @Test
    void should_refuse_a_custom_name_with_a_fallback_when_the_grammar_names_no_rule() {
        var silentGrammar = new AdditionalMetricGrammarPort() {
            @Override
            public Optional<AdditionalMetricField> parse(String name) {
                return Optional.empty();
            }

            @Override
            public Optional<AdditionalMetricValueType> typeDeclaredBy(String rawKey) {
                return Optional.empty();
            }

            @Override
            public Optional<String> refusal(String name) {
                return Optional.empty();
            }
        };
        var silentKeys = new MetricDefinitionKeyDomainService(silentGrammar, new MetricDefinitionFixture().ports);

        assertThatThrownBy(() -> silentKeys.field("custom.fraud-tier"))
            .hasMessage("'custom.fraud-tier' is not a valid field name: it is not a valid custom metric name")
            .extracting("technicalCode")
            .isEqualTo("observability.metric_definition.invalid_name");
    }

    @Test
    void should_name_the_root_of_the_reserved_typed_family_when_refusing_it() {
        assertThatThrownBy(() -> keys.writableField("additional-metrics.double_custom_basket-value"))
            .hasMessage(
                "'additional-metrics.double_custom_basket-value' is reserved: keys of the form additional-metrics.<type>_custom_<name> belong to the typed custom metrics"
            )
            .extracting("technicalCode")
            .isEqualTo("observability.metric_definition.reserved_key");
    }

    @Test
    void should_tell_the_custom_metric_a_decoy_looks_like_and_refuse_to_for_any_other_field() {
        assertThat(keys.field("additional-metrics.keyword_custom.a.b").customMetricLookalike()).isEqualTo("custom.a.b");
        assertThatThrownBy(() -> keys.field("additional-metrics.keyword_customer.tier").customMetricLookalike()).isInstanceOf(
            IllegalStateException.class
        );
        assertThatThrownBy(() -> keys.field("custom.keyword_custom.x").customMetricLookalike()).isInstanceOf(IllegalStateException.class);
    }

    /** The grammar reads these names, so reads accept them; only a write refuses them. */
    @ParameterizedTest
    @ValueSource(strings = { "custom.fraud\r\ntier", "custom.fraud\u0000tier", "custom.fraud\uD83Dtier", "custom.fraud\uDE80tier" })
    void should_read_but_never_write_a_name_holding_a_control_character_or_an_unpaired_surrogate(String name) {
        assertThat(keys.field(name).name()).isEqualTo(name);
        assertThatThrownBy(() -> keys.writableField(name))
            .hasMessageEndingWith(": " + MetricDefinitionNames.UNPRINTABLE_RULE)
            .extracting("technicalCode")
            .isEqualTo("observability.metric_definition.invalid_name");
    }
}
