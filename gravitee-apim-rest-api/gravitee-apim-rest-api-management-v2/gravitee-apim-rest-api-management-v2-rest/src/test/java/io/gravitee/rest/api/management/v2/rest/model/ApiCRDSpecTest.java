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
package io.gravitee.rest.api.management.v2.rest.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.rest.api.management.v2.rest.provider.ObjectMapperResolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ApiCRDSpecTest {

    private final ObjectMapper importMapper = new ObjectMapperResolver().getContext(ApiCRDSpec.class);

    @ParameterizedTest
    @CsvSource({ "DEFAULT, DEFAULT", "default, DEFAULT", "BEST_MATCH, BEST_MATCH", "best_match, BEST_MATCH", "best-match, BEST_MATCH" })
    void should_read_the_flow_execution_mode_as_the_enum_name_or_its_label(String mode, FlowMode expected) throws JsonProcessingException {
        var spec = importMapper.readValue("{\"flowExecution\":{\"mode\":\"" + mode + "\",\"matchRequired\":true}}", ApiCRDSpec.class);

        assertThat(spec.getFlowExecution().getMode()).isEqualTo(expected);
        assertThat(spec.getFlowExecution().isMatchRequired()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "\"x\"", "[]" })
    void should_refuse_a_flow_execution_that_is_not_an_object(String flowExecution) {
        assertThatThrownBy(() -> importMapper.readValue("{\"flowExecution\":" + flowExecution + "}", ApiCRDSpec.class)).isInstanceOf(
            MismatchedInputException.class
        );
    }
}
