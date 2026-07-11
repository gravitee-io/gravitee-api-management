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

import static java.util.Objects.requireNonNull;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.domain_service.UndeployClusterDomainService;
import io.gravitee.apim.core.cluster.domain_service.VirtualClusterBoundApisQueryService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class UndeployClusterUseCase {

    private final ClusterCrudService clusterCrudService;
    private final UndeployClusterDomainService undeployClusterDomainService;
    private final VirtualClusterBoundApisQueryService virtualClusterBoundApisQueryService;

    public record Input(String clusterId, AuditInfo auditInfo) {}

    public record Output(Cluster cluster) {}

    public Output execute(Input input) {
        Cluster cluster = clusterCrudService.findByIdAndEnvironmentId(input.clusterId(), input.auditInfo().environmentId());

        requireNonNull(cluster.getCrossId(), "Cluster crossId must not be null to undeploy");

        // A started native API bound to this virtual cluster must never be left pointing at an
        // undeployed cluster. Block the undeploy and ask the operator to stop those APIs first.
        if (cluster.getType() == ClusterType.KAFKA_VIRTUAL_CLUSTER) {
            List<Api> startedApis = virtualClusterBoundApisQueryService.findStartedBoundApis(
                input.auditInfo().environmentId(),
                cluster.getCrossId()
            );
            if (!startedApis.isEmpty()) {
                throw new InvalidDataException(
                    "Cannot undeploy virtual cluster '" +
                        cluster.getName() +
                        "': " +
                        startedApis.size() +
                        " started API(s) are bound to it. Stop them first: " +
                        startedApis.stream().map(Api::getName).collect(Collectors.joining(", ")) +
                        "."
                );
            }
        }

        Cluster updatedCluster = undeployClusterDomainService.undeploy(cluster, input.auditInfo());

        return new Output(updatedCluster);
    }
}
