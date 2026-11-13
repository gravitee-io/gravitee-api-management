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
package io.gravitee.gamma.rest.core.gravitee_plugin.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType;
import java.util.HashSet;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListPolicyPluginsUseCaseTest {

    @Test
    void should_read_absent_flows_as_no_flow_requested() {
        assertThat(new ListPolicyPluginsUseCase.Input("org", null, null).flows()).as("flows").isEmpty();
    }

    @Test
    void should_keep_its_own_copy_of_the_requested_flows() {
        var requested = new HashSet<>(Set.of(PolicyFlowType.GUARDIAN));

        var input = new ListPolicyPluginsUseCase.Input("org", requested, null);
        requested.add(PolicyFlowType.LLM_PROXY);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(input.flows()).as("flows").containsExactly(PolicyFlowType.GUARDIAN);
            softly.assertThat(input.flows()).as("unmodifiable flows").isUnmodifiable();
        });
    }
}
