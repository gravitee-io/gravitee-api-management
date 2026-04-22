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
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterAuditEvent;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@UseCase
public class DeleteClusterUseCase {

    private final ClusterCrudService clusterCrudService;
    private final AuditDomainService auditService;

    public record Input(String clusterId, AuditInfo auditInfo) {}

    public record Output() {}

    public Output execute(Input input) {
        Cluster cluster = clusterCrudService.findByIdAndEnvironmentId(input.clusterId, input.auditInfo.environmentId());

        if (cluster.getLifecycleState() == ClusterLifecycleState.DEPLOYED || cluster.getLifecycleState() == ClusterLifecycleState.PENDING) {
            throw new InvalidDataException("Cluster must be undeployed before deletion.");
        }

        clusterCrudService.delete(input.clusterId, input.auditInfo.environmentId());

        createAuditLog(input.clusterId, input.auditInfo());

        return new Output();
    }

    private void createAuditLog(String deletedClusterId, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(ClusterAuditEvent.CLUSTER_DELETED)
                .actor(auditInfo.actor())
                .newValue(null)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.CLUSTER, deletedClusterId))
                .build()
        );
    }
}
