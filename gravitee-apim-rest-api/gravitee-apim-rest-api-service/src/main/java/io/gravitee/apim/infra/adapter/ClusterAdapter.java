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
package io.gravitee.apim.infra.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterDefinition;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.definition.model.cluster.ClusterType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class ClusterAdapter {

    public static final ObjectMapper mapper = new ObjectMapper();

    @Mapping(target = "definition", source = "cluster", qualifiedByName = "mapDefinition")
    @Mapping(target = "type", source = "type", qualifiedByName = "mapTypeToString")
    @Mapping(target = "lifecycleState", source = "lifecycleState", qualifiedByName = "mapLifecycleStateToString")
    public abstract io.gravitee.repository.management.model.Cluster toRepository(Cluster cluster);

    @Named("mapTypeToString")
    public String mapTypeToString(ClusterType type) {
        return type != null ? type.name() : null;
    }

    @Named("mapDefinition")
    public String mapDefinition(Cluster cluster) throws JsonProcessingException {
        var clusterDefinition = ClusterDefinition.builder()
            .id(cluster.getId())
            .crossId(cluster.getCrossId())
            .type(cluster.getType() != null ? cluster.getType().name() : null)
            .name(cluster.getName())
            .configuration(cluster.getConfiguration())
            .build();
        return mapper.writeValueAsString(clusterDefinition);
    }

    @Named("mapLifecycleStateToString")
    public String mapLifecycleStateToString(ClusterLifecycleState state) {
        return state != null ? state.name() : null;
    }

    @Mapping(target = "configuration", source = "cluster", qualifiedByName = "mapConfiguration")
    @Mapping(target = "type", source = "type", qualifiedByName = "mapTypeFromString")
    @Mapping(target = "lifecycleState", source = "lifecycleState", qualifiedByName = "mapLifecycleStateFromString")
    public abstract Cluster fromRepository(io.gravitee.repository.management.model.Cluster cluster);

    @Named("mapLifecycleStateFromString")
    public ClusterLifecycleState mapLifecycleStateFromString(String state) {
        return state != null ? ClusterLifecycleState.valueOf(state) : ClusterLifecycleState.UNDEPLOYED;
    }

    @Named("mapTypeFromString")
    public ClusterType mapTypeFromString(String type) {
        return type != null ? ClusterType.valueOf(type) : ClusterType.KAFKA_CLUSTER_STANDALONE;
    }

    @Named("mapConfiguration")
    public Object mapConfiguration(io.gravitee.repository.management.model.Cluster cluster) throws JsonProcessingException {
        if (cluster.getDefinition() == null || cluster.getDefinition().isEmpty()) {
            return null;
        }

        var clusterDefinition = mapper.readValue(cluster.getDefinition(), ClusterDefinition.class);
        if (clusterDefinition == null) {
            return null;
        }
        return clusterDefinition.getConfiguration();
    }
}
