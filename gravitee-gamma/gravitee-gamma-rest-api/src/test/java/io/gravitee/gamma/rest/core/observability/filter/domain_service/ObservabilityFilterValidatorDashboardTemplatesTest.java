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
package io.gravitee.gamma.rest.core.observability.filter.domain_service;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * "Duplicate as custom dashboard" posts a module template's filters unchanged, so every dashboard filter a
 * shipped template declares must pass the save-time check against the real catalog. The shapes are the AIM
 * templates' (`apiTypeScope` and `filterSlot` in `config/templates/shared.ts`); the authz and APIM templates
 * declare no dashboard filter.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityFilterValidatorDashboardTemplatesTest {

    private final ObservabilityFilterValidator validator = new ObservabilityFilterValidator(new SpiFilterRegistry());

    @ParameterizedTest
    @ValueSource(strings = { "LLM", "MCP", "A2A" })
    void should_accept_the_api_type_scope_a_template_locks(String apiType) {
        var conditions = List.of(new FilterCondition("API_TYPE", FilterOperator.IN, List.of(apiType)));

        assertThatCode(() -> validator.validateDashboardFilters(conditions)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "API",
            "APPLICATION",
            "HTTP_METHOD",
            "API_PRODUCT",
            "LLM_PROXY_MODEL",
            "LLM_PROXY_PROVIDER",
            "MCP_PROXY_TOOL",
            "MCP_PROXY_METHOD",
        }
    )
    void should_accept_the_empty_slots_templates_offer(String field) {
        var conditions = List.of(
            new FilterCondition("API_TYPE", FilterOperator.IN, List.of("LLM")),
            new FilterCondition(field, FilterOperator.IN, List.of())
        );

        assertThatCode(() -> validator.validateDashboardFilters(conditions)).doesNotThrowAnyException();
    }
}
