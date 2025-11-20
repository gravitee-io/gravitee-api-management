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
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonNodeUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String asTextOrNull(JsonNode jsonNode) {
        return jsonNode != null ? jsonNode.asText() : null;
    }

    /**
     * This method returns the jsonNode elements as a Map<String, String>.
     * If the jsonNode is null, or the elements are not all String pairs of key-values, the result will be null.
     *
     * @param jsonNode
     * @return Map<String, String>
     */
    public static Map<String, String> asMapOrNull(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }
        try {
            return MAPPER.convertValue(jsonNode, new TypeReference<>() {});
        } catch (IllegalArgumentException e) {
            return null;
        }
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
