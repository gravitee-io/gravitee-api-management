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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class BuiltInAdditionalMetricKeysTest {

    @Test
    void should_hold_the_keys_the_analytics_resolvers_read_for_metrics_filters_and_facets() {
        assertThat(BuiltInAdditionalMetricKeys.all())
            .containsEntry("long_llm-proxy_tokens-sent", Set.of("LLM_PROMPT_TOKEN_SENT"))
            .containsEntry("keyword_mcp-proxy_tools/call", Set.of("MCP_PROXY_TOOL"))
            .containsKeys("double_llm-proxy_sent-cost", "keyword_llm-proxy_model", "keyword_edge_tool", "keyword_authz_operation");
    }

    @Test
    void should_hold_the_companion_fields_read_through_the_entity_type_of_a_filter() {
        assertThat(new AuthzTrafficFieldResolver().entityTypeFromFilter(new Filter(Filter.Name.AUTHZ_SUBJECT_ID, Filter.Operator.EQ, "x")))
            .as("the companion this test relies on")
            .isEqualTo("additional-metrics.keyword_authz_subject-type");
        assertThat(BuiltInAdditionalMetricKeys.all())
            .as("taken, but served under no name: the filter's own field is the one it is served under")
            .containsEntry("keyword_authz_subject-type", Set.of())
            .containsEntry("keyword_authz_resource-type", Set.of())
            .containsEntry("keyword_authz_subject-id", Set.of("AUTHZ_SUBJECT_ID"));
    }

    @Test
    void should_hold_the_keys_only_the_logs_path_reads() {
        assertThat(BuiltInAdditionalMetricKeys.all()).containsKeys(
            "keyword_native-kafka_client-software-version",
            "keyword_native-kafka_failure-side",
            "keyword_native-kafka_broker-id",
            "int_authz_batch-index",
            "int_authz_batch-size"
        );
    }

    @Test
    void should_hold_raw_keys_only() {
        assertThat(BuiltInAdditionalMetricKeys.all().keySet())
            .isNotEmpty()
            .allSatisfy(key ->
                assertThat(key).doesNotStartWith("additional-metrics.").matches("^(long|int|double|bool|keyword|string|json)_.+")
            );
    }

    @Test
    void should_run_every_field_resolver_of_the_package() throws Exception {
        var packagePath = BuiltInAdditionalMetricKeys.class.getPackageName().replace('.', '/');
        // The main classes, not the first match of the classpath: the test classes share the package.
        var directory = Path.of(BuiltInAdditionalMetricKeys.class.getProtectionDomain().getCodeSource().getLocation().toURI()).resolve(
            packagePath
        );
        Set<Class<?>> resolvers = new HashSet<>();
        try (var files = Files.list(directory)) {
            for (var file : files
                .map(f -> f.getFileName().toString())
                .filter(f -> f.endsWith(".class") && !f.contains("$"))
                .toList()) {
                var type = Class.forName(BuiltInAdditionalMetricKeys.class.getPackageName() + "." + file.replace(".class", ""));
                if (FieldResolver.class.isAssignableFrom(type) && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
                    resolvers.add(type);
                }
            }
        }

        assertThat(resolvers).isNotEmpty();
        assertThat(BuiltInAdditionalMetricKeys.RESOLVERS.stream().map(Object::getClass).collect(Collectors.toSet())).isEqualTo(resolvers);
    }
}
