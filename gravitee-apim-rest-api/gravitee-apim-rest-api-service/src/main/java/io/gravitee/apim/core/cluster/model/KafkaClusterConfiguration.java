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
package io.gravitee.apim.core.cluster.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.utils.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @param backendMetadataMaxAge seconds during which each gateway instance reuses its own all-topics metadata request to
 *     this cluster, {@code null} when the cluster leaves the gateway default
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record KafkaClusterConfiguration(List<KafkaClusterConnection> connections, Integer backendMetadataMaxAge) {
    private static final TypeReference<Map<String, Object>> RAW = new TypeReference<>() {};
    private static final TypeReference<List<KafkaClusterConnection>> CONNECTIONS = new TypeReference<>() {};
    private static final String BACKEND_METADATA_MAX_AGE = "backendMetadataMaxAge";

    public KafkaClusterConfiguration(List<KafkaClusterConnection> connections) {
        this(connections, null);
    }

    /**
     * {@code configuration} with a cross id on every connection, slugified from its name when it has none. The rest is
     * kept as sent, for the schema validation to judge: through this record, a max age of 1.5 would reach it as 1, and
     * "abc" would fail before it. A configuration whose connections do not map, or that has a null connection, is returned
     * as sent too, for the validation to reject.
     */
    public static Object withGeneratedConnectionCrossIds(Object configuration, ObjectMapper objectMapper) {
        Sent sent = read(configuration, objectMapper).orElse(null);
        if (sent == null || sent.connections() == null || sent.connections().isEmpty() || sent.connections().contains(null)) {
            return configuration;
        }
        List<KafkaClusterConnection> updatedConnections = sent
            .connections()
            .stream()
            .map(conn ->
                new KafkaClusterConnection(
                    StringUtils.isEmpty(conn.crossId()) ? StringUtils.slugify(conn.name()) : conn.crossId(),
                    conn.name(),
                    conn.bootstrapServers(),
                    conn.security()
                )
            )
            .toList();
        Map<String, Object> rebuilt = objectMapper.convertValue(new KafkaClusterConfiguration(updatedConnections, null), RAW);
        if (sent.raw().containsKey(BACKEND_METADATA_MAX_AGE)) {
            rebuilt.put(BACKEND_METADATA_MAX_AGE, sent.raw().get(BACKEND_METADATA_MAX_AGE));
        }
        return rebuilt;
    }

    /**
     * The connections of {@code configuration}, read before the schema validation has judged the rest: through this
     * record, a max age of "abc" would fail the read instead of being rejected. Empty when the connections do not map,
     * which the validation rejects.
     */
    public static Optional<KafkaClusterConfiguration> connectionsOf(Object configuration, ObjectMapper objectMapper) {
        return read(configuration, objectMapper).map(sent -> new KafkaClusterConfiguration(sent.connections()));
    }

    private record Sent(Map<String, Object> raw, List<KafkaClusterConnection> connections) {}

    /** {@code configuration} as sent, with its connections read; empty when they do not map. */
    private static Optional<Sent> read(Object configuration, ObjectMapper objectMapper) {
        try {
            Map<String, Object> raw = objectMapper.convertValue(configuration, RAW);
            return Optional.of(new Sent(raw, objectMapper.convertValue(raw.get("connections"), CONNECTIONS)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
