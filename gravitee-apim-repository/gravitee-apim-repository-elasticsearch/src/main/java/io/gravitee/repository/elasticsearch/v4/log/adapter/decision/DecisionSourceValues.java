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
package io.gravitee.repository.elasticsearch.v4.log.adapter.decision;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the values of a decision document. An absent value reads as {@code null}, an absent list as an empty one.
 *
 * @author GraviteeSource Team
 */
public final class DecisionSourceValues {

    private DecisionSourceValues() {}

    public static List<String> textList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        var values = new ArrayList<String>(node.size());
        node.forEach(value -> {
            if (!value.isNull()) {
                values.add(value.asText());
            }
        });
        return values;
    }

    /** {@code null} rather than a failed page when the stamp is unreadable. */
    public static Long epochMillis(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        try {
            // the reporter writes an offset date-time ("…+02:00"), not a bare instant
            return OffsetDateTime.parse(timestamp).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static Long longOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asLong();
    }

    public static Integer intOrNull(JsonNode node) {
        return node == null || node.isNull() || node.isMissingNode() ? null : node.asInt();
    }

    public static Boolean booleanOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asBoolean();
    }
}
