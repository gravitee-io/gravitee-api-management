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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Same cases as the observability library's {@code deriveAdditionalMetricLabel}, so both derive the same label. The
 * server labels fields only, typed by the grammar: the library's cases of a key with no type prefix have no field here.
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
            "long_failover_count | LONG | Failover count",
            "keyword_mcp-proxy_tools/call | KEYWORD | Mcp proxy tools/call",
            "json_fraud-signals | JSON | Fraud signals",
            "long_llm-proxy_tokens-received | LONG | Llm proxy tokens received",
            "double__checkout--basket__value_ | DOUBLE | Checkout basket value",
            "bool_-_ | BOOL | bool_-_",
        }
    )
    void should_derive_the_label_as_the_library_does(String rawKey, AdditionalMetricValueType valueType, String label) {
        assertThat(AdditionalMetricLabel.derive(new AdditionalMetricField(rawKey, valueType))).isEqualTo(label);
    }
}
