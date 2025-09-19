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
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class ClusterAdapter {

    public static final ObjectMapper mapper = new ObjectMapper();

    @Mapping(target = "definition", source = "cluster", qualifiedByName = "mapDefinition")
    public abstract io.gravitee.repository.management.model.Cluster toRepository(Cluster cluster);

    @Named("mapDefinition")
    public String mapDefinition(Cluster cluster) throws JsonProcessingException {
        var clusterDefinition = io.gravitee.definition.model.cluster.Cluster.builder()
            .id(cluster.getId())
            .name(cluster.getName())
            .configuration(cluster.getConfiguration())
            .build();
        return mapper.writeValueAsString(clusterDefinition);
    }

    @Mapping(target = "configuration", source = "cluster", qualifiedByName = "mapConfiguration")
    public abstract Cluster fromRepository(io.gravitee.repository.management.model.Cluster cluster);

    @Named("mapConfiguration")
    public Object mapConfiguration(io.gravitee.repository.management.model.Cluster cluster) throws JsonProcessingException {
        if (cluster.getDefinition() == null || cluster.getDefinition().isEmpty()) {
            return null;
        }

        var clusterDefinition = mapper.readValue(cluster.getDefinition(), io.gravitee.definition.model.cluster.Cluster.class);
        if (clusterDefinition == null) {
            return null;
        }
        return clusterDefinition.getConfiguration();
    }
}
