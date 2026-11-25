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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricGrammarPortAdapterTest {

    private final AdditionalMetricGrammarPortAdapter grammar = new AdditionalMetricGrammarPortAdapter();

    /** The Gamma roots mirror the platform's by name: a root added on one side only fails here, not at runtime. */
    @Test
    void should_mirror_every_root_of_the_platform_with_the_same_prefix() {
        var platformRoots = io.gravitee.apim.core.observability.model.AdditionalMetricField.Root.values();

        assertThat(AdditionalMetricField.Root.values()).hasSameSizeAs(platformRoots);
        assertThat(platformRoots).allSatisfy(root ->
            assertThat(AdditionalMetricField.Root.valueOf(root.name()).prefix()).isEqualTo(root.prefix())
        );
    }

    @Test
    void should_read_a_custom_name_into_a_keyword_field_under_the_custom_root() {
        assertThat(grammar.parse("custom.order.channel")).contains(
            new AdditionalMetricField(AdditionalMetricField.Root.CUSTOM, "order.channel", AdditionalMetricValueType.KEYWORD)
        );
    }

    @Test
    void should_read_every_name_of_the_platform_grammar_back_to_the_same_wire_name() {
        assertThat(Arrays.asList("additional-metrics.keyword_mcp-proxy_tools/call", "custom.fraud-tier", "custom.Merchant ID")).allSatisfy(
            name -> assertThat(grammar.parse(name)).hasValueSatisfying(field -> assertThat(field.name()).isEqualTo(name))
        );
    }
}
