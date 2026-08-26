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
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.utils.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Asserts what the es{@code 7,8,9}x index templates actually render: the configured lifecycle property names
 * when they are overridden, the Elasticsearch defaults when they are blank, and JSON-safe output when a name
 * is malformed. Runs without the OpenSearch container {@link IndexPreparerIntegrationTest} needs, which
 * proves the complementary half — that a cluster accepts the rendered body.
 */
class IndexTemplateTest {

    /**
     * Every dated-index type. The two data streams are covered separately: they render a policy but must
     * not carry a rollover alias, so they cannot share these assertions. es7x is kept alongside es8x/es9x
     * because it is the odd one out structurally — its settings sit at the root rather than under
     * {@code template} — and it is pushed through the legacy template API.
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

    /**
     * Every rendered template whose dynamic templates map {@code additional-metrics.keyword_*}. OpenSearch has
     * no preparer this harness can drive, so its templates are covered by the sibling test that reads them off
     * the classpath.
     */
    static Stream<Arguments> es_trees_and_types_carrying_additional_keyword_metrics() {
        return Stream.of("es7x", "es8x", "es9x").flatMap(esDir ->
            Stream.of(Type.REQUEST, Type.V4_METRICS, Type.V4_MESSAGE_METRICS).map(type -> Arguments.of(esDir, type))
        );
    }

    @ParameterizedTest(name = "{0} {1} bounds additional-metrics.keyword_* with ignore_above")
    @MethodSource("es_trees_and_types_carrying_additional_keyword_metrics")
    void should_bound_additional_keyword_metrics_so_one_oversized_value_cannot_break_indexing(String esDir, Type type) {
        // additional-metrics.keyword_* carries values a client controls — the Kafka client.id arrives before
        // authentication and the wire format allows 32767 bytes of it. Lucene's term limit is 32766, so an
        // unbounded keyword makes Elasticsearch reject the bulk item and the connection record is lost.
        // Defence in depth: the gateway bounds client.id at the source, this covers every keyword_* metric,
        // including ones other plugins add later.
        assertThat(preparerFor(esDir, configurationWithPolicies()).generateIndexTemplate(type))
            .contains("\"additional-metrics.keyword_*\"")
            .contains("\"ignore_above\"");
    }

    @ParameterizedTest(name = "{0} templates bound additional-metrics.keyword_* with ignore_above")
    @ValueSource(strings = { "es7x", "es8x", "es9x", "opensearch" })
    void should_bound_additional_keyword_metrics_in_every_template_family(String esDir) throws Exception {
        // Read off the classpath rather than rendered, so OpenSearch — which has no preparer to drive — is
        // covered by the same invariant as the rest.
        var templates = List.of("index-template-request.ftl", "index-template-v4-metrics.ftl", "index-template-v4-message-metrics.ftl");

        for (var template : templates) {
            var path = "/freemarker/" + esDir + "/mapping/" + template;
            try (var in = IndexTemplateTest.class.getResourceAsStream(path)) {
                assertThat(in).as("%s is on the classpath", path).isNotNull();
                var body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                assertThat(body).as("%s maps additional-metrics.keyword_*", path).contains("\"additional-metrics.keyword_*\"");
                assertThat(body).as("%s bounds it with ignore_above", path).contains("\"ignore_above\"");
            }
        }
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

    static Stream<Arguments> es_trees_and_data_stream_types() {
        return Stream.of("es7x", "es8x", "es9x").flatMap(esDir ->
            Stream.of(Type.EVENT_METRICS, Type.AUTHZ_DECISIONS).map(type -> Arguments.of(esDir, type))
        );
    }

    @ParameterizedTest(name = "{0} {1} renders the configured lifecycle policy")
    @MethodSource("es_trees_and_data_stream_types")
    void should_render_the_lifecycle_policy_of_a_data_stream(String esDir, Type type) {
        assertThat(preparerFor(esDir, configurationWithPolicies()).generateIndexTemplate(type)).contains(
            "\"index.lifecycle.name\": \"policy-" + type.getType() + "\""
        );
    }

    @ParameterizedTest(name = "{0} {1} sets no rollover alias")
    @MethodSource("es_trees_and_data_stream_types")
    void should_not_set_a_rollover_alias_on_a_data_stream(String esDir, Type type) {
        // A data stream rolls over on its own; Elasticsearch rejects the alias ILM uses for dated indexes.
        assertThat(preparerFor(esDir, configurationWithPolicies()).generateIndexTemplate(type)).doesNotContain("rollover_alias");
    }

    @ParameterizedTest(name = "{0} data stream templates escape the policy they interpolate")
    @ValueSource(strings = { "es7x", "es8x", "es9x", "opensearch" })
    void should_escape_the_lifecycle_policy_in_every_data_stream_template(String esDir) throws Exception {
        // Read off the classpath so OpenSearch, which has no preparer to drive here, is held to the same rule.
        for (var template : List.of("index-template-event-metrics.ftl", "index-template-authz-decisions.ftl")) {
            var path = "/freemarker/" + esDir + "/mapping/" + template;
            try (var in = IndexTemplateTest.class.getResourceAsStream(path)) {
                assertThat(in).as("%s is on the classpath", path).isNotNull();
                var body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                assertThat(body)
                    .as("%s escapes the policy name", path)
                    .doesNotContain("${indexLifecyclePolicyEventMetrics}")
                    .doesNotContain("${indexLifecyclePolicyAuthzDecisions}");
                assertThat(body).as("%s sets no rollover alias", path).doesNotContain("rollover_alias");
            }
        }
    }

    private static ReporterConfiguration configurationWithPolicies() {
        var configuration = new ReporterConfiguration();
        configuration.setIndexLifecyclePolicyHealth("policy-health");
        configuration.setIndexLifecyclePolicyMonitor("policy-monitor");
        configuration.setIndexLifecyclePolicyRequest("policy-request");
        configuration.setIndexLifecyclePolicyLog("policy-log");
        configuration.setIndexLifecyclePolicyEventMetrics("policy-event-metrics");
        configuration.setIndexLifecyclePolicyAuthzDecisions("policy-authz-decisions");
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
            default -> throw new IllegalArgumentException("Unknown es dir: " + esDir);
        };
    }
}
