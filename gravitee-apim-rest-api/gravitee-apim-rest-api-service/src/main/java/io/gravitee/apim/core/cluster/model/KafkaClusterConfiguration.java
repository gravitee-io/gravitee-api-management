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
     * "abc" would fail before it.
     */
    public static Object withGeneratedConnectionCrossIds(Object configuration, ObjectMapper objectMapper) {
        Map<String, Object> sent = objectMapper.convertValue(configuration, RAW);
        List<KafkaClusterConnection> connections = objectMapper.convertValue(sent.get("connections"), CONNECTIONS);
        if (connections == null || connections.isEmpty()) {
            return configuration;
        }
        List<KafkaClusterConnection> updatedConnections = connections
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
        if (sent.containsKey(BACKEND_METADATA_MAX_AGE)) {
            rebuilt.put(BACKEND_METADATA_MAX_AGE, sent.get(BACKEND_METADATA_MAX_AGE));
        }
        return rebuilt;
    }
}
