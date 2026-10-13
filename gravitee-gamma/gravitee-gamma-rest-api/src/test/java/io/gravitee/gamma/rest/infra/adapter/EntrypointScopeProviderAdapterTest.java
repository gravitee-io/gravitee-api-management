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

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.repository.analytics.engine.api.query.ObservabilityEntrypoints;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/** Gamma reads the registry through this adapter and must agree with it on the tokens both sides exchange. */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EntrypointScopeProviderAdapterTest {

    private final EntrypointScopeProviderAdapter adapter = new EntrypointScopeProviderAdapter();

    @Test
    void should_leave_out_of_logs_the_dedicated_families_and_the_excluded_entrypoints_only() {
        assertThat(adapter.excludedFromLogs())
            .isEqualTo(ObservabilityEntrypoints.LOGS_EXCLUDED_IDS)
            .containsExactly("edge", "authzen", "tcp-proxy");
    }

    @Test
    void should_name_the_entrypoints_adding_an_api_to_the_ai_kinds_whatever_its_type() {
        assertThat(adapter.entrypointsAdding(Set.of(ApiType.MCP, ApiType.LLM, ApiType.A2A))).containsExactlyInAnyOrder(
            "mcp",
            "agent-to-agent"
        );
    }

    @Test
    void should_name_no_entrypoint_for_kinds_only_the_api_type_decides() {
        assertThat(adapter.entrypointsAdding(Set.of(ApiType.HTTP_PROXY, ApiType.MESSAGE, ApiType.NATIVE, ApiType.LLM))).isEmpty();
    }
}
