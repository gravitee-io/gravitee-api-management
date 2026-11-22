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
package io.gravitee.gamma.definition.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalArgumentsTest {

    private static final int ENVELOPE_BYTES = "{\"text\":\"\"}".length();

    @Test
    void accepts_arguments_that_fill_the_limit_exactly() {
        JsonNode arguments = textOfBytes(ApprovalArguments.MAX_BYTES - ENVELOPE_BYTES);

        assertThat(ApprovalArguments.sizeInBytes(arguments)).isEqualTo(ApprovalArguments.MAX_BYTES);
        assertThat(ApprovalArguments.isTooLarge(arguments)).isFalse();
    }

    @Test
    void refuses_arguments_one_byte_over_the_limit() {
        JsonNode arguments = textOfBytes(ApprovalArguments.MAX_BYTES - ENVELOPE_BYTES + 1);

        assertThat(ApprovalArguments.isTooLarge(arguments)).isTrue();
    }

    @Test
    void counts_bytes_not_characters() {
        JsonNode arguments = JsonNodeFactory.instance.objectNode().put("text", "é".repeat(ApprovalArguments.MAX_BYTES / 2));

        assertThat(ApprovalArguments.sizeInBytes(arguments)).isGreaterThan(ApprovalArguments.MAX_BYTES);
        assertThat(ApprovalArguments.isTooLarge(arguments)).isTrue();
    }

    @Test
    void measures_a_float_as_the_json_text_holds_it_when_read_with_big_decimals() throws Exception {
        String json = "{\"p\":0.12345678901234567891}";
        JsonNode exact = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(json);
        JsonNode approximate = new ObjectMapper().readTree(json);

        assertThat(ApprovalArguments.sizeInBytes(exact)).isEqualTo(json.length());
        assertThat(ApprovalArguments.sizeInBytes(approximate)).isLessThan(json.length());
    }

    @Test
    void names_the_missing_arguments_when_a_call_carries_none() {
        assertThatNullPointerException()
            .isThrownBy(() -> ApprovalArguments.isTooLarge(null))
            .withMessage("arguments");
        assertThatNullPointerException()
            .isThrownBy(() -> ApprovalArguments.sizeInBytes(null))
            .withMessage("arguments");
    }

    private static JsonNode textOfBytes(int bytes) {
        return JsonNodeFactory.instance.objectNode().put("text", "a".repeat(bytes));
    }
}
