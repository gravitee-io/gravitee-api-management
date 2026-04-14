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

import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterAuditEvent;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class DeployClusterUseCase {

    private final ClusterCrudService clusterCrudService;
    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;
    private final AuditDomainService auditService;

    public record Input(String clusterId, AuditInfo auditInfo) {}

    public record Output(Cluster cluster) {}

    public Output execute(Input input) {
        Cluster cluster = clusterCrudService.findByIdAndEnvironmentId(input.clusterId(), input.auditInfo().environmentId());

        Cluster beforeDeploy = Cluster.builder()
            .id(cluster.getId())
            .lifecycleState(cluster.getLifecycleState())
            .version(cluster.getVersion())
            .build();

        requireNonNull(cluster.getCrossId(), "Cluster crossId must not be null to deploy");

        cluster.deploy();

        publishEvent(input.auditInfo(), cluster);

        Cluster updatedCluster = clusterCrudService.update(cluster);

        createAuditLog(beforeDeploy, updatedCluster, input.auditInfo());

        return new Output(updatedCluster);
    }

    private void publishEvent(AuditInfo auditInfo, Cluster cluster) {
        Event event = eventCrudService.createEvent(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            Set.of(auditInfo.environmentId()),
            EventType.DEPLOY_CLUSTER,
            cluster,
            Map.ofEntries(
                entry(Event.EventProperties.USER, auditInfo.actor().userId()),
                entry(Event.EventProperties.CLUSTER_ID, cluster.getCrossId())
            )
        );

        eventLatestCrudService.createOrPatchLatestEvent(auditInfo.organizationId(), cluster.getId(), event);
    }

    private void createAuditLog(Cluster oldCluster, Cluster newCluster, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(ClusterAuditEvent.CLUSTER_DEPLOYED)
                .actor(auditInfo.actor())
                .oldValue(oldCluster)
                .newValue(newCluster)
                .createdAt(newCluster.getUpdatedAt().atZone(TimeProvider.clock().getZone()))
                .properties(Map.of(AuditProperties.CLUSTER, newCluster.getId()))
                .build()
        );
    }
}
