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
package io.gravitee.apim.core.shared_policy_group.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.plugin.model.FlowPhase;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SharedPolicyGroupTest {

    @ParameterizedTest
    @MethodSource("providePhases")
    void should_map_flow_phase_to_definition_phase(
        FlowPhase flowPhase,
        io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.Phase expectedPhase
    ) {
        var sharedPolicyGroup = SharedPolicyGroup.builder()
            .crossId("cross-id")
            .environmentId("environment-id")
            .name("name")
            .phase(flowPhase)
            .steps(List.of())
            .version(1)
            .deployedAt(ZonedDateTime.now())
            .build();

        var definition = sharedPolicyGroup.toDefinition();

        assertThat(definition.getPhase()).isEqualTo(expectedPhase);
    }

    private static Stream<Arguments> providePhases() {
        return Stream.of(
            Arguments.of(FlowPhase.REQUEST, io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.Phase.REQUEST),
            Arguments.of(FlowPhase.RESPONSE, io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.Phase.RESPONSE),
            Arguments.of(FlowPhase.PUBLISH, io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.Phase.MESSAGE_REQUEST),
            Arguments.of(FlowPhase.SUBSCRIBE, io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup.Phase.MESSAGE_RESPONSE)
        );
    }
}
