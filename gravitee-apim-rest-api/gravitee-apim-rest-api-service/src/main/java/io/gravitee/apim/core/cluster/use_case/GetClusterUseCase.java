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
package io.gravitee.apim.core.cluster.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.domain_service.ClusterConfigurationAccessDomainService;
import io.gravitee.apim.core.cluster.model.Cluster;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@UseCase
public class GetClusterUseCase {

    private final ClusterCrudService clusterCrudService;
    private final ClusterConfigurationAccessDomainService clusterConfigurationAccessDomainService;

    public record Input(String clusterId, String environmentId, String organizationId, String userId) {}

    public record Output(Cluster cluster) {}

    public Output execute(Input input) {
        var cluster = clusterCrudService.findByIdAndEnvironmentId(input.clusterId, input.environmentId);
        return new Output(clusterConfigurationAccessDomainService.visibleTo(cluster, input.organizationId, input.userId));
    }
}
