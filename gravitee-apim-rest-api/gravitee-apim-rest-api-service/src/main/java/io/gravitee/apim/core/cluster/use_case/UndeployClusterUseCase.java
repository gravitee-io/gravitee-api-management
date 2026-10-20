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
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.domain_service.ClusterConfigurationAccessDomainService;
import io.gravitee.apim.core.cluster.domain_service.ClusterInUseDomainService;
import io.gravitee.apim.core.cluster.domain_service.UndeployClusterDomainService;
import io.gravitee.apim.core.cluster.model.Cluster;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class UndeployClusterUseCase {

    private final ClusterCrudService clusterCrudService;
    private final UndeployClusterDomainService undeployClusterDomainService;
    private final ClusterInUseDomainService clusterInUseDomainService;
    private final ClusterConfigurationAccessDomainService clusterConfigurationAccessDomainService;

    public record Input(String clusterId, AuditInfo auditInfo) {}

    public record Output(Cluster cluster) {}

    public Output execute(Input input) {
        Cluster cluster = clusterCrudService.findByIdAndEnvironmentId(input.clusterId(), input.auditInfo().environmentId());

        requireNonNull(cluster.getCrossId(), "Cluster crossId must not be null to undeploy");

        // A started API using this cluster must never be left pointing at an undeployed cluster.
        clusterInUseDomainService.checkNotUsed(cluster, input.auditInfo().environmentId(), "undeploy");

        Cluster updatedCluster = undeployClusterDomainService.undeploy(cluster, input.auditInfo());

        return new Output(
            clusterConfigurationAccessDomainService.visibleTo(
                updatedCluster,
                input.auditInfo().organizationId(),
                input.auditInfo().actor().userId()
            )
        );
    }
}
