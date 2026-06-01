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
package io.gravitee.gateway.handlers.sharedpolicygroup.reactor.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultSharedPolicyGroupReactorTest {

    @ParameterizedTest
    @MethodSource("providePhases")
    void should_map_shared_policy_group_phase_to_execution_phase(SharedPolicyGroup.Phase phase, ExecutionPhase expectedPhase) {
        assertThat(DefaultSharedPolicyGroupReactor.toExecutionPhase(phase)).isEqualTo(expectedPhase);
    }

    private static Stream<Arguments> providePhases() {
        return Stream.of(
            Arguments.of(SharedPolicyGroup.Phase.REQUEST, ExecutionPhase.REQUEST),
            Arguments.of(SharedPolicyGroup.Phase.RESPONSE, ExecutionPhase.RESPONSE),
            Arguments.of(SharedPolicyGroup.Phase.PUBLISH, ExecutionPhase.MESSAGE_REQUEST),
            Arguments.of(SharedPolicyGroup.Phase.SUBSCRIBE, ExecutionPhase.MESSAGE_RESPONSE),
            Arguments.of(SharedPolicyGroup.Phase.MESSAGE_REQUEST, ExecutionPhase.MESSAGE_REQUEST),
            Arguments.of(SharedPolicyGroup.Phase.MESSAGE_RESPONSE, ExecutionPhase.MESSAGE_RESPONSE)
        );
    }
}
