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
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.reporter.elasticsearch.config.PipelineConfiguration;
import io.gravitee.apim.reporter.elasticsearch.config.ReporterConfiguration;
import io.gravitee.apim.reporter.elasticsearch.mapping.es7.ES7IndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.es8.ES8IndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.es9.ES9IndexPreparer;
import io.gravitee.apim.reporter.elasticsearch.mapping.opensearch.OpenSearchIndexPreparer;
import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.utils.Type;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Asserts what the es{@code 7,8,9}x and {@code opensearch} index templates actually render: the configured lifecycle property names
 * when they are overridden, the Elasticsearch defaults when they are blank, and JSON-safe output when a name
 * is malformed. Runs without the OpenSearch container {@link IndexPreparerIntegrationTest} needs, which
 * proves the complementary half — that a cluster accepts the rendered body.
 */
class IndexTemplateTest {

    /**
     * Strict on purpose. A default mapper stops at the first complete value, so a stray {@code }} closing
     * the root after {@code settings} leaves the mappings as trailing content and still parses; and it
     * keeps the last of two identical keys, so a settings block emitting the lifecycle key twice — a merge
     * leaving both the hard-coded {@code index.lifecycle.name} and the interpolated property name — reads
     * as valid JSON here while the cluster silently drops one.
     */
    private static final ObjectMapper JSON = new ObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

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

    /** Every rendered template whose dynamic templates map {@code additional-metrics.keyword_*}, on every tree. */
    static Stream<Arguments> all_trees_and_types_carrying_additional_keyword_metrics() {
        return all_trees().flatMap(esDir ->
            Stream.of(Type.REQUEST, Type.V4_METRICS, Type.V4_MESSAGE_METRICS).map(type -> Arguments.of(esDir, type))
        );
    }

    @ParameterizedTest(name = "{0} {1} bounds additional-metrics.keyword_* with ignore_above")
    @MethodSource("all_trees_and_types_carrying_additional_keyword_metrics")
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

    /**
     * All three configurations, because they render different bodies: a policy adds a settings entry, and a
     * separator that only works in one arrangement leaves the others invalid. The unset case is the state
     * most installations are in; the override case is the AWS-OpenSearch-compat arrangement, where a
     * user-supplied string becomes a JSON key rather than a value.
     */
    @ParameterizedTest(name = "{0} {1} template is valid JSON with policies {2}")
    @MethodSource("all_trees_and_all_types_and_policy_state")
    void should_render_a_body_that_parses_as_json(String esDir, Type type, String policyState) {
        // Every other assertion here is a substring match, which cannot see a body broken into invalid
        // JSON by a stray separator — the failure mode a cluster reports only as a parse error.
        var configuration = configurationFor(policyState);

        assertThatNoException().isThrownBy(() -> JSON.readTree(preparerFor(esDir, configuration).generateIndexTemplate(type)));
    }

    private static ReporterConfiguration configurationFor(String policyState) {
        return switch (policyState) {
            case "unset" -> new ReporterConfiguration();
            case "set" -> configurationWithPolicies();
            case "set with overridden property names" -> {
                var configuration = configurationWithPolicies();
                configuration.setIndexLifecyclePolicyPropertyName("index.plugins.index_state_management.policy_id");
                configuration.setIndexLifecycleRolloverAliasPropertyName("index.plugins.index_state_management.rollover_alias");
                yield configuration;
            }
            default -> throw new IllegalArgumentException("Unknown policy state: " + policyState);
        };
    }

    /** Every type that has a template, taken from the set the preparer itself iterates: a type added
     * upstream gets a template in all four trees and would otherwise be left out of these assertions
     * with nothing to signal it. */
    static Stream<Type> all_types() {
        return Stream.of(Type.TYPES);
    }

