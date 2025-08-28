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
import io.gravitee.common.utils.TimeProvider;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@UseCase
public class UpdateClusterGroupsUseCase {

    private final ClusterCrudService clusterCrudService;
    private final AuditDomainService auditService;

    public record Input(String clusterId, Set<String> groups, AuditInfo auditInfo) {}

    public record Output(Set<String> groups) {}

    public Output execute(Input input) {
        clusterCrudService.updateGroups(input.clusterId, input.auditInfo().environmentId(), input.groups());

        Set<String> updatedGroups = clusterCrudService
            .findByIdAndEnvironmentId(input.clusterId, input.auditInfo().environmentId())
            .getGroups();

        createAuditLog(updatedGroups, input.groups(), input.clusterId, input.auditInfo());

        return new Output(updatedGroups);
    }

    private void createAuditLog(
        Set<String> clusterGroupsBeforeUpdate,
        Set<String> clusterGroupsAfterUpdate,
        String clusterId,
        AuditInfo auditInfo
    ) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(ClusterAuditEvent.CLUSTER_GROUPS_UPDATED)
                .actor(auditInfo.actor())
                .oldValue(clusterGroupsBeforeUpdate)
                .newValue(clusterGroupsAfterUpdate)
                .createdAt(Instant.now().atZone(ZoneId.of("UTC")))
                .properties(Map.of(AuditProperties.CLUSTER, clusterId))
                .build()
        );
    }
}
