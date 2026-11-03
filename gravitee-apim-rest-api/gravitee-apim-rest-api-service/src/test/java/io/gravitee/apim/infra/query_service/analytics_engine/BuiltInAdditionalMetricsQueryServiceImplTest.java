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
package io.gravitee.apim.infra.query_service.analytics_engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class BuiltInAdditionalMetricsQueryServiceImplTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @Test
    void should_add_the_fields_the_filter_values_read_to_those_the_repository_resolves() {
        when(analyticsRepository.builtInAdditionalMetricKeys()).thenReturn(
            Map.of(
                "keyword_authz_subject-type",
                Set.of("AUTHZ_SUBJECT_ID"),
                "keyword_mcp-proxy_tools/call",
                Set.of("MCP_PROXY_TOOL_ENGINE")
            )
        );

        var namesByRawKey = new BuiltInAdditionalMetricsQueryServiceImpl(analyticsRepository).namesByRawKey();

        assertThat(namesByRawKey)
            .containsEntry("keyword_authz_subject-type", Set.of("AUTHZ_SUBJECT_ID"))
            .containsEntry("keyword_mcp-proxy_tools/call", Set.of("MCP_PROXY_TOOL_ENGINE", "MCP_PROXY_TOOL"))
            .containsEntry("keyword_native-kafka_client-software-version", Set.of("NATIVE_CLIENT_SOFTWARE_VERSION"));
    }

    @Test
    void should_list_only_the_additional_metric_keys_of_the_filter_values_mapping() {
        assertThat(FilterValuesQueryServiceImpl.additionalMetricKeys())
            .containsEntry("keyword_llm-proxy_model", Set.of("LLM_PROXY_MODEL"))
            .containsKey("keyword_edge_type")
            .doesNotContainKeys("api-id", "status");
        assertThat(FilterValuesQueryServiceImpl.additionalMetricKeys().keySet()).noneMatch(key -> key.startsWith("additional-metrics."));
    }
}
