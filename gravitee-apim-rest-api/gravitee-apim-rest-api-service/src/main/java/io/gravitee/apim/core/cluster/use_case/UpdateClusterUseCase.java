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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.domain_service.UndeployClusterDomainService;
import io.gravitee.apim.core.cluster.domain_service.ValidateClusterService;
import io.gravitee.apim.core.cluster.domain_service.VirtualClusterBoundApisQueryService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterAuditEvent;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.apim.core.cluster.model.KafkaClusterConnection;
import io.gravitee.apim.core.cluster.model.KafkaVirtualClusterConfiguration;
import io.gravitee.apim.core.cluster.model.UpdateCluster;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@UseCase
public class UpdateClusterUseCase {

    private final ClusterCrudService clusterCrudService;
    private final ValidateClusterService validateClusterService;
    private final AuditDomainService auditService;
    private final PermissionDomainService permissionDomainService;
    private final ObjectMapper objectMapper;
    private final UndeployClusterDomainService undeployClusterDomainService;
    private final VirtualClusterBoundApisQueryService virtualClusterBoundApisQueryService;

    public record Input(String clusterId, UpdateCluster updateCluster, AuditInfo auditInfo) {}

    public record Output(Cluster cluster) {}

    public Output execute(Input input) {
        Cluster clusterToUpdate = clusterCrudService.findByIdAndEnvironmentId(input.clusterId, input.auditInfo.environmentId());

        if (
            !permissionDomainService.hasPermission(
                input.auditInfo.organizationId(),
                input.auditInfo.actor().userId(),
                RolePermission.CLUSTER_CONFIGURATION,
                input.clusterId,
                RolePermissionAction.UPDATE
            )
        ) {
            input.updateCluster().setConfiguration(null);
        }

        if (input.updateCluster().getConfiguration() != null && clusterToUpdate.getType() == ClusterType.KAFKA_CLUSTER) {
            input.updateCluster().setConfiguration(generateConnectionCrossIds(input.updateCluster().getConfiguration()));
        }

        // Removing all backends from a deployed virtual cluster undeploys it. A started native API
        // bound to that cluster must never be left pointing at it, so block the change while any
        // such API exists.
        boolean undeployEmptiedVirtualCluster =
            clusterToUpdate.getType() == ClusterType.KAFKA_VIRTUAL_CLUSTER &&
            input.updateCluster().getConfiguration() != null &&
            hasNoBackends(input.updateCluster().getConfiguration()) &&
            (clusterToUpdate.getLifecycleState() == ClusterLifecycleState.DEPLOYED ||
                clusterToUpdate.getLifecycleState() == ClusterLifecycleState.PENDING);

        if (undeployEmptiedVirtualCluster) {
            List<Api> startedApis = virtualClusterBoundApisQueryService.findStartedBoundApis(
                input.auditInfo().environmentId(),
                clusterToUpdate.getCrossId()
            );
            if (!startedApis.isEmpty()) {
                throw new InvalidDataException(
                    "Cannot remove all backends from virtual cluster '" +
                        clusterToUpdate.getName() +
                        "': " +
                        startedApis.size() +
                        " started API(s) are bound to it. Stop them first: " +
                        startedApis.stream().map(Api::getName).collect(Collectors.joining(", ")) +
                        "."
                );
            }
        }

        Cluster existingClusterSnapshot = Cluster.builder()
            .id(clusterToUpdate.getId())
            .crossId(clusterToUpdate.getCrossId())
            .type(clusterToUpdate.getType())
            .name(clusterToUpdate.getName())
            .environmentId(clusterToUpdate.getEnvironmentId())
            .organizationId(clusterToUpdate.getOrganizationId())
            .build();

        clusterToUpdate.update(input.updateCluster());

        validateClusterService.validateForUpdate(existingClusterSnapshot, clusterToUpdate);

        if (undeployEmptiedVirtualCluster) {
            // Persist the emptied configuration and transition the cluster to UNDEPLOYED in one step.
            Cluster undeployedCluster = undeployClusterDomainService.undeploy(clusterToUpdate, input.auditInfo());
            return new Output(undeployedCluster);
        }

        Cluster updatedCluster = clusterCrudService.update(clusterToUpdate);

        createAuditLog(clusterToUpdate, updatedCluster, input.auditInfo());

        return new Output(updatedCluster);
    }

    private boolean hasNoBackends(Object configuration) {
        KafkaVirtualClusterConfiguration config = objectMapper.convertValue(configuration, KafkaVirtualClusterConfiguration.class);
        return config.backends() == null || config.backends().isEmpty();
    }

    private Object generateConnectionCrossIds(Object configuration) {
        var config = objectMapper.convertValue(configuration, KafkaClusterConfiguration.class);
        if (config.connections() == null || config.connections().isEmpty()) {
            return configuration;
        }
        List<KafkaClusterConnection> updatedConnections = config
            .connections()
            .stream()
            .map(conn ->
                new KafkaClusterConnection(
                    StringUtils.isEmpty(conn.crossId()) ? StringUtils.slugify(conn.name()) : conn.crossId(),
                    conn.name(),
                    conn.bootstrapServers(),
                    conn.security()
                )
            )
            .toList();
        return objectMapper.convertValue(new KafkaClusterConfiguration(updatedConnections), Object.class);
    }

    private void createAuditLog(Cluster clusterBeforeUpdate, Cluster updatedCluster, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(ClusterAuditEvent.CLUSTER_UPDATED)
                .actor(auditInfo.actor())
                .oldValue(clusterBeforeUpdate)
                .newValue(updatedCluster)
                .createdAt(updatedCluster.getUpdatedAt().atZone(TimeProvider.clock().getZone()))
                .properties(Map.of(AuditProperties.CLUSTER, updatedCluster.getId()))
                .build()
        );
    }
}
