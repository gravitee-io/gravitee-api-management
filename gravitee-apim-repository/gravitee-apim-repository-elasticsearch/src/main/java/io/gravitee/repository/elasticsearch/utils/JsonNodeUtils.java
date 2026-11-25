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
package io.gravitee.repository.elasticsearch.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;

@CustomLog
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonNodeUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String asTextOrNull(JsonNode jsonNode) {
        return jsonNode != null ? jsonNode.asText() : null;
    }

    /**
     * Converts a {@link JsonNode} to a {@link Map} where keys are strings and values are of type {@code T}.
     * If the input is {@code null} or cannot be converted, it returns {@code null}.
     *
     * @param jsonNode the {@link JsonNode} to convert; may be {@code null}
     * @return a {@link Map} representation of the {@link JsonNode}, or {@code null} if the input
     *         is {@code null} or cannot be converted
     */
    public static <T> Map<String, T> asMapOrNull(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }
        try {
            return MAPPER.convertValue(jsonNode, new TypeReference<>() {});
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Reads an object of keyword values, such as the {@code custom} metrics of a request, as a flat map of texts in
     * document order, the way {@link #putFlattened} walks it. What it leaves out is logged at debug level with the id
     * of the document.
     *
     * @return the flat map, or {@code null} when the node is absent or not an object
     */
    public static Map<String, String> asFlatTextMapOrNull(JsonNode jsonNode, String documentId) {
        if (jsonNode == null || !jsonNode.isObject()) {
            return null;
        }
        var flat = new LinkedHashMap<String, String>();
        var leftOut = putFlattened(jsonNode, "", flat);
        if (!leftOut.isEmpty()) {
            log.debug("Document {}: {} left out of the flattened object (a null value, or a name written twice)", documentId, leftOut);
        }
        return Collections.unmodifiableMap(flat);
    }

    /**
     * Copies the members of a JSON object into a flat map of texts. A nested object is walked into dotted names
     * ({@code {"order": {"channel": "web"}}} gives {@code order.channel}); any other value is read with
     * {@link JsonNode#asText()}, so an array reads as an empty text; a {@code null} value is skipped. When two members
     * flatten to the same name (a dotted name and the same name as a nested object), the last one read wins.
     *
     * @param prefix the dotted name of {@code object} itself, empty at the root
     * @return the names left out: the skipped {@code null} values and the names written twice
     */
    public static List<String> putFlattened(JsonNode object, String prefix, Map<String, String> into) {
        var leftOut = new ArrayList<String>();
        putFlattened(object, prefix, into, leftOut);
        return leftOut;
    }

    private static void putFlattened(JsonNode object, String prefix, Map<String, String> into, List<String> leftOut) {
        object
            .properties()
            .forEach(member -> {
                var name = prefix.isEmpty() ? member.getKey() : prefix + "." + member.getKey();
                var value = member.getValue();
                if (value.isObject()) {
                    putFlattened(value, name, into, leftOut);
                } else if (value.isNull()) {
                    leftOut.add(name);
                } else if (into.put(name, value.asText()) != null) {
                    leftOut.add(name);
                }
            });
    }

    public static int asIntOr(JsonNode jsonNode, int defaultValue) {
        return jsonNode != null ? jsonNode.asInt() : defaultValue;
    }

    public static long asLongOr(JsonNode jsonNode, long defaultValue) {
        return jsonNode != null ? jsonNode.asLong() : defaultValue;
    }

    public static boolean asBooleanOrFalse(JsonNode jsonNode) {
        return jsonNode != null && jsonNode.asBoolean();
    }
}
