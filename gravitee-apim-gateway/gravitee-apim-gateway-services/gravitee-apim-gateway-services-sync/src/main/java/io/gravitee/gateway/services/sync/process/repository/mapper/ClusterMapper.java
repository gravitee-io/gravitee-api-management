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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import static io.gravitee.repository.management.model.Event.EventProperties.CLUSTER_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster;
import io.gravitee.definition.model.cluster.ReactableCluster;
import io.gravitee.definition.model.cluster.SecurityConfig;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CustomLog
public class ClusterMapper {

    private final ObjectMapper objectMapper;

    public Maybe<String> toId(Event clusterEvent) {
        return Maybe.fromCallable(() -> {
            String clusterId = null;
            if (clusterEvent.getProperties() != null) {
                clusterId = clusterEvent.getProperties().get(CLUSTER_ID.getValue());
            }
            if (clusterId == null) {
                log.warn("Unable to extract cluster info from event [{}].", clusterEvent.getId());
            }
            return clusterId;
        });
    }

    @SuppressWarnings("unchecked")
    public Maybe<ReactableCluster> to(Event clusterEvent) {
        return Maybe.fromCallable(() -> {
            try {
                Map<String, Object> payload = objectMapper.readValue(clusterEvent.getPayload(), Map.class);
                String type = (String) payload.get("type");
                String id = (String) payload.get("id");
                String crossId = (String) payload.get("crossId");
                String name = (String) payload.get("name");
                String environmentId = (String) payload.get("environmentId");
                String organizationId = (String) payload.get("organizationId");
                Object deployedAtRaw = payload.get("deployedAt");
                Date deployedAt = deployedAtRaw != null ? new Date(((Number) deployedAtRaw).longValue() * 1000) : null;

                if ("KAFKA_CLUSTER".equals(type)) {
                    return buildKafkaCluster(id, crossId, name, environmentId, organizationId, deployedAt, payload);
                } else {
                    log.warn("Cluster type [{}] is not deployable yet, skipping cluster [{}].", type, id);
                    return null;
                }
            } catch (Exception e) {
                log.error("Unable to extract cluster definition from event [{}].", clusterEvent.getId(), e);
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private KafkaClusterReactableCluster buildKafkaCluster(
        String id,
        String crossId,
        String name,
        String environmentId,
        String organizationId,
        Date deployedAt,
        Map<String, Object> payload
    ) {
        List<KafkaClusterReactableCluster.KafkaClusterConnection> connections = Collections.emptyList();
        Object configuration = payload.get("configuration");
        if (configuration instanceof Map) {
            Map<String, Object> configMap = (Map<String, Object>) configuration;
            Object connectionsRaw = configMap.get("connections");
            if (connectionsRaw instanceof List) {
                connections = ((List<Map<String, Object>>) connectionsRaw).stream()
                    .map(connMap ->
                        KafkaClusterReactableCluster.KafkaClusterConnection.builder()
                            .crossId((String) connMap.get("crossId"))
                            .name((String) connMap.get("name"))
                            .bootstrapServers((String) connMap.get("bootstrapServers"))
                            .security(mapSecurity(connMap.get("security")))
                            .build()
                    )
                    .toList();
            }
        }

        return KafkaClusterReactableCluster.builder()
            .id(id)
            .crossId(crossId)
            .name(name)
            .type(ClusterType.KAFKA_CLUSTER)
            .environmentId(environmentId)
            .organizationId(organizationId)
            .deployedAt(deployedAt)
            .connections(connections)
            .build();
    }

    private SecurityConfig mapSecurity(Object securityRaw) {
        if (securityRaw == null) return null;
        return objectMapper.convertValue(securityRaw, SecurityConfig.class);
    }
}
