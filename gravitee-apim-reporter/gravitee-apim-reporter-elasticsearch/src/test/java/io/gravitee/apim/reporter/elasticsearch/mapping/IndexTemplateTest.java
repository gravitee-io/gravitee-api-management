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
package io.gravitee.apim.reporter.elasticsearch.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.mapping.es7.ES7IndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.es8.ES8IndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.es9.ES9IndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.opensearch.OpenSearchIndexPreparer;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.utils.Type;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Asserts what the es{@code 7,8,9}x index templates actually render: the configured lifecycle property names
 * when they are overridden, the Elasticsearch defaults when they are blank, and JSON-safe output when a name
 * is malformed. Runs without the OpenSearch container {@link IndexPreparerIntegrationTest} needs, which
 * proves the complementary half — that a cluster accepts the rendered body.
 */
class IndexTemplateTest {

    /**
     * Every alias-managed type whose template can render lifecycle settings. es7x is kept alongside
     * es8x/es9x because it is the odd one out structurally — its settings sit at the root rather than
     * under {@code template} — and it is pushed through the legacy template API.
     *
     * <p>EVENT_METRICS is covered separately: it is the only data-stream template, so neither of those
     * two statements holds for it, and a data stream rolls itself over, so it must carry no rollover
     * alias at all.
     */
    static Stream<Arguments> es_trees_and_lifecycle_types() {
        return Stream.of("es7x", "es8x", "es9x").flatMap(esDir ->
            Stream.of(
                Type.REQUEST,
                Type.HEALTH_CHECK,
                Type.LOG,
                Type.MONITOR,
                Type.V4_LOG,
                Type.V4_METRICS,
                Type.V4_MESSAGE_LOG,
                Type.V4_MESSAGE_METRICS
            ).map(type -> Arguments.of(esDir, type))
        );
    }

    @ParameterizedTest(name = "{0} {1} template uses the configured ISM property names")
    @MethodSource("es_trees_and_lifecycle_types")
    void should_render_configured_ism_property_names_instead_of_hardcoded_ilm_keys(String esDir, Type type) {
        var configuration = configurationWithPolicies();
        configuration.setIndexLifecyclePolicyPropertyName("index.plugins.index_state_management.policy_id");
        configuration.setIndexLifecycleRolloverAliasPropertyName("index.plugins.index_state_management.rollover_alias");

        assertThat(preparerFor(esDir, configuration).generateIndexTemplate(type))
            .contains("\"index.plugins.index_state_management.policy_id\"")
            .contains("\"index.plugins.index_state_management.rollover_alias\"")
            .doesNotContain("\"index.lifecycle.");
    }

    @ParameterizedTest(name = "{0} {1} template falls back to the default keys when property names are blank")
    @MethodSource("es_trees_and_lifecycle_types")
    void should_fall_back_to_default_property_names_when_configured_blank(String esDir, Type type) {
        var configuration = configurationWithPolicies();
        configuration.setIndexLifecyclePolicyPropertyName("");
        configuration.setIndexLifecycleRolloverAliasPropertyName("  index.lifecycle.rollover_alias  ");

        // An empty or padded key would make the cluster reject the whole template, taking the shard, replica
        // and refresh settings down with it — blank has to mean "unset", not "render nothing at all".
        assertThat(preparerFor(esDir, configuration).generateIndexTemplate(type))
            .contains("\"index.lifecycle.name\"")
            .contains("\"index.lifecycle.rollover_alias\"")
            .doesNotContain("\"\":")
            .doesNotContain("\"  index.lifecycle.rollover_alias  \"");
    }

    static Stream<Arguments> es_trees() {
        return Stream.of(Arguments.of("es7x"), Arguments.of("es8x"), Arguments.of("es9x"));
    }

    /** Every tree that ships an event-metrics template, with the lifecycle key that tree writes. */
    static Stream<Arguments> all_trees_and_policy_keys() {
        return Stream.of(
            Arguments.of("es7x", "index.lifecycle.name"),
            Arguments.of("es8x", "index.lifecycle.name"),
            Arguments.of("es9x", "index.lifecycle.name"),
            Arguments.of("opensearch", "index.plugins.index_state_management.policy_id")
        );
    }

    static Stream<Arguments> all_trees() {
        return Stream.of(Arguments.of("es7x"), Arguments.of("es8x"), Arguments.of("es9x"), Arguments.of("opensearch"));
    }

