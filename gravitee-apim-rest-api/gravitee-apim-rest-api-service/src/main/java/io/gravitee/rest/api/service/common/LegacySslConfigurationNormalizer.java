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
package io.gravitee.rest.api.service.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import lombok.experimental.UtilityClass;

/**
 * Normalizes the legacy representation of the SSL "None" keyStore/trustStore discriminator.
 * <p>
 * Configurations written before gravitee-plugin-common-configurations 1.2.3 persist that
 * discriminator as an empty string, while the shared SSL form schema now pins the "None"
 * branch on {@code const: "NONE"}. The empty string therefore matches no {@code oneOf}
 * branch, which makes both the generated form and the JSON schema validation reject it.
 * <p>
 * Both representations are equivalent for the gateway, which maps {@code ""} to
 * {@code NONE} when deserializing.
 */
@UtilityClass
public class LegacySslConfigurationNormalizer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String NONE = "NONE";

    /**
     * Plugins whose configuration embeds the shared SSL options schema, and whose definitions may
     * therefore carry the legacy empty-string discriminator. Plugins declaring their own SSL schema
     * (kafka, native-kafka) never used that representation and must be left untouched, since
     * normalization drops the sibling fields of the store it rewrites.
     */
    private static final Set<String> SHARED_SSL_SCHEMA_TYPES = Set.of(
        "http-proxy",
        "tcp-proxy",
        "mcp-proxy",
        "llm-proxy",
        "a2a-proxy",
        "http-dynamic-properties"
    );

    /**
     * Normalizes the given configuration when the plugin is known to embed the shared SSL options
     * schema, and returns it untouched otherwise.
     */
    public static String normalizeLegacySslNoneValues(String pluginType, String configuration) {
        return SHARED_SSL_SCHEMA_TYPES.contains(pluginType) ? normalizeLegacySslNoneValues(configuration) : configuration;
    }

    private static String normalizeLegacySslNoneValues(String sharedConfiguration) {
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
