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
package io.gravitee.rest.api.fetcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Tells whether two fetcher configurations point at the same place. A stored secret may only be reused by a
 * configuration that still connects to where the secret was meant to be sent.
 */
public final class FetcherConfigurationAddress {

    /** The fields in which the fetcher plugins carry the address they connect to. */
    private static final Pattern ADDRESS_FIELD = Pattern.compile("repository|.*[uU]rl");

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private FetcherConfigurationAddress() {}

    /** A configuration that cannot be read is never considered the same address. */
    public static boolean sameAddress(String oldConfiguration, String newConfiguration) {
        if (oldConfiguration == null || newConfiguration == null) {
            return false;
        }
        JsonNode oldNode;
        JsonNode newNode;
        try {
            oldNode = JSON_MAPPER.readTree(oldConfiguration);
            newNode = JSON_MAPPER.readTree(newConfiguration);
        } catch (IOException e) {
            return false;
        }
        if (!oldNode.isObject() || !newNode.isObject()) {
            return false;
        }
        Set<String> addressFields = new HashSet<>();
        oldNode.fieldNames().forEachRemaining(addressFields::add);
        newNode.fieldNames().forEachRemaining(addressFields::add);
        addressFields.removeIf(field -> !ADDRESS_FIELD.matcher(field).matches());
        return addressFields.stream().allMatch(field -> Objects.equals(valueOf(oldNode, field), valueOf(newNode, field)));
    }

    private static JsonNode valueOf(JsonNode configuration, String field) {
        var value = configuration.get(field);
        return value == null || value.isNull() ? null : value;
    }
}
