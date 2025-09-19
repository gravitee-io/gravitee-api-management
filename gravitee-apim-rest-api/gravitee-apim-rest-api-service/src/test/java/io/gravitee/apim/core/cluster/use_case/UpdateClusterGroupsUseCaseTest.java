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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import inmemory.ClusterCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterAuditEvent;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class UpdateClusterGroupsUseCaseTest {

    private final ClusterCrudServiceInMemory clusterCrudService = new ClusterCrudServiceInMemory();
    private final AuditDomainService auditDomainService = Mockito.mock(AuditDomainService.class);

    private UpdateClusterGroupsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateClusterGroupsUseCase(clusterCrudService, auditDomainService);
        clusterCrudService.initWith(
            List.of(
                Cluster.builder()
                    .id("cluster-1")
                    .environmentId("env-1")
                    .organizationId("org-1")
                    .name("Cluster 1")
                    .description("desc")
                    .groups(Set.of("G1"))
                    .build()
            )
        );
    }

    @Test
    void should_update_cluster_groups_and_create_audit_log() {
        // Given
        var auditInfo = AuditInfo.builder()
            .organizationId("org-1")
            .environmentId("env-1")
            .actor(AuditActor.builder().userId("system").build())
            .build();
        var input = new UpdateClusterGroupsUseCase.Input("cluster-1", Set.of("G2", "G3"), auditInfo);

        // When
        var output = useCase.execute(input);

        // Then - groups updated in storage and returned by use case
        var updated = clusterCrudService.findByIdAndEnvironmentId("cluster-1", "env-1");
        assertThat(updated.getGroups()).containsExactlyInAnyOrder("G2", "G3");
        assertThat(output.groups()).containsExactlyInAnyOrder("G2", "G3");

        // And - audit log created with expected values
        ArgumentCaptor<EnvironmentAuditLogEntity> captor = ArgumentCaptor.forClass(EnvironmentAuditLogEntity.class);
        verify(auditDomainService, times(1)).createEnvironmentAuditLog(captor.capture());

        EnvironmentAuditLogEntity audit = captor.getValue();
        assertThat(audit.environmentId()).isEqualTo("env-1");
        assertThat(audit.organizationId()).isEqualTo("org-1");
        assertThat(audit.event()).isEqualTo(ClusterAuditEvent.CLUSTER_GROUPS_UPDATED);
        assertThat(audit.properties()).containsEntry(AuditProperties.CLUSTER, "cluster-1");
        assertThat((Set<String>) audit.oldValue()).containsExactlyInAnyOrder("G2", "G3");
        assertThat((Set<String>) audit.newValue()).containsExactlyInAnyOrder("G2", "G3");
        assertThat(audit.createdAt()).isNotNull();
    }
}
