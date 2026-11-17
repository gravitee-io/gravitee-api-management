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
package io.gravitee.gamma.rest.core.guardian.domain_service;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import java.util.Optional;

/**
 * What a step and a resource have in common once their plugin is known: a configuration, read and
 * compared as a JSON object. Kept in one place so the two are judged by the same rules.
 *
 * @author GraviteeSource Team
 */
final class ConfigurationDocuments {

    /** What a step or a resource carrying no configuration is judged as, so a schema requiring a property refuses it. */
    private static final String EMPTY = "{}";

    private ConfigurationDocuments() {}

    /** The configuration to judge: the one given, or an empty object when there is none. */
    static String orEmpty(String configuration) {
        return configuration == null || configuration.isBlank() ? EMPTY : configuration;
    }

    /** The document as a JSON object, or nothing when it is not one. */
    static Optional<JsonNode> asObject(JsonDeserializer jsonDeserializer, String configuration) {
        try {
            JsonNode node = jsonDeserializer.deserialize(configuration, JsonNode.class);
            return node != null && node.isObject() ? Optional.of(node) : Optional.empty();
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    /**
     * Compared as JSON rather than as text: the validator rebuilds the document it returns, so the same
     * configuration may come back with its properties in another order or spaced differently, and a
     * deployment refusing that difference would refuse a draft nothing is wrong with.
     */
    static boolean isSame(JsonDeserializer jsonDeserializer, String accepted, JsonNode judged) {
        return asObject(jsonDeserializer, accepted).filter(judged::equals).isPresent();
    }

    /** The refusals are joined into one message, so each has to read as a sentence of its own. */
    static String asSentence(String message) {
        return message.endsWith(".") ? message : message + ".";
    }
}
