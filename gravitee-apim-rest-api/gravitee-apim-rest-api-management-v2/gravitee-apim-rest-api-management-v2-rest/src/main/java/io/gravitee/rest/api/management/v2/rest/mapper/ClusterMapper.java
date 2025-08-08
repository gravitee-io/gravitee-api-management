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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.rest.api.management.v2.rest.model.Cluster;
import io.gravitee.rest.api.management.v2.rest.model.CreateCluster;
import io.gravitee.rest.api.management.v2.rest.model.UpdateCluster;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { DateMapper.class })
public interface ClusterMapper {
    ClusterMapper INSTANCE = Mappers.getMapper(ClusterMapper.class);

    io.gravitee.apim.core.cluster.model.CreateCluster map(CreateCluster createCluster);

    Cluster map(io.gravitee.apim.core.cluster.model.Cluster cluster);

    io.gravitee.apim.core.cluster.model.UpdateCluster map(UpdateCluster updateCluster);
}
