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
import io.gravitee.apim.core.cluster.domain_service.ValidateClusterService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterAuditEvent;
import io.gravitee.apim.core.cluster.model.CreateCluster;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class CreateClusterUseCase {

    private final ClusterCrudService clusterCrudService;
    private final ValidateClusterService validateClusterService;
    private final AuditDomainService auditService;

    public record Input(CreateCluster createCluster, AuditInfo auditInfo) {}

    public record Output(Cluster cluster) {}

    public Output execute(Input input) {
        Cluster clusterToCreate = Cluster
            .builder()
            .name(input.createCluster.getName())
            .description(input.createCluster.getDescription())
            .configuration(input.createCluster.getConfiguration())
            .id(UuidString.generateRandom())
            .createdAt(TimeProvider.instantNow())
            .environmentId(input.auditInfo().environmentId())
            .organizationId(input.auditInfo().organizationId())
            .build();

        validateClusterService.validate(clusterToCreate);

        Cluster createdCluster = clusterCrudService.create(clusterToCreate);

        createAuditLog(createdCluster, input.auditInfo());

        return new Output(createdCluster);
    }

    private void createAuditLog(Cluster cluster, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(ClusterAuditEvent.CLUSTER_CREATED)
                .actor(auditInfo.actor())
                .newValue(cluster)
                .createdAt(cluster.getCreatedAt().atZone(TimeProvider.clock().getZone()))
                .properties(Map.of(AuditProperties.CLUSTER, cluster.getId()))
                .build()
        );
    }
}
