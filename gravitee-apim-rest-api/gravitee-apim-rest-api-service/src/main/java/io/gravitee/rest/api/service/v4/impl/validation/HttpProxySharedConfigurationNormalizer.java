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
package io.gravitee.rest.api.service.v4.impl.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.UtilityClass;

@UtilityClass
class HttpProxySharedConfigurationNormalizer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String NONE = "NONE";

    static String normalizeLegacySslNoneValues(String sharedConfiguration) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(sharedConfiguration);
            if (!(root.path("ssl") instanceof ObjectNode sslObject)) {
                return sharedConfiguration;
            }

            boolean normalized = normalizeLegacySslNoneValue(sslObject, "trustStore");
            normalized |= normalizeLegacySslNoneValue(sslObject, "keyStore");
            return normalized ? OBJECT_MAPPER.writeValueAsString(root) : sharedConfiguration;
        } catch (JsonProcessingException ignored) {
            return sharedConfiguration;
        }
    }

    private static boolean normalizeLegacySslNoneValue(ObjectNode sslObject, String storeName) {
        JsonNode store = sslObject.get(storeName);
        if (!(store instanceof ObjectNode storeObject)) {
            return false;
        }

        JsonNode type = storeObject.get("type");
        if (type == null || !type.isTextual() || !type.asText().isEmpty()) {
            return false;
        }

        storeObject.removeAll();
        storeObject.put("type", NONE);
        return true;
    }
}