    @ParameterizedTest(name = "{0} event-metrics template carries the configured lifecycle policy as {1}")
    @MethodSource("all_trees_and_policy_keys")
    void should_attach_the_configured_lifecycle_policy_to_the_event_metrics_data_stream(String esDir, String policyKey) {
        assertThat(preparerFor(esDir, configurationWithPolicies()).generateIndexTemplate(Type.EVENT_METRICS)).contains(
            "\"" + policyKey + "\": \"policy-event-metrics\""
        );
    }

    @ParameterizedTest(name = "{0} event-metrics template carries no rollover alias")
    @MethodSource("all_trees")
    void should_not_render_a_rollover_alias_on_the_event_metrics_data_stream(String esDir) {
        // A data stream rolls itself over: ILM/ISM resolve the rollover target from the parent data
        // stream and never read this setting, so writing it is spec-noise on a "data_stream" template.
        assertThat(preparerFor(esDir, configurationWithPolicies()).generateIndexTemplate(Type.EVENT_METRICS)).doesNotContain(
            "rollover_alias"
        );
    }

    @ParameterizedTest(name = "{0} event-metrics template escapes a malformed policy name")
    @MethodSource("all_trees")
    void should_escape_the_event_metrics_policy_so_a_malformed_one_cannot_break_the_json_body(String esDir) {
        var configuration = configurationWithPolicies();
        configuration.setIndexLifecyclePolicyEventMetrics("bad\"policy");

        assertThat(preparerFor(esDir, configuration).generateIndexTemplate(Type.EVENT_METRICS)).contains("\"bad\\\"policy\"");
    }

    @ParameterizedTest(name = "{0} event-metrics template honours the configured ISM property name")
    @MethodSource("es_trees")
    void should_attach_the_event_metrics_policy_under_the_configured_property_name(String esDir) {
        var configuration = configurationWithPolicies();
        configuration.setIndexLifecyclePolicyPropertyName("index.plugins.index_state_management.policy_id");

        assertThat(preparerFor(esDir, configuration).generateIndexTemplate(Type.EVENT_METRICS)).contains(
            "\"index.plugins.index_state_management.policy_id\": \"policy-event-metrics\""
        );
    }

    @Test
    void should_render_default_ilm_keys_for_an_untouched_elasticsearch_configuration() {
        assertThat(preparerFor("es8x", configurationWithPolicies()).generateIndexTemplate(Type.LOG))
            .contains("\"index.lifecycle.name\": \"policy-log\"")
            .contains("\"index.lifecycle.rollover_alias\"");
    }

    @Test
    void should_escape_property_names_and_policies_so_a_malformed_one_cannot_break_the_json_body() {
        var configuration = configurationWithPolicies();
        configuration.setIndexLifecyclePolicyPropertyName("bad\"name");
        configuration.setIndexLifecycleRolloverAliasPropertyName("bad\"alias");
        configuration.setIndexLifecyclePolicyLog("bad\"policy");

        assertThat(preparerFor("es8x", configuration).generateIndexTemplate(Type.LOG))
            .contains("\"bad\\\"name\"")
            .contains("\"bad\\\"alias\"")
            .contains("\"bad\\\"policy\"");
    }

    private static ReporterConfiguration configurationWithPolicies() {
        var configuration = new ReporterConfiguration();
        configuration.setIndexLifecyclePolicyHealth("policy-health");
        configuration.setIndexLifecyclePolicyMonitor("policy-monitor");
        configuration.setIndexLifecyclePolicyRequest("policy-request");
        configuration.setIndexLifecyclePolicyLog("policy-log");
        configuration.setIndexLifecyclePolicyEventMetrics("policy-event-metrics");
        return configuration;
    }

    private static AbstractIndexPreparer preparerFor(String esDir, ReporterConfiguration configuration) {
        var freeMarkerComponent = FreeMarkerComponent.builder()
            .classLoader(IndexTemplateTest.class.getClassLoader())
            .classLoaderTemplateBase("freemarker")
            .build();
        var pipelineConfiguration = new PipelineConfiguration(freeMarkerComponent);

        // No client: generateIndexTemplate only renders, it never talks to the cluster.
        return switch (esDir) {
            case "es7x" -> new ES7IndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, null);
            case "es8x" -> new ES8IndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, null);
            case "es9x" -> new ES9IndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, null);
            case "opensearch" -> new OpenSearchIndexPreparer(configuration, pipelineConfiguration, freeMarkerComponent, null);
            default -> throw new IllegalArgumentException("Unknown es dir: " + esDir);
        };
    }
}