    static Stream<Arguments> all_trees_and_all_types_and_policy_state() {
        return all_trees().flatMap(tree ->
            all_types().flatMap(type ->
                Stream.of("set", "unset", "set with overridden property names").map(policyState -> Arguments.of(tree, type, policyState))
            )
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

    /**
     * The lifecycle key each tree writes on a data stream: the es trees render the configured property
     * name, the opensearch tree hard-codes its ISM key and interpolates no property name at all.
     */
    private static final Map<String, String> DATA_STREAM_POLICY_KEY = Map.of(
        "es7x",
        "index.lifecycle.name",
        "es8x",
        "index.lifecycle.name",
        "es9x",
        "index.lifecycle.name",
        "opensearch",
        "index.plugins.index_state_management.policy_id"
    );

    static Stream<String> all_trees() {
        return Stream.of("es7x", "es8x", "es9x", "opensearch");
    }

    static Stream<Type> data_stream_types() {
        return Stream.of(Type.EVENT_METRICS, Type.DECISIONS);
    }

    /** Every tree that ships data-stream templates, against both data-stream types. */
    static Stream<Arguments> all_trees_and_data_stream_types() {
        return all_trees().flatMap(esDir -> data_stream_types().map(type -> Arguments.of(esDir, type)));
    }

    /** The es trees only: the opensearch tree hard-codes its ISM key, so there is no override to honour. */
    static Stream<Arguments> es_trees_and_data_stream_types() {
        return Stream.of("es7x", "es8x", "es9x").flatMap(esDir -> data_stream_types().map(type -> Arguments.of(esDir, type)));
    }

    @ParameterizedTest(name = "{0} {1} renders the configured lifecycle policy")
    @MethodSource("all_trees_and_data_stream_types")
    void should_render_the_lifecycle_policy_of_a_data_stream(String esDir, Type type) {
        assertThat(preparerFor(esDir, configurationWithPolicies()).generateIndexTemplate(type)).contains(
            "\"" + DATA_STREAM_POLICY_KEY.get(esDir) + "\": \"policy-" + type.getType() + "\""
        );
    }

    @ParameterizedTest(name = "{0} {1} sets no rollover alias")
    @MethodSource("all_trees_and_data_stream_types")
    void should_not_set_a_rollover_alias_on_a_data_stream(String esDir, Type type) {
        // A data stream rolls over on its own: ILM/ISM resolve the target from the parent stream and never
        // read this setting, and Elasticsearch rejects the alias ILM uses for dated indexes.
        assertThat(preparerFor(esDir, configurationWithPolicies()).generateIndexTemplate(type)).doesNotContain("rollover_alias");
    }

    @ParameterizedTest(name = "{0} {1} honours the configured ISM property name")
    @MethodSource("es_trees_and_data_stream_types")
    void should_render_the_data_stream_policy_under_the_configured_property_name(String esDir, Type type) {
        var configuration = configurationWithPolicies();
        configuration.setIndexLifecyclePolicyPropertyName("index.plugins.index_state_management.policy_id");

        assertThat(preparerFor(esDir, configuration).generateIndexTemplate(type)).contains(
            "\"index.plugins.index_state_management.policy_id\": \"policy-" + type.getType() + "\""
        );
    }

    @ParameterizedTest(name = "{0} {1} renders no lifecycle settings when no policy is set")
    @MethodSource("all_trees_and_data_stream_types")
    void should_render_no_lifecycle_settings_for_a_data_stream_when_no_policy_is_set(String esDir, Type type) {
        // The default deployment: the key is documented but unset, and must stay entirely absent.
        assertThat(preparerFor(esDir, new ReporterConfiguration()).generateIndexTemplate(type))
            .doesNotContain(DATA_STREAM_POLICY_KEY.get(esDir))
            .doesNotContain("rollover_alias");
    }

    @ParameterizedTest(name = "{0} {1} escapes a malformed policy")
    @MethodSource("all_trees_and_data_stream_types")
    void should_escape_the_lifecycle_policy_in_every_data_stream_template(String esDir, Type type) {
        // Rendered rather than read off the classpath: this proves the escape actually applies, where
        // inspecting the template source only proves the interpolation is spelled with ?json_string.
        var configuration = configurationWithPolicies();
        setDataStreamPolicy(configuration, type, "bad\"policy");

        assertThat(preparerFor(esDir, configuration).generateIndexTemplate(type)).contains("\"bad\\\"policy\"");
    }

    @ParameterizedTest(name = "{0} decisions mapping keeps the matched rule version")
    @MethodSource("all_trees")
    void should_map_the_matched_rule_version_as_a_keyword(String esDir) throws Exception {
        var matchedRules = decisionsMappings(esDir).path("properties").path("matched-rules");

        assertThat(matchedRules.path("type").asText()).isEqualTo("nested");
        assertThat(matchedRules.path("properties").path("version").path("type").asText()).isEqualTo("keyword");
    }

    @ParameterizedTest(name = "{0} renders no authz-decisions template")
    @MethodSource("all_trees")
    void should_render_no_authz_decisions_template(String esDir) {
        assertThat(Stream.of(Type.TYPES).map(Type::getType)).doesNotContain("authz-decisions");
        assertThat(IndexTemplateTest.class.getResource("/freemarker/" + esDir + "/mapping/index-template-authz-decisions.ftl")).isNull();
    }

    private static JsonNode decisionsMappings(String esDir) throws Exception {
        var mappings = JSON.readTree(preparerFor(esDir, configurationWithPolicies()).generateIndexTemplate(Type.DECISIONS))
            .path("template")
            .path("mappings");
        assertThat(mappings.isObject()).as("%s decisions template carries mappings", esDir).isTrue();
        return mappings;
    }

    private static void setDataStreamPolicy(ReporterConfiguration configuration, Type type, String policy) {
        switch (type) {
            case EVENT_METRICS -> configuration.setIndexLifecyclePolicyEventMetrics(policy);
            case DECISIONS -> configuration.setIndexLifecyclePolicyDecisions(policy);
            default -> throw new IllegalArgumentException("Not a data-stream type: " + type);
        }
    }

    private static ReporterConfiguration configurationWithPolicies() {
        var configuration = new ReporterConfiguration();
        configuration.setIndexLifecyclePolicyHealth("policy-health");
        configuration.setIndexLifecyclePolicyMonitor("policy-monitor");
        configuration.setIndexLifecyclePolicyRequest("policy-request");
        configuration.setIndexLifecyclePolicyLog("policy-log");
        configuration.setIndexLifecyclePolicyEventMetrics("policy-event-metrics");
        configuration.setIndexLifecyclePolicyDecisions("policy-decisions");
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
