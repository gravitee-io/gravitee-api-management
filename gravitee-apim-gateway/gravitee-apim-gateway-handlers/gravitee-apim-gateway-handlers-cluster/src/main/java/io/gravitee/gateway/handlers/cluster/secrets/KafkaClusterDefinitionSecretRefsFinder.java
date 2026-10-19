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
package io.gravitee.gateway.handlers.cluster.secrets;

import static io.gravitee.gateway.handlers.cluster.manager.impl.ClusterManagerImpl.CLUSTER_DEFINITION_KIND;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster;
import io.gravitee.definition.model.cluster.KafkaClusterReactableCluster.KafkaClusterConnection;
import io.gravitee.definition.model.cluster.SecurityConfig;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionDescriptor;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsFinder;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsListener;
import io.gravitee.secrets.api.discovery.SecretRefsLocation;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Finds the secret references of a registered Kafka cluster: every string of each connection's security. The listener
 * rewrites them into expressions that {@link io.gravitee.definition.model.cluster.ClusterSecurityResolver} evaluates.
 */
public class KafkaClusterDefinitionSecretRefsFinder implements DefinitionSecretRefsFinder<KafkaClusterReactableCluster> {

    private final ObjectMapper objectMapper;

    public KafkaClusterDefinitionSecretRefsFinder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canHandle(Object definition) {
        return definition instanceof KafkaClusterReactableCluster;
    }

    @Override
    public DefinitionDescriptor toDefinitionDescriptor(KafkaClusterReactableCluster definition, DefinitionMetadata metadata) {
        return new DefinitionDescriptor(
            new Definition(CLUSTER_DEFINITION_KIND, definition.getId()),
            Optional.ofNullable(metadata).map(DefinitionMetadata::revision)
        );
    }

    @Override
    public void findSecretRefs(KafkaClusterReactableCluster definition, DefinitionSecretRefsListener listener) {
        if (definition.getConnections() == null) {
            return;
        }
        for (KafkaClusterConnection connection : definition.getConnections()) {
            if (connection.getSecurity() == null) {
                continue;
            }
            listener.onCandidate(
                write(connection.getSecurity()),
                new SecretRefsLocation(CLUSTER_DEFINITION_KIND, definition.getType().name()),
                updated -> connection.setSecurity(read(updated))
            );
        }
    }

    private String write(SecurityConfig security) {
        try {
            return objectMapper.writeValueAsString(security);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private SecurityConfig read(String security) {
        try {
            return objectMapper.readValue(security, SecurityConfig.class);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
