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
package io.gravitee.definition.model.v4;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.definition.ApiDefinitionFixtures;
import fixtures.definition.FlowFixtures;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    void should_round_trip_the_internal_flow() throws Exception {
        // Given
        var api = ApiDefinitionFixtures.anApiV4();
        api.setInternalFlow(anInternalFlow());

        // When
        var json = objectMapper.writeValueAsString(api);
        var read = objectMapper.readValue(json, Api.class);

        // Then
        assertThat(json).contains("\"internalFlow\":");
        assertThat(read.getInternalFlow()).isEqualTo(api.getInternalFlow());
    }

    @Test
    void should_copy_the_internal_flow() {
        // Given
        var api = ApiDefinitionFixtures.anApiV4();
        api.setInternalFlow(anInternalFlow());

        // When
        var copy = new Api(api);

        // Then
        assertThat(copy.getInternalFlow()).isEqualTo(api.getInternalFlow());
    }

    private static Flow anInternalFlow() {
        Flow flow = FlowFixtures.aProxyFlowV4();
        flow.setRequest(List.of(Step.builder().policy("guardian-policy").configuration("{\"guardianId\":\"guardian-id\"}").build()));
        return flow;
    }
}
