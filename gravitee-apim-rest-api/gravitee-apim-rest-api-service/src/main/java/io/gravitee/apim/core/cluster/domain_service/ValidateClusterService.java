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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.apim.core.cluster.model.KafkaVirtualClusterBackend;
import io.gravitee.apim.core.cluster.query_service.ClusterQueryService;
import io.gravitee.apim.core.json.JsonSchemaChecker;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ValidateClusterService {

    private final JsonSchemaChecker jsonSchemaChecker;
    private final ClusterConfigurationSchemaService clusterConfigurationSchemaService;
    private final ObjectMapper objectMapper;
    private final ClusterQueryService clusterQueryService;

    public void validate(Cluster cluster) {
        if (StringUtils.isEmpty(cluster.getName())) {
            throw new InvalidDataException("Name is required.");
        }
        if (Objects.isNull(cluster.getType())) {
            throw new InvalidDataException("Type is required.");
        }
        if (Objects.isNull(cluster.getConfiguration())) {
            throw new InvalidDataException("Configuration is required.");
        }
        try {
            String configJson = objectMapper.writeValueAsString(cluster.getConfiguration());
            String schema = clusterConfigurationSchemaService.getConfigurationSchema(cluster.getType());
            jsonSchemaChecker.validate(schema, configJson);
        } catch (JsonProcessingException e) {
            throw new InvalidDataException("Configuration is not valid JSON.");
        }

        if (cluster.getType() == ClusterType.KAFKA_CLUSTER) {
            validateUniqueConnectionNames(cluster);
            validateUniqueConnectionCrossIds(cluster);
        }

        if (cluster.getType() == ClusterType.KAFKA_VIRTUAL_CLUSTER) {
            validateUniqueBackends(cluster);
        }
    }

    public void validateForCreate(Cluster cluster) {
        validate(cluster);
        validateCrossIdUniqueness(cluster);
    }

    public void validateForUpdate(Cluster existingCluster, Cluster updatedCluster) {
        validate(updatedCluster);
        if (existingCluster.getCrossId() != null && !existingCluster.getCrossId().equals(updatedCluster.getCrossId())) {
            throw new InvalidDataException("CrossId is immutable and cannot be changed after creation.");
        }
    }

    private void validateCrossIdUniqueness(Cluster cluster) {
        if (StringUtils.isEmpty(cluster.getCrossId())) {
            throw new InvalidDataException("CrossId is required.");
        }
        var existing = clusterQueryService.findByCrossIdAndEnvironmentId(cluster.getCrossId(), cluster.getEnvironmentId());
        if (existing.isPresent() && !existing.get().getId().equals(cluster.getId())) {
            throw new InvalidDataException("A cluster with crossId '" + cluster.getCrossId() + "' already exists in this environment.");
        }
    }

    private void validateUniqueConnectionNames(Cluster cluster) {
        var config = cluster.getKafkaClusterConfiguration(objectMapper);
        var duplicates = config
            .connections()
            .stream()
            .collect(Collectors.groupingBy(c -> c.name(), Collectors.counting()))
            .entrySet()
            .stream()
            .filter(e -> e.getValue() > 1)
            .map(e -> e.getKey())
            .toList();

        if (!duplicates.isEmpty()) {
            throw new InvalidDataException("Connection names must be unique. Duplicates found: " + duplicates);
        }
    }

    private void validateUniqueBackends(Cluster cluster) {
        var config = cluster.getKafkaVirtualClusterConfiguration(objectMapper);
        if (config.backends() == null || config.backends().isEmpty()) {
            return;
        }
        var duplicates = config
            .backends()
            .stream()
            .collect(Collectors.groupingBy(b -> b, Collectors.counting()))
            .entrySet()
            .stream()
            .filter(e -> e.getValue() > 1)
            .map(e -> e.getKey())
            .toList();

        if (!duplicates.isEmpty()) {
            throw new InvalidDataException("Backend references must be unique within a virtual cluster. Duplicates found: " + duplicates);
        }
    }

    private void validateUniqueConnectionCrossIds(Cluster cluster) {
        var config = cluster.getKafkaClusterConfiguration(objectMapper);
        if (config.connections() == null || config.connections().isEmpty()) {
            return;
        }
        var duplicates = config
            .connections()
            .stream()
            .filter(c -> c.crossId() != null)
            .collect(Collectors.groupingBy(c -> c.crossId(), Collectors.counting()))
            .entrySet()
            .stream()
            .filter(e -> e.getValue() > 1)
            .map(e -> e.getKey())
            .toList();

        if (!duplicates.isEmpty()) {
            throw new InvalidDataException("Connection crossIds must be unique within a cluster. Duplicates found: " + duplicates);
        }
    }
}
