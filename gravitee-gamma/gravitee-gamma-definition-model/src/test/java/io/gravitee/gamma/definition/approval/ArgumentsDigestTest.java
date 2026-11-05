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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ArgumentsDigestTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String TOOL = "refund_order";

    private static JsonNode tree(Object arguments) {
        return JSON.valueToTree(arguments);
    }

    private static Map<String, Object> ordered(Object... keysAndValues) {
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    @Test
    void ignores_key_order_at_every_depth() {
        var first = ordered("amount", 120, "order", ordered("id", "o-1", "lines", List.of(1, 2)));
        var second = ordered("order", ordered("lines", List.of(1, 2), "id", "o-1"), "amount", 120);

        assertThat(ArgumentsDigest.of(TOOL, tree(first))).isEqualTo(ArgumentsDigest.of(TOOL, tree(second)));
    }

    @Test
    void ignores_how_a_number_is_written() {
        var plain = ArgumentsDigest.of(TOOL, tree(Map.of("amount", 120)));

        assertThat(ArgumentsDigest.of(TOOL, tree(Map.of("amount", new BigDecimal("120.00"))))).isEqualTo(plain);
        assertThat(ArgumentsDigest.of(TOOL, tree(Map.of("amount", new BigDecimal("1.2E2"))))).isEqualTo(plain);
        assertThat(ArgumentsDigest.of(TOOL, tree(Map.of("amount", 120.0)))).isEqualTo(plain);
    }

    @Test
    void digests_a_huge_exponent_without_expanding_it() {
        var huge = tree(Map.of("amount", new BigDecimal("1e10000000")));

        var digest = assertTimeoutPreemptively(Duration.ofSeconds(1), () -> ArgumentsDigest.of(TOOL, huge));

        assertThat(digest).isNotEqualTo(ArgumentsDigest.of(TOOL, tree(Map.of("amount", new BigDecimal("1e10000001")))));
    }

    @Test
    void tells_apart_a_number_and_its_text_other_values_another_tool_and_another_array_order() {
        var digest = ArgumentsDigest.of(TOOL, tree(Map.of("amount", 120)));

        assertThat(ArgumentsDigest.of(TOOL, tree(Map.of("amount", "120")))).isNotEqualTo(digest);
        assertThat(ArgumentsDigest.of(TOOL, tree(Map.of("amount", 121)))).isNotEqualTo(digest);
        assertThat(ArgumentsDigest.of("cancel_order", tree(Map.of("amount", 120)))).isNotEqualTo(digest);
        assertThat(ArgumentsDigest.of(TOOL, tree(Map.of("ids", List.of(1, 2))))).isNotEqualTo(
            ArgumentsDigest.of(TOOL, tree(Map.of("ids", List.of(2, 1))))
        );
    }

    @Test
    void refuses_a_call_without_arguments_or_with_a_number_no_json_text_holds() {
        assertThatThrownBy(() -> ArgumentsDigest.of(TOOL, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("arguments");
        assertThatThrownBy(() -> ArgumentsDigest.of(null, tree(Map.of())))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("toolName");
        assertThatThrownBy(() -> ArgumentsDigest.of(TOOL, tree(Map.of("amount", Double.POSITIVE_INFINITY))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("finite");
        assertThatThrownBy(() -> ArgumentsDigest.of(TOOL, JSON.createObjectNode().put("amount", Float.NaN)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("finite");
    }

    @Test
    void is_the_lowercase_hex_sha256_of_the_canonical_call_that_aim_has_always_stored() {
        var arguments = new LinkedHashMap<String, Object>();
        arguments.put("note", null);
        arguments.put("express", true);

        // sha256 of {"name":"refund_order","arguments":{"express":true,"note":null}}
        assertThat(ArgumentsDigest.of(TOOL, tree(arguments))).isEqualTo("9cb2dd1e942bad79c0d7b82f0a7eb2203f49909a0678cc965cfba911ea639fed");
    }

    @Test
    void refuses_text_that_is_not_valid_unicode_rather_than_mixing_it_up_with_other_text() {
        assertThatThrownBy(() -> ArgumentsDigest.of(TOOL, tree(Map.of("note", "\uD800"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unicode");
    }

    @Test
    void tells_a_null_value_from_a_missing_key_and_keeps_non_ascii_text_apart() {
        var withNull = new LinkedHashMap<String, Object>();
        withNull.put("note", null);

        assertThat(ArgumentsDigest.of(TOOL, tree(withNull))).isNotEqualTo(ArgumentsDigest.of(TOOL, tree(Map.of())));
        assertThat(ArgumentsDigest.of(TOOL, tree(Map.of("note", "café")))).isEqualTo(
            ArgumentsDigest.of(TOOL, tree(Map.of("note", "café")))
        );
        assertThat(ArgumentsDigest.of(TOOL, tree(Map.of("note", "café")))).isNotEqualTo(
            ArgumentsDigest.of(TOOL, tree(Map.of("note", "cafe")))
        );
    }
}
