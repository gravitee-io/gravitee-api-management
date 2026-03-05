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
import io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionDescriptor;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsFinder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SharedPolicyGroupDefinitionSecretRefsFinderTest {

    DefinitionSecretRefsFinder<SharedPolicyGroup> underTest = new SharedPolicyGroupDefinitionSecretRefsFinder();

    @Test
    void should_can_handle() {
        assertThat(underTest.canHandle(null)).isFalse();
        assertThat(underTest.canHandle(new Api())).isFalse();
        assertThat(underTest.canHandle(new SharedPolicyGroup())).isTrue();
    }

    @Test
    void should_get_definition_descriptor() {
        SharedPolicyGroup spg = new SharedPolicyGroup();
        spg.setId("spg-123");
        assertThat(underTest.toDefinitionDescriptor(spg, new DefinitionMetadata(null))).isEqualTo(
            new DefinitionDescriptor(new Definition("shared-policy-group", "spg-123"), Optional.empty())
        );
        assertThat(underTest.toDefinitionDescriptor(spg, new DefinitionMetadata("v2"))).isEqualTo(
            new DefinitionDescriptor(new Definition("shared-policy-group", "spg-123"), Optional.of("v2"))
        );
    }

    @Test
    void should_not_fail_with_null_or_empty_policies() {
        SharedPolicyGroup withNull = new SharedPolicyGroup();
        withNull.setPolicies(null);
        assertThatCode(() ->
            underTest.findSecretRefs(withNull, (config, location, setter) -> setter.accept(config))
        ).doesNotThrowAnyException();

        SharedPolicyGroup withEmpty = new SharedPolicyGroup();
        withEmpty.setPolicies(List.of());
        assertThatCode(() ->
            underTest.findSecretRefs(withEmpty, (config, location, setter) -> setter.accept(config))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_find_secrets_in_policies() {
        String config1 = "policy1-config";
        String policy1 = "policy1-type";
        String config2 = "policy2-config";
        String policy2 = "policy2-type";

        SharedPolicyGroup spg = new SharedPolicyGroup();
        spg.setPolicies(List.of(newStep(config1, policy1), newStep(config2, policy2)));

        Set<String> actualConfigs = new HashSet<>();
        Set<String> actualLocations = new HashSet<>();

        underTest.findSecretRefs(spg, (config, location, setter) -> {
            actualConfigs.add(config);
            actualLocations.add(location.id());
            setter.accept(processed(config));
        });

        assertThat(actualConfigs).containsExactlyInAnyOrder(config1, config2);
        assertThat(actualLocations).containsExactlyInAnyOrder(policy1, policy2);

        // Verify setter worked
        assertThat(spg.getPolicies().get(0).getConfiguration()).isEqualTo(processed(config1));
        assertThat(spg.getPolicies().get(1).getConfiguration()).isEqualTo(processed(config2));
    }

    private static Step newStep(String configuration, String policy) {
        Step step = new Step();
        step.setConfiguration(configuration);
        step.setPolicy(policy);
        return step;
    }

    private String processed(String original) {
        return original.concat(" - updated!");
    }
}
