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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Identifies a held call by its tool and arguments, the same way on the gateway and in AIM: calling again with the same
 * arguments finds the same approval, and other arguments never ride on it. Key order, whitespace and how a number is
 * written do not change it.
 */
public final class ArgumentsDigest {

    private ArgumentsDigest() {}

    /**
     * The lowercase hex SHA-256 of {@code {"name": toolName, "arguments": arguments}} in canonical form. Read
     * {@code arguments} with {@code USE_BIG_DECIMAL_FOR_FLOATS}, as the gateway and AIM do: a {@code double} keeps
     * fewer digits than the JSON text, and the digest would then differ from theirs.
     *
     * @throws IllegalArgumentException when a number is not finite, which no JSON text holds, or a text is not valid Unicode
     */
    public static String of(String toolName, JsonNode arguments) {
        Objects.requireNonNull(toolName, "toolName");
        Objects.requireNonNull(arguments, "arguments");
        ObjectNode call = JsonNodeFactory.instance.objectNode();
        call.put("name", toolName);
        call.set("arguments", canonical(arguments));
        return Sha256.hex(call.toString());
    }

    private static JsonNode canonical(JsonNode node) {
        if (node.isObject()) {
            Map<String, JsonNode> sorted = new TreeMap<>();
            node.properties().forEach(field -> sorted.put(field.getKey(), canonical(field.getValue())));
            ObjectNode object = JsonNodeFactory.instance.objectNode();
            sorted.forEach(object::set);
            return object;
        }
        if (node.isArray()) {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            node.forEach(element -> array.add(canonical(element)));
            return array;
        }
        if (node.isNumber()) {
            if ((node.isDouble() || node.isFloat()) && !Double.isFinite(node.doubleValue())) {
                throw new IllegalArgumentException("An argument is a number that is not finite: " + node.doubleValue());
            }
            // Not the plain string: 1e10000000 would expand to ten million digits.
            return JsonNodeFactory.instance.numberNode(node.decimalValue().stripTrailingZeros());
        }
        return node;
    }
}
