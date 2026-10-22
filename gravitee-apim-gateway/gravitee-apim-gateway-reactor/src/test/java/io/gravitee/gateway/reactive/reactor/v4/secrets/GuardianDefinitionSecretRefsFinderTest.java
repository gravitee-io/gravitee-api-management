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
package io.gravitee.gateway.reactive.reactor.v4.secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionDescriptor;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsFinder;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianDefinitionSecretRefsFinderTest {

    DefinitionSecretRefsFinder<Guardian> underTest = new GuardianDefinitionSecretRefsFinder();

    @Test
    void should_only_handle_guardian_definitions() {
        assertThat(underTest.canHandle(null)).isFalse();
        assertThat(underTest.canHandle(new Api())).isFalse();
        assertThat(underTest.canHandle(new Guardian())).isTrue();
    }

    @Test
    void should_describe_definition_with_guardian_kind() {
        Guardian guardian = Guardian.builder().id("guardian-123").build();
        assertThat(underTest.toDefinitionDescriptor(guardian, new DefinitionMetadata(null))).isEqualTo(
            new DefinitionDescriptor(new Definition("guardian", "guardian-123"), Optional.empty())
        );
        assertThat(underTest.toDefinitionDescriptor(guardian, new DefinitionMetadata("v2"))).isEqualTo(
            new DefinitionDescriptor(new Definition("guardian", "guardian-123"), Optional.of("v2"))
        );
    }

    @Test
    void should_not_fail_with_null_or_empty_policies() {
        assertThatCode(() ->
            underTest.findSecretRefs(Guardian.builder().policies(null).build(), (config, location, setter) -> setter.accept(config))
        ).doesNotThrowAnyException();
        assertThatCode(() ->
            underTest.findSecretRefs(Guardian.builder().policies(List.of()).build(), (config, location, setter) -> setter.accept(config))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_find_secrets_in_policies() {
        Guardian guardian = Guardian.builder()
            .policies(List.of(newStep("policy1-config", "policy1-type"), newStep("policy2-config", "policy2-type")))
            .build();

        Set<String> actualConfigs = new HashSet<>();
        Set<String> actualLocations = new HashSet<>();
        underTest.findSecretRefs(guardian, (config, location, setter) -> {
            actualConfigs.add(config);
            actualLocations.add(location.id());
            setter.accept(config + " - updated!");
        });

        assertThat(actualConfigs).containsExactlyInAnyOrder("policy1-config", "policy2-config");
        assertThat(actualLocations).containsExactlyInAnyOrder("policy1-type", "policy2-type");
        assertThat(guardian.getPolicies().get(0).getConfiguration()).isEqualTo("policy1-config - updated!");
        assertThat(guardian.getPolicies().get(1).getConfiguration()).isEqualTo("policy2-config - updated!");
    }

    private static Step newStep(String configuration, String policy) {
        Step step = new Step();
        step.setConfiguration(configuration);
        step.setPolicy(policy);
        return step;
    }
}
