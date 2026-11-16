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
package io.gravitee.gamma.rest.core.gravitee_plugin.model;

import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.GUARDIAN;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.HTTP_PROXY;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.LLM_PROXY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeclaredFlowsTest {

    private static final DeclaredFlows GUARDRAIL = new DeclaredFlows(
        Map.of(GUARDIAN, List.of("REQUEST", "RESPONSE"), LLM_PROXY, List.of("RESPONSE"))
    );

    @ParameterizedTest(name = "{0}")
    @MethodSource("requests")
    void should_tell_whether_the_flows_declare_the_requested_flows_and_phase(
        String description,
        Set<PolicyFlowType> flows,
        String phase,
        boolean expected
    ) {
        assertThat(GUARDRAIL.declares(flows, phase)).as(description).isEqualTo(expected);
    }

    static Stream<Arguments> requests() {
        return Stream.of(
            Arguments.of("no flow nor phase requested", Set.of(), null, true),
            Arguments.of("a declared flow", Set.of(GUARDIAN), null, true),
            Arguments.of("an absent flow", Set.of(HTTP_PROXY), null, false),
            Arguments.of("declared and absent flows", Set.of(GUARDIAN, HTTP_PROXY), null, false),
            Arguments.of("a phase of every requested flow", Set.of(GUARDIAN, LLM_PROXY), "RESPONSE", true),
            Arguments.of("a phase missing from one requested flow", Set.of(GUARDIAN, LLM_PROXY), "REQUEST", false),
            Arguments.of("a phase of one flow, no flow requested", Set.of(), "REQUEST", true),
            Arguments.of("a phase of no flow, no flow requested", Set.of(), "INTERACT", false),
            Arguments.of("a phase compared with case", Set.of(), "request", false),
            Arguments.of("a blank phase filters nothing", Set.of(GUARDIAN), " ", true),
            Arguments.of("an empty phase filters nothing", Set.of(), "", true)
        );
    }
}
