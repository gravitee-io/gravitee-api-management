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
package io.gravitee.gateway.reactor.environmentflow;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.v4.environmentflow.EnvironmentFlow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gateway.reactive.reactor.environmentflow.ReactableEnvironmentFlow;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ReactableEnvironmentFlowTest {

    @Test
    void should_return_empty_dependencies_resource_type() {
        final ReactableEnvironmentFlow environmentFlow = new ReactableEnvironmentFlow();
        final Set<Resource> dependencies = environmentFlow.dependencies(Resource.class);
        assertThat(dependencies).isEmpty();
    }

    @Test
    void should_return_empty_policy_dependencies_definition_null() {
        final ReactableEnvironmentFlow environmentFlow = new ReactableEnvironmentFlow();
        final Set<Policy> dependencies = environmentFlow.dependencies(Policy.class);
        assertThat(dependencies).isEmpty();
    }

    @Test
    void should_return_empty_policy_dependencies_definition_policies_null() {
        final ReactableEnvironmentFlow environmentFlow = new ReactableEnvironmentFlow();
        environmentFlow.setDefinition(EnvironmentFlow.builder().policies(null).build());
        final Set<Policy> dependencies = environmentFlow.dependencies(Policy.class);
        assertThat(dependencies).isEmpty();
    }

    @Test
    void should_return_empty_policy_dependencies_definition_policies_empty() {
        final ReactableEnvironmentFlow environmentFlow = new ReactableEnvironmentFlow();
        environmentFlow.setDefinition(EnvironmentFlow.builder().policies(List.of()).build());
        final Set<Policy> dependencies = environmentFlow.dependencies(Policy.class);
        assertThat(dependencies).isEmpty();
    }

    @Test
    void should_return_policy_dependencies() {
        final ReactableEnvironmentFlow environmentFlow = new ReactableEnvironmentFlow();
        environmentFlow.setDefinition(
            EnvironmentFlow
                .builder()
                .policies(
                    List.of(
                        Step.builder().policy("policy-enabled").enabled(true).build(),
                        Step.builder().policy("policy-disabled").enabled(false).build(),
                        Step.builder().policy("another-policy-enabled").enabled(true).build()
                    )
                )
                .build()
        );

        final Set<Policy> dependencies = environmentFlow.dependencies(Policy.class);
        assertThat(dependencies)
            .hasSize(2)
            .extracting(Policy::getName)
            .containsExactlyInAnyOrder("policy-enabled", "another-policy-enabled");
    }
}
