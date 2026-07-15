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
package io.gravitee.apim.core.cluster.domain_service;

import static java.util.Map.entry;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterAuditEvent;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Undeploys a cluster: flips its lifecycle state to UNDEPLOYED, publishes the UNDEPLOY_CLUSTER
 * event and writes the CLUSTER_UNDEPLOYED audit log, then persists and returns the updated cluster.
 *
 * <p>Shared by {@code UndeployClusterUseCase} (explicit undeploy) and {@code UpdateClusterUseCase}
 * (a deployed virtual cluster whose backends are all removed is auto-undeployed).
 */
@DomainService
@RequiredArgsConstructor
public class UndeployClusterDomainService {

    private final ClusterCrudService clusterCrudService;
    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;
    private final AuditDomainService auditService;

    public Cluster undeploy(Cluster cluster, AuditInfo auditInfo) {
        return undeploy(cluster, cluster.getLifecycleState(), auditInfo);
    }

    /**
     * Variant for callers that mutated the cluster before undeploying it (e.g. an update that
     * empties the backends): {@code previousLifecycleState} is the state to record as the audit
     * log's "before" value instead of the cluster's current (already mutated) one.
     */
    public Cluster undeploy(Cluster cluster, ClusterLifecycleState previousLifecycleState, AuditInfo auditInfo) {
        Cluster beforeUndeploy = Cluster.builder()
            .id(cluster.getId())
            .lifecycleState(previousLifecycleState)
            .version(cluster.getVersion())
            .build();

        cluster.undeploy();

        publishEvent(auditInfo, cluster);

        Cluster updatedCluster = clusterCrudService.update(cluster);

        createAuditLog(beforeUndeploy, updatedCluster, auditInfo);

        return updatedCluster;
    }

    private void publishEvent(AuditInfo auditInfo, Cluster cluster) {
        Event event = eventCrudService.createEvent(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            Set.of(auditInfo.environmentId()),
            EventType.UNDEPLOY_CLUSTER,
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
                .event(ClusterAuditEvent.CLUSTER_UNDEPLOYED)
                .actor(auditInfo.actor())
                .oldValue(oldCluster)
                .newValue(newCluster)
                .createdAt(newCluster.getUpdatedAt().atZone(TimeProvider.clock().getZone()))
                .properties(Map.of(AuditProperties.CLUSTER, newCluster.getId()))
                .build()
        );
    }
}
