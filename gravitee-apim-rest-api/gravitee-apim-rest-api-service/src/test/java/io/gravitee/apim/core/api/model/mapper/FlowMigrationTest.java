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
package io.gravitee.apim.core.api.model.mapper;

import static io.gravitee.apim.core.api.model.utils.MigrationResultUtils.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.model.utils.MigrationResult;
import io.gravitee.apim.core.api.model.utils.MigrationWarnings;
import io.gravitee.apim.infra.json.jackson.JsonMapperFactory;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.PathOperator;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FlowMigrationTest {

    private final FlowMigration cut = new FlowMigration(JsonMapperFactory.build());

    @Test
    void should_map_a_simple_flow() {
        var v2Flow = new Flow();
        v2Flow.setName("flow-name");
        v2Flow.setPathOperator(new PathOperator("/", Operator.STARTS_WITH));
        v2Flow.setMethods(Set.of(HttpMethod.GET, HttpMethod.POST));
        v2Flow.setEnabled(true);

        var result = cut.mapFlow(v2Flow);

        assertThat(get(result)).satisfies(v4Flow -> {
            assertThat(v4Flow.getName()).isEqualTo("flow-name");
            assertThat(v4Flow.isEnabled()).isTrue();
            assertThat(v4Flow.getRequest()).isEmpty();
            assertThat(v4Flow.getResponse()).isEmpty();
            assertThat(v4Flow.getSelectors())
                .hasSize(1)
                .first()
                .isInstanceOf(HttpSelector.class)
                .satisfies(httpSelector -> {
                    assertThat(((HttpSelector) httpSelector).getPath()).isEqualTo("/");
                    assertThat(((HttpSelector) httpSelector).getPathOperator()).isEqualTo(Operator.STARTS_WITH);
                    assertThat(((HttpSelector) httpSelector).getMethods()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.POST);
                });
        });
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void should_map_a_flow_with_pre_and_post_steps() {
        var v2Flow = new Flow();
        var preStep = new Step();
        preStep.setName("pre-step");
        preStep.setPolicy("rate-limit");
        preStep.setConfiguration("{}");
        v2Flow.setPre(List.of(preStep));

        var postStep = new Step();
        postStep.setName("post-step");
        postStep.setPolicy("mock");
        postStep.setConfiguration("{}");
        v2Flow.setPost(List.of(postStep));

        var result = cut.mapFlow(v2Flow);

        assertThat(get(result)).satisfies(v4Flow -> {
            assertThat(v4Flow.getRequest()).hasSize(1);
            assertThat(v4Flow.getRequest()).map(io.gravitee.definition.model.v4.flow.step.Step::getName).first().isEqualTo("pre-step");
            assertThat(v4Flow.getRequest()).map(io.gravitee.definition.model.v4.flow.step.Step::getPolicy).first().isEqualTo("rate-limit");
            assertThat(v4Flow.getResponse()).hasSize(1);
            assertThat(v4Flow.getResponse()).map(io.gravitee.definition.model.v4.flow.step.Step::getName).first().isEqualTo("post-step");
            assertThat(v4Flow.getResponse()).map(io.gravitee.definition.model.v4.flow.step.Step::getPolicy).first().isEqualTo("mock");
        });
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void should_return_issue_for_incompatible_policy() {
        var v2Flow = new Flow();
        var step = new Step();
        step.setPolicy("cloud-events");
        v2Flow.setPre(List.of(step));

        var result = cut.mapFlow(v2Flow);

        assertThrows(NullPointerException.class, () -> get(result));
        assertThat(result.issues())
            .map(MigrationResult.Issue::message)
            .containsExactly(MigrationWarnings.POLICY_NOT_COMPATIBLE.formatted("cloud-events"));
        assertThat(result.issues()).map(MigrationResult.Issue::state).containsExactly(MigrationResult.State.IMPOSSIBLE);
    }

    @Test
    void should_return_issue_for_non_gravitee_policy() {
        var v2Flow = new Flow();
        var step = new Step();
        step.setPolicy("unknown-policy");
        v2Flow.setPre(List.of(step));

        var result = cut.mapFlow(v2Flow);

        assertThat(get(result)).isNotNull();
        assertThat(result.issues())
            .map(MigrationResult.Issue::message)
            .containsExactly(MigrationWarnings.NON_GRAVITEE_POLICY.formatted("unknown-policy"));
        assertThat(result.issues()).map(MigrationResult.Issue::state).containsExactly(MigrationResult.State.CAN_BE_FORCED);
    }

    @Test
    void should_map_a_flow_with_a_condition() {
        var v2Flow = new Flow();
        v2Flow.setCondition("{#context.attribute['condition'] == 'true'}");

        var result = cut.mapFlow(v2Flow);

        assertThat(get(result)).satisfies(v4Flow ->
            assertThat(v4Flow.getSelectors())
                .hasSize(2)
                .anySatisfy(selector -> {
                    assertThat(selector).isInstanceOf(ConditionSelector.class);
                    assertThat(((ConditionSelector) selector).getCondition()).isEqualTo("{#context.attribute['condition'] == 'true'}");
                })
        );
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void should_not_map_blank_or_null_condition() {
        var v2Flow = new Flow();
        v2Flow.setCondition(" ");

        var result = cut.mapFlow(v2Flow);
        assertThat(get(result).getSelectors()).hasSize(1).noneMatch(ConditionSelector.class::isInstance);

        v2Flow.setCondition(null);
        result = cut.mapFlow(v2Flow);
        assertThat(get(result).getSelectors()).hasSize(1).noneMatch(ConditionSelector.class::isInstance);
    }

    @Test
    void should_not_map_blank_or_null_description_and_condition_on_step() {
        var v2Flow = new Flow();
        var step = new Step();
        step.setPolicy("rate-limit");
        step.setDescription(" ");
        step.setCondition(" ");
        v2Flow.setPre(List.of(step));

        var result = cut.mapFlow(v2Flow);

        assertThat(get(result).getRequest()).map(io.gravitee.definition.model.v4.flow.step.Step::getDescription).first().isNull();
        assertThat(get(result).getRequest()).map(io.gravitee.definition.model.v4.flow.step.Step::getCondition).first().isNull();
    }

    @Test
    void should_map_multiple_flows() {
        var v2Flow1 = new Flow();
        v2Flow1.setName("flow1");
        var v2Flow2 = new Flow();
        v2Flow2.setName("flow2");

        var result = cut.mapFlows(List.of(v2Flow1, v2Flow2));

        assertThat(get(result)).map(AbstractFlow::getName).containsExactly("flow1", "flow2");
        assertThat(result.issues()).isEmpty();
    }

    @Nested
    class GroovyPolicy {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        private static final String GROOVY_SCRIPT = "println(Hello World);";
        private static final Collection<String> OLD_KEYS = List.of(
            "onRequestContentScript",
            "onResponseContentScript",
            "onRequestScript",
            "onResponseScript"
        );

        @ParameterizedTest
        @CsvSource(
            delimiterString = "|",
            textBlock = """
            onRequestContentScript  | true        | true
            onRequestScript         | false       | false
            onResponseContentScript | true        | true
            onResponseScript        | false       | false
            """
        )
        void should_map_the_configuration(String inputField, boolean expectedReadContent, boolean expectedOverrideContent)
            throws Exception {
            // Given
            var v2Flow = buildV2Flow(
                """
                {
                  "%s" : "%s",
                  "scope" : "REQUEST"
                }
                """.formatted(inputField, GROOVY_SCRIPT)
            );

            // When
            var result = get(cut.mapFlow(v2Flow));

            // Then
            JsonNode jsonNode = OBJECT_MAPPER.readTree(result.getRequest().getFirst().getConfiguration());
            assertThat(jsonNode.get("scope").asText()).isEqualTo("REQUEST");
            assertThat(jsonNode.get("script").asText()).isEqualTo(GROOVY_SCRIPT);
            assertThat(jsonNode.get("readContent").asBoolean()).isEqualTo(expectedReadContent);
            assertThat(jsonNode.get("overrideContent").asBoolean()).isEqualTo(expectedOverrideContent);
            for (var key : OLD_KEYS) {
                assertThat(jsonNode.has(key)).isFalse();
            }
        }

        @Test
        void impossible_to_parse_configuration() {
            // Given
            var v2Flow = buildV2Flow(
                """
                {
                  "%s" : "%s
                """
            );

            // When
            var result = cut.mapFlow(v2Flow);

            // Then
            assertThat(result.issues()).hasSize(1);
            assertThat(result.issues().iterator().next().message()).startsWith(MigrationWarnings.GROOVY_PARSE_ERROR);
            assertThat(result.issues().iterator().next().state()).isEqualTo(MigrationResult.State.IMPOSSIBLE);
        }

        @Test
        void bad_policy_configuration() {
            // Given
            var v2Flow = buildV2Flow(
                """
                {
                  "onRequestContentScript" : "%s",
                  "onRequestScript" : "%s",
                  "scope" : "REQUEST"
                }
                """.formatted(GROOVY_SCRIPT, GROOVY_SCRIPT)
            );

            // When
            var result = cut.mapFlow(v2Flow);

            // Then
            assertThat(result.issues()).hasSize(1);
            assertThat(result.issues().iterator().next().message()).startsWith(MigrationWarnings.GROOVY_MULTIPLE_SCRIPTS);
            assertThat(result.issues().iterator().next().state()).isEqualTo(MigrationResult.State.IMPOSSIBLE);
        }

        private Flow buildV2Flow(String groovyPolicyCfg) {
            var v2Flow = new Flow();
            var step = new Step();
            step.setPolicy("groovy");
            step.setConfiguration(groovyPolicyCfg);
            v2Flow.setPre(List.of(step));
            return v2Flow;
        }
    }
}
