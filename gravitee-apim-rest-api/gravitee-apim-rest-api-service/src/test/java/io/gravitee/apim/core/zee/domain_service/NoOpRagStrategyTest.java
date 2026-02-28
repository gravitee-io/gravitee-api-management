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
package io.gravitee.apim.core.zee.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NoOpRagStrategyTest {

    @Test
    void singleton_instance_is_not_null() {
        assertThat(NoOpRagStrategy.INSTANCE).isNotNull();
    }

    @Test
    void resource_type_is_null() {
        assertThat(NoOpRagStrategy.INSTANCE.resourceType()).isNull();
    }

    @Test
    void retrieve_context_returns_empty_string() {
        var result = NoOpRagStrategy.INSTANCE.retrieveContext("env-1", "org-1", Map.of("apiId", "api-123"));
        assertThat(result).isEmpty();
    }

    @Test
    void retrieve_context_returns_empty_string_for_empty_data() {
        var result = NoOpRagStrategy.INSTANCE.retrieveContext("env-1", "org-1", Map.of());
        assertThat(result).isEmpty();
    }

    @Test
    void retrieve_context_returns_empty_string_for_null_values() {
        var result = NoOpRagStrategy.INSTANCE.retrieveContext(null, null, Map.of());
        assertThat(result).isEmpty();
    }

    @Test
    void satisfies_rag_context_strategy_interface() {
        RagContextStrategy strategy = NoOpRagStrategy.INSTANCE;
        assertThat(strategy).isNotNull();
        assertThat(strategy.retrieveContext("e", "o", Map.of())).isEqualTo("");
    }
}
