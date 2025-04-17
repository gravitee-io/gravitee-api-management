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
package io.gravitee.apim.core.api.domain_service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import fixtures.definition.ApiDefinitionFixtures;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.domain_service.policy.PolicyValidationDomainServiceLegacyWrapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.debug.DebugApiV2;
import io.gravitee.definition.model.debug.DebugApiV4;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.plugin.core.api.PluginMoreInformation;
import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import io.gravitee.rest.api.model.v4.policy.ApiProtocolType;
import io.gravitee.rest.api.model.v4.policy.PolicyPluginEntity;
import io.gravitee.rest.api.service.v4.PolicyPluginService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiPolicyValidatorDomainServiceTest {

    private static final String API_ID = "api-id";
    private ApiPolicyValidatorDomainService cut;
    private final PolicyValidationDomainService policyValidationDomainService = new PolicyValidationDomainServiceLegacyWrapper(
        new SimpleValidationOnlyPolicyService()
    );

    @BeforeEach
    void setUp() {
        cut = new ApiPolicyValidatorDomainService(policyValidationDomainService);
    }

    @Nested
    class ApiV1 {

        @Test
        void should_not_validate_null_v1_api() {
            assertThatThrownBy(() -> cut.checkPolicyConfigurations(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Api should not be null");
        }

        @ParameterizedTest
        @MethodSource("provideEmptyCasesForPaths")
        void should_validate_v1_api_without_any_path(DebugApiV2 api, List<Plan> plans) {
            api.setDefinitionVersion(DefinitionVersion.V1);
            api.setPlans(plans);
            assertDoesNotThrow(() -> cut.checkPolicyConfigurations(api));
        }

        @Test
        void should_validate_paths() {
            var api = new DebugApiV2();
            api.setDefinitionVersion(DefinitionVersion.V1);
            api.setPaths(
                Map.of(
                    "path1",
                    List.of(buildRule("policy1", "config1")),
                    "path2",
                    List.of(buildRule("policy1", "config1"), buildRule("policy2", "config2")),
                    "path-no-rule",
                    List.of()
                )
            );
            api.setPlans(
                List.of(
                    Plan
                        .builder()
                        .id("plan1")
                        .paths(
                            Map.of(
                                "path1",
                                List.of(buildRule("policy1", "config1")),
                                "path2",
                                List.of(buildRule("policy1", "config1"), buildRule("policy2", "config2")),
                                "path-no-rule",
                                List.of()
                            )
                        )
                        .build(),
                    Plan
                        .builder()
                        .id("plan2")
                        .paths(
                            Map.of(
                                "path1",
                                List.of(buildRule("policy1", "config1")),
                                "path2",
                                List.of(buildRule("policy1", "config1"), buildRule("policy2", "config2")),
                                "path-no-rule",
                                List.of()
                            )
                        )
                        .build()
                )
            );

            cut.checkPolicyConfigurations(api);

            SoftAssertions.assertSoftly(softly -> {
                // Verify paths at api level
                softly
                    .assertThat(
                        api.getPaths().values().stream().flatMap(Collection::stream).map(rule -> rule.getPolicy().getConfiguration())
                    )
                    .hasSize(3)
                    .allMatch(configuration -> configuration.equals("validated"));
                // verify paths for plans
                softly
                    .assertThat(
                        api
                            .getPlans()
                            .stream()
                            .flatMap(plan -> plan.getPaths().values().stream().flatMap(Collection::stream))
                            .map(rule -> rule.getPolicy().getConfiguration())
                    )
                    .hasSize(6)
                    .allMatch(configuration -> configuration.equals("validated"));
            });
        }

        private Rule buildRule(String name, String configuration) {
            final Rule rule = new Rule();
            rule.setPolicy(Policy.builder().name(name).configuration(configuration).build());
            return rule;
        }

        private static Stream<Arguments> provideEmptyCasesForPaths() {
            var api = new DebugApiV2();
            api.setPaths(Map.of());
            return Stream.concat(provideEmptyCases(), Stream.of(Arguments.of(api, null), Arguments.of(api, List.of())));
        }
    }

    @Nested
    class ApiV2 {

        @Test
        void should_not_validate_null_v2_api() {
            assertThatThrownBy(() -> cut.checkPolicyConfigurations(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Api should not be null");
        }

        @ParameterizedTest
        @MethodSource("provideEmptyCasesForFlows")
        void should_validate_v2_api_without_any_flow(DebugApiV2 api) {
            api.setDefinitionVersion(DefinitionVersion.V2);
            assertDoesNotThrow(() -> cut.checkPolicyConfigurations(api));
        }

        @Test
        void should_validate_flows() {
            var api = new DebugApiV2();
            api.setDefinitionVersion(DefinitionVersion.V2);
            api.setFlows(
                List.of(
                    Flow
                        .builder()
                        .id("flow1")
                        .pre(
                            List.of(
                                Step.builder().policy("policy1").configuration("config1").build(),
                                Step.builder().policy("policy2").configuration("config2").build()
                            )
                        )
                        .post(List.of(Step.builder().policy("policy3").configuration("config3").build()))
                        .build(),
                    Flow.builder().build()
                )
            );
            api.setPlans(
                List.of(
                    Plan
                        .builder()
                        .id("plan1")
                        .flows(
                            List.of(
                                Flow
                                    .builder()
                                    .id("flow1")
                                    .pre(
                                        List.of(
                                            Step.builder().policy("policy1").configuration("config1").build(),
                                            Step.builder().policy("policy2").configuration("config2").build()
                                        )
                                    )
                                    .post(List.of(Step.builder().policy("policy3").configuration("config3").build()))
                                    .build()
                            )
                        )
                        .build(),
                    Plan
                        .builder()
                        .id("plan2")
                        .flows(
                            List.of(
                                Flow
                                    .builder()
                                    .id("flow1")
                                    .pre(
                                        List.of(
                                            Step.builder().policy("policy1").configuration("config1").build(),
                                            Step.builder().policy("policy2").configuration("config2").build()
                                        )
                                    )
                                    .post(List.of(Step.builder().policy("policy3").configuration("config3").build()))
                                    .build()
                            )
                        )
                        .build()
                )
            );

            cut.checkPolicyConfigurations(api);

            SoftAssertions.assertSoftly(softly -> {
                // Verify flows at api level
                softly
                    .assertThat(
                        api
                            .getFlows()
                            .stream()
                            .flatMap(flow -> Stream.concat(flow.getPre().stream(), flow.getPost().stream()))
                            .map(Step::getConfiguration)
                    )
                    .hasSize(3)
                    .allMatch(configuration -> configuration.equals("validated"));
                // verify paths for plans
                softly
                    .assertThat(
                        api
                            .getPlans()
                            .stream()
                            .flatMap(plan ->
                                plan.getFlows().stream().flatMap(flow -> Stream.concat(flow.getPre().stream(), flow.getPost().stream()))
                            )
                            .map(Step::getConfiguration)
                    )
                    .hasSize(6)
                    .allMatch(configuration -> configuration.equals("validated"));
            });
        }

        private static Stream<Arguments> provideEmptyCasesForFlows() {
            var api = new DebugApiV2();
            api.setFlows(List.of(Flow.builder().build()));
            return Stream.concat(provideEmptyCases(), Stream.of(Arguments.of(api, null), Arguments.of(api, List.of())));
        }
    }

    @Nested
    class ApiV4 {

        @Test
        void should_not_validate_null_v4_api() {
            assertThatThrownBy(() -> cut.checkPolicyConfigurations(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Api should not be null");
        }

        @ParameterizedTest
        @MethodSource("provideV4EmptyCasesForFlows")
        void should_validate_v4_api_without_any_flow(DebugApiV4 api) {
            assertDoesNotThrow(() -> cut.checkPolicyConfigurations(api));
        }

        @Test
        void should_validate_flows() {
            var api = new DebugApiV4(
                ApiDefinitionFixtures
                    .aHttpProxyApiV4(API_ID)
                    .toBuilder()
                    .flows(
                        List.of(
                            io.gravitee.definition.model.v4.flow.Flow
                                .builder()
                                .id("flow1")
                                .request(
                                    List.of(
                                        io.gravitee.definition.model.v4.flow.step.Step
                                            .builder()
                                            .policy("policy1")
                                            .configuration("config1")
                                            .build(),
                                        io.gravitee.definition.model.v4.flow.step.Step
                                            .builder()
                                            .policy("policy2")
                                            .configuration("config2")
                                            .build()
                                    )
                                )
                                .response(
                                    List.of(
                                        io.gravitee.definition.model.v4.flow.step.Step
                                            .builder()
                                            .policy("policy3")
                                            .configuration("config3")
                                            .build()
                                    )
                                )
                                .build(),
                            io.gravitee.definition.model.v4.flow.Flow.builder().build()
                        )
                    )
                    .plans(
                        Map.ofEntries(
                            Map.entry(
                                "plan1",
                                io.gravitee.definition.model.v4.plan.Plan
                                    .builder()
                                    .id("plan1")
                                    .flows(
                                        List.of(
                                            io.gravitee.definition.model.v4.flow.Flow
                                                .builder()
                                                .id("flow1")
                                                .request(
                                                    List.of(
                                                        io.gravitee.definition.model.v4.flow.step.Step
                                                            .builder()
                                                            .policy("policy1")
                                                            .configuration("config1")
                                                            .build(),
                                                        io.gravitee.definition.model.v4.flow.step.Step
                                                            .builder()
                                                            .policy("policy2")
                                                            .configuration("config2")
                                                            .build()
                                                    )
                                                )
                                                .response(
                                                    List.of(
                                                        io.gravitee.definition.model.v4.flow.step.Step
                                                            .builder()
                                                            .policy("policy3")
                                                            .configuration("config3")
                                                            .build()
                                                    )
                                                )
                                                .build()
                                        )
                                    )
                                    .build()
                            ),
                            Map.entry(
                                "plan2",
                                io.gravitee.definition.model.v4.plan.Plan
                                    .builder()
                                    .id("plan2")
                                    .flows(
                                        List.of(
                                            io.gravitee.definition.model.v4.flow.Flow
                                                .builder()
                                                .id("flow1")
                                                .request(
                                                    List.of(
                                                        io.gravitee.definition.model.v4.flow.step.Step
                                                            .builder()
                                                            .policy("policy1")
                                                            .configuration("config1")
                                                            .build(),
                                                        io.gravitee.definition.model.v4.flow.step.Step
                                                            .builder()
                                                            .policy("policy2")
                                                            .configuration("config2")
                                                            .build()
                                                    )
                                                )
                                                .response(
                                                    List.of(
                                                        io.gravitee.definition.model.v4.flow.step.Step
                                                            .builder()
                                                            .policy("policy3")
                                                            .configuration("config3")
                                                            .build()
                                                    )
                                                )
                                                .build()
                                        )
                                    )
                                    .build()
                            )
                        )
                    )
                    .build(),
                new HttpRequest("/", "GET")
            );

            cut.checkPolicyConfigurations(api);

            SoftAssertions.assertSoftly(softly -> {
                // Verify flows at api level
                softly
                    .assertThat(
                        api
                            .getApiDefinition()
                            .getFlows()
                            .stream()
                            .flatMap(flow ->
                                Stream.concat(
                                    Optional.ofNullable(flow.getRequest()).stream().flatMap(Collection::stream),
                                    Optional.ofNullable(flow.getResponse()).stream().flatMap(Collection::stream)
                                )
                            )
                            .map(io.gravitee.definition.model.v4.flow.step.Step::getConfiguration)
                    )
                    .hasSize(3)
                    .allMatch(configuration -> configuration.equals("validated"));
                // verify paths for plans
                softly
                    .assertThat(
                        Objects
                            .requireNonNull(api.getApiDefinition().getPlans())
                            .stream()
                            .flatMap(plan ->
                                plan
                                    .getFlows()
                                    .stream()
                                    .flatMap(flow -> Stream.concat(flow.getRequest().stream(), flow.getResponse().stream()))
                            )
                            .map(io.gravitee.definition.model.v4.flow.step.Step::getConfiguration)
                    )
                    .hasSize(6)
                    .allMatch(configuration -> configuration.equals("validated"));
            });
        }

        private static Stream<Arguments> provideV4EmptyCasesForFlows() {
            return Stream.of(
                Arguments.of(
                    new DebugApiV4(
                        ApiDefinitionFixtures.aHttpProxyApiV4(API_ID).toBuilder().flows(List.of()).build(),
                        new HttpRequest("/", "GET")
                    )
                ),
                Arguments.of(
                    new DebugApiV4(
                        ApiDefinitionFixtures.aHttpProxyApiV4(API_ID).toBuilder().flows(null).build(),
                        new HttpRequest("/", "GET")
                    )
                )
            );
        }
    }

    private static Stream<Arguments> provideEmptyCases() {
        return Stream.of(Arguments.of(new DebugApiV2(), null), Arguments.of(new DebugApiV2(), List.of()));
    }

    private static class SimpleValidationOnlyPolicyService implements PolicyPluginService {

        @Override
        public String validatePolicyConfiguration(String policyName, String configuration) {
            return "validated";
        }

        @Override
        public String validatePolicyConfiguration(PolicyPluginEntity policyPluginEntity, String configuration) {
            throw new IllegalStateException("should not be called");
        }

        @Override
        public String getSchema(String policyPluginId, ApiProtocolType apiProtocolType, SchemaDisplayFormat schemaDisplayFormat) {
            throw new IllegalStateException("should not be called");
        }

        @Override
        public String getDocumentation(String policyPluginId, ApiProtocolType apiProtocolType) {
            throw new IllegalStateException("should not be called");
        }

        @Override
        public Set<PolicyPluginEntity> findAll() {
            throw new IllegalStateException("should not be called");
        }

        @Override
        public PolicyPluginEntity findById(String plugin) {
            throw new IllegalStateException("should not be called");
        }

        @Override
        public String getSchema(String plugin) {
            throw new IllegalStateException("should not be called");
        }

        @Override
        public String getIcon(String plugin) {
            throw new IllegalStateException("should not be called");
        }

        @Override
        public String getDocumentation(String plugin) {
            throw new IllegalStateException("should not be called");
        }

        @Override
        public PluginMoreInformation getMoreInformation(String pluginId) {
            throw new IllegalStateException("should not be called");
        }
    }
}
