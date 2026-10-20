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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.domain_service.ClusterInUseDomainService;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;

/**
 * The started APIs that block a change of a cluster, so that a UI can name them before the change is attempted: those
 * using the cluster (they block undeploying or deleting it), or, given {@code connectionCrossIds}, those using one of
 * these connections (they block removing them).
 */
@AllArgsConstructor
@UseCase
public class GetClusterStartedApisUseCase {

    private final ClusterCrudService clusterCrudService;
    private final ClusterInUseDomainService clusterInUseDomainService;

    /** @param connectionCrossIds connections about to be removed; {@code null} or empty for the whole cluster */
    public record Input(String clusterId, String environmentId, Set<String> connectionCrossIds) {}

    public record Output(List<Api> apis) {}

    public Output execute(Input input) {
        var cluster = clusterCrudService.findByIdAndEnvironmentId(input.clusterId(), input.environmentId());
        if (input.connectionCrossIds() == null || input.connectionCrossIds().isEmpty()) {
            return new Output(clusterInUseDomainService.startedApisUsing(cluster, input.environmentId()));
        }
        return new Output(
            clusterInUseDomainService.startedApisUsingConnections(cluster, input.connectionCrossIds(), input.environmentId())
        );
    }
}
