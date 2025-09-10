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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonNodeUtils {

    public static String asTextOrNull(JsonNode jsonNode) {
        return jsonNode != null ? jsonNode.asText() : null;
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
