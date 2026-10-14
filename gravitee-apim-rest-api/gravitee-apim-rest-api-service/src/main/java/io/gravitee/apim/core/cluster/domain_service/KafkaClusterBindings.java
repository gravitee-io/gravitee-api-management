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
package io.gravitee.apim.core.cluster.domain_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads which registered Kafka cluster connections the {@code kafka} endpoints of a Message API are bound to: an endpoint
 * group whose shared configuration sets {@code mode: CLUSTER} and a {@code clusterCrossId}, and endpoints holding a
 * {@code connectionCrossId}. An endpoint not inheriting the group configuration is read from its own override.
 * A shared configuration in {@code CLUSTER} mode may also name a {@code defaultConnectionCrossId}: the connection the
 * management UIs give the endpoints using it. The gateway ignores it.
 */
public final class KafkaClusterBindings {

    private static final String KAFKA_ENDPOINT_TYPE = "kafka";
    private static final String CLUSTER_MODE = "CLUSTER";

    /**
     * @param clusterCrossId cluster of the connection the endpoint uses (its group's, or its own override); {@code null}
     *     when that connection is in {@code DIRECT} mode
     */
    public record Binding(String groupName, String endpointName, String clusterCrossId, String connectionCrossId) {
        public boolean isCluster() {
            return clusterCrossId != null;
        }

        /** How error messages name the endpoint. */
        public String subject() {
            return "Endpoint [" + endpointName + "] of group [" + groupName + "]";
        }
    }

    /** @param subject how error messages name the group, or the endpoint overriding it, that names the default */
    public record DefaultConnection(String subject, String clusterCrossId, String connectionCrossId) {}

    private KafkaClusterBindings() {}

    /** The default connections the shared configurations in {@code CLUSTER} mode name (the group's and the overrides). */
    public static List<DefaultConnection> defaultConnections(List<EndpointGroup> endpointGroups, ObjectMapper objectMapper) {
        List<DefaultConnection> defaults = new ArrayList<>();
        if (endpointGroups == null) {
            return defaults;
        }
        for (EndpointGroup group : endpointGroups) {
            if (group == null || !KAFKA_ENDPOINT_TYPE.equals(group.getType())) {
                continue;
            }
            addDefaultConnection(defaults, "Endpoint group [" + group.getName() + "]", group.getSharedConfiguration(), objectMapper);
            if (group.getEndpoints() == null) {
                continue;
            }
            for (Endpoint endpoint : group.getEndpoints()) {
                if (endpoint != null && !endpoint.isInheritConfiguration()) {
                    addDefaultConnection(
                        defaults,
                        "Endpoint [" + endpoint.getName() + "] of group [" + group.getName() + "]",
                        endpoint.getSharedConfigurationOverride(),
                        objectMapper
                    );
                }
            }
        }
        return defaults;
    }

    private static void addDefaultConnection(
        List<DefaultConnection> defaults,
        String subject,
        String sharedConfiguration,
        ObjectMapper objectMapper
    ) {
        JsonNode node = read(sharedConfiguration, objectMapper);
        String clusterCrossId = clusterCrossId(node);
        String connectionCrossId = text(node, "defaultConnectionCrossId");
        if (clusterCrossId != null && connectionCrossId != null) {
            defaults.add(new DefaultConnection(subject, clusterCrossId, connectionCrossId));
        }
    }

    /** Every {@code kafka} endpoint, with the cluster it is bound to ({@code null} in {@code DIRECT} mode). */
    public static List<Binding> of(List<EndpointGroup> endpointGroups, ObjectMapper objectMapper) {
        List<Binding> bindings = new ArrayList<>();
        if (endpointGroups == null) {
            return bindings;
        }
        for (EndpointGroup group : endpointGroups) {
            if (group == null || !KAFKA_ENDPOINT_TYPE.equals(group.getType())) {
                continue;
            }
            if (group.getEndpoints() == null) {
                continue;
            }
            for (Endpoint endpoint : group.getEndpoints()) {
                if (endpoint == null) {
                    continue;
                }
                String effectiveSharedConfiguration = endpoint.isInheritConfiguration()
                    ? group.getSharedConfiguration()
                    : endpoint.getSharedConfigurationOverride();
                bindings.add(
                    new Binding(
                        group.getName(),
                        endpoint.getName(),
                        clusterCrossId(read(effectiveSharedConfiguration, objectMapper)),
                        text(read(endpoint.getConfiguration(), objectMapper), "connectionCrossId")
                    )
                );
            }
        }
        return bindings;
    }

    /** Distinct cross ids of the clusters the endpoints are bound to. */
    public static List<String> clusterCrossIds(List<EndpointGroup> endpointGroups, ObjectMapper objectMapper) {
        return of(endpointGroups, objectMapper).stream().map(Binding::clusterCrossId).filter(Objects::nonNull).distinct().toList();
    }

    private static String clusterCrossId(JsonNode node) {
        if (!CLUSTER_MODE.equals(text(node, "mode"))) {
            return null;
        }
        String crossId = text(node, "clusterCrossId");
        // A CLUSTER group without a cluster is still a cluster binding; the schema validation reports the missing field.
        return crossId == null ? "" : crossId;
    }

    private static JsonNode read(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field) || !node.get(field).isTextual()) {
            return null;
        }
        return node.get(field).asText();
    }
}
