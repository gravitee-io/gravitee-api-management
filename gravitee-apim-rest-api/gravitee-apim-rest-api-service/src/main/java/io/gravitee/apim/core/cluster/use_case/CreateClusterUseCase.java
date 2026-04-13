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
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.domain_service.ValidateClusterService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterAuditEvent;
import io.gravitee.apim.core.cluster.model.ClusterType;
import io.gravitee.apim.core.cluster.model.CreateCluster;
import io.gravitee.apim.core.cluster.model.KafkaClusterConfiguration;
import io.gravitee.apim.core.cluster.model.KafkaClusterConnection;
import io.gravitee.apim.core.membership.crud_service.MembershipCrudService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.apim.core.membership.query_service.RoleQueryService;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class CreateClusterUseCase {

    private final ClusterCrudService clusterCrudService;
    private final ValidateClusterService validateClusterService;
    private final AuditDomainService auditService;
    private final MembershipCrudService membershipCrudService;
    private final RoleQueryService roleQueryService;
    private final ObjectMapper objectMapper;

    public record Input(CreateCluster createCluster, AuditInfo auditInfo) {}

    public record Output(Cluster cluster) {}

    public Output execute(Input input) {
        Instant now = TimeProvider.instantNow();
        String crossId = StringUtils.isEmpty(input.createCluster.getCrossId())
            ? StringUtils.slugify(input.createCluster.getName())
            : input.createCluster.getCrossId();

        Object configuration = input.createCluster.getConfiguration();
        if (input.createCluster.getType() == ClusterType.KAFKA_CLUSTER && configuration != null) {
            configuration = generateConnectionCrossIds(configuration);
        }

        Cluster clusterToCreate = Cluster.builder()
            .type(input.createCluster.getType())
            .crossId(crossId)
            .name(input.createCluster.getName())
            .description(input.createCluster.getDescription())
            .configuration(configuration)
            .id(UuidString.generateRandom())
            .createdAt(now)
            .updatedAt(now)
            .environmentId(input.auditInfo().environmentId())
            .organizationId(input.auditInfo().organizationId())
            .build();

        validateClusterService.validateForCreate(clusterToCreate);

        Cluster createdCluster = clusterCrudService.create(clusterToCreate);

        createClusterPrimaryOwner(input.auditInfo.actor().userId(), createdCluster.getId(), now);

        createAuditLog(createdCluster, input.auditInfo());

        return new Output(createdCluster);
    }

    private Membership createClusterPrimaryOwner(String memberId, String clusterId, Instant createdAt) {
        ZonedDateTime createdAtZonedDateTime = createdAt.atZone(TimeProvider.clock().getZone());
        Role role = roleQueryService
            .findByScopeAndNameAndOrganizationId(
                Role.Scope.CLUSTER,
                "PRIMARY_OWNER",
                GraviteeContext.getExecutionContext().getOrganizationId()
            )
            .orElseThrow();
        Membership membership = Membership.builder()
            .id(UuidString.generateRandom())
            .memberId(memberId)
            .memberType(Membership.Type.USER)
            .referenceId(clusterId)
            .referenceType(Membership.ReferenceType.CLUSTER)
            .roleId(role.getId())
            .createdAt(createdAtZonedDateTime)
            .updatedAt(createdAtZonedDateTime)
            .build();
        return membershipCrudService.create(membership);
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

    private void createAuditLog(Cluster cluster, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
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
