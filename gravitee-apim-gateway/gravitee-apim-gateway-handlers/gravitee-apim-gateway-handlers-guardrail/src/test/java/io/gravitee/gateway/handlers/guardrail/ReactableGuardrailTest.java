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
package io.gravitee.gateway.handlers.guardrail;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardrail.Guardrail;
import io.gravitee.definition.model.v4.resource.Resource;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ReactableGuardrailTest {

    @Test
    void should_be_built_from_its_definition() {
        Guardrail definition = Guardrail.builder().id("guardrail-id").name("guardrail-name").environmentId("env-id").build();

        ReactableGuardrail reactable = new ReactableGuardrail(definition);

        assertThat(reactable.getId()).isEqualTo("guardrail-id");
        assertThat(reactable.getName()).isEqualTo("guardrail-name");
        assertThat(reactable.getEnvironmentId()).isEqualTo("env-id");
        assertThat(reactable.getDefinition()).isSameAs(definition);
        assertThat(reactable.enabled()).isTrue();
    }

    @Test
    void should_return_empty_dependencies_for_unknown_type() {
        assertThat(new ReactableGuardrail().dependencies(String.class)).isEmpty();
    }

    @Test
    void should_return_empty_resource_dependencies_when_definition_or_resources_are_missing() {
        assertThat(new ReactableGuardrail().dependencies(Resource.class)).isEmpty();
        assertThat(
            ReactableGuardrail.builder().definition(Guardrail.builder().resources(null).build()).build().dependencies(Resource.class)
        ).isEmpty();
    }

    @Test
    void should_return_resources_as_dependencies() {
        Resource cache = Resource.builder().name("cache").type("cache").build();
        Resource disabled = Resource.builder().name("disabled").type("oauth2").enabled(false).build();
        ReactableGuardrail reactable = ReactableGuardrail.builder()
            .definition(Guardrail.builder().resources(List.of(cache, disabled)).build())
            .build();

        // The resource manager skips the disabled ones, as it does for an API.
        assertThat(reactable.dependencies(Resource.class)).containsExactlyInAnyOrder(cache, disabled);
    }

    @Test
    void should_return_empty_policy_dependencies_when_definition_or_policies_are_missing() {
        assertThat(new ReactableGuardrail().dependencies(Policy.class)).isEmpty();
        assertThat(
            ReactableGuardrail.builder().definition(Guardrail.builder().policies(null).build()).build().dependencies(Policy.class)
        ).isEmpty();
        assertThat(
            ReactableGuardrail.builder().definition(Guardrail.builder().policies(List.of()).build()).build().dependencies(Policy.class)
        ).isEmpty();
    }

    @Test
    void should_return_enabled_policies_as_dependencies() {
        ReactableGuardrail reactable = ReactableGuardrail.builder()
            .definition(
                Guardrail.builder()
                    .policies(
                        List.of(
                            Step.builder().policy("policy-enabled").configuration("{}").enabled(true).build(),
                            Step.builder().policy("policy-disabled").enabled(false).build(),
                            Step.builder().policy("another-policy-enabled").enabled(true).build()
                        )
                    )
                    .build()
            )
            .build();

        assertThat(reactable.dependencies(Policy.class))
            .extracting(Policy::getName)
            .containsExactlyInAnyOrder("policy-enabled", "another-policy-enabled");
    }

    @Test
    void should_identify_a_deployment_by_id_and_environment_only() {
        ReactableGuardrail deployed = new ReactableGuardrail(
            Guardrail.builder().id("guardrail-id").environmentId("env-id").name("v1").build()
        );
        ReactableGuardrail redeployed = ReactableGuardrail.builder()
            .definition(Guardrail.builder().id("guardrail-id").environmentId("env-id").name("v2").build())
            .deployedAt(new Date())
            .revision("2")
            .build();
        ReactableGuardrail otherEnvironment = new ReactableGuardrail(
            Guardrail.builder().id("guardrail-id").environmentId("other-env").name("v1").build()
        );

        assertThat(deployed).isEqualTo(redeployed).hasSameHashCodeAs(redeployed).isNotEqualTo(otherEnvironment);
        assertThat(deployed.key()).isEqualTo(new GuardrailKey("guardrail-id", "env-id")).isEqualTo(redeployed.key());
    }
}
