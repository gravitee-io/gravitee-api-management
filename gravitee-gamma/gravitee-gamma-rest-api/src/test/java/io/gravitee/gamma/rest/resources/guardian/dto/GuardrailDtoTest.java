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
package io.gravitee.gamma.rest.resources.guardian.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.gravitee.gamma.rest.core.guardian.model.Guardrail;
import java.io.IOException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/** A plugin manifest may declare no name: the documented contract and the serialized response must both treat it as optional. */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardrailDtoTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void should_leave_out_a_missing_name() {
        JsonNode guardrail = JSON.valueToTree(GuardrailDto.from(new Guardrail("data-logging-masking", null)));

        assertThat(guardrail.has("name")).as("a null name is omitted, not serialized as null").isFalse();
        assertThat(guardrail.get("id").asText()).isEqualTo("data-logging-masking");
    }

    @Test
    void should_document_only_the_id_as_required() throws IOException {
        try (var spec = GuardrailDtoTest.class.getResourceAsStream("/openapi/guardians/openapi-guardians.yaml")) {
            var required = new YAMLMapper().readTree(spec).at("/components/schemas/Guardrail/required");

            assertThat(required).as("Guardrail required properties").singleElement().extracting(JsonNode::asText).isEqualTo("id");
        }
    }
}
