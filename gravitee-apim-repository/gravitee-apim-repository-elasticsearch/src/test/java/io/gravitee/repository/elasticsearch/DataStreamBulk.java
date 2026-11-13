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
package io.gravitee.repository.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a bulk written for daily indices into the bulks that load the same documents into one data stream, one bulk per
 * daily index. Rolling the data stream over after each one gives every daily index its own backing index, so documents
 * keep the mapping and the {@code _id} they had.
 *
 * <p>A data stream only accepts {@code create}: two documents sharing an {@code _id} in the same daily index were one
 * document, the last written, and stay so.
 */
final class DataStreamBulk {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DataStreamBulk() {}

    static List<String> of(String dailyBulk, String dataStream) {
        final List<String> lines = dailyBulk
            .lines()
            .filter(line -> !line.isBlank())
            .toList();
        final Map<String, Map<String, String>> documentsByDailyIndex = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i += 2) {
            final JsonNode target = read(lines.get(i)).elements().next();
            final String dailyIndex = target.path("_index").asText();
            final String id = target.path("_id").asText(null);

            final ObjectNode create = MAPPER.createObjectNode();
            create.put("_index", dataStream);
            if (id != null) {
                create.put("_id", id);
            }
            documentsByDailyIndex
                .computeIfAbsent(dailyIndex, index -> new LinkedHashMap<>())
                .put(id == null ? "line-" + i : id, MAPPER.createObjectNode().set("create", create) + "\n" + lines.get(i + 1));
        }
        return documentsByDailyIndex
            .values()
            .stream()
            .map(documents -> String.join("\n", documents.values()) + "\n")
            .toList();
    }

    private static JsonNode read(String line) {
        try {
            return MAPPER.readTree(line);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Not a bulk action line: " + line, e);
        }
    }
}
