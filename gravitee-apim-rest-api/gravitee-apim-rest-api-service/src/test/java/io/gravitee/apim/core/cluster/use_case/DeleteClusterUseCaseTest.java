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

import inmemory.AbstractUseCaseTest;
import inmemory.ClusterCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterAuditEvent;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteClusterUseCaseTest extends AbstractUseCaseTest {

    private DeleteClusterUseCase deleteClusterUseCase;
    private final ClusterCrudServiceInMemory clusterCrudService = new ClusterCrudServiceInMemory();
    private Cluster existingCluster;

    @BeforeEach
    void setUp() {
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        deleteClusterUseCase = new DeleteClusterUseCase(clusterCrudService, auditService);

        existingCluster = Cluster.builder()
            .id(GENERATED_UUID)
            .name("Cluster 1")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 1")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .build();
        clusterCrudService.initWith(List.of(existingCluster));
    }

    @Test
    void should_delete() {
        // When
        deleteClusterUseCase.execute(new DeleteClusterUseCase.Input(existingCluster.getId(), AUDIT_INFO));

        // Then
        assertThat(((ClusterCrudServiceInMemory) clusterCrudService).storage()).isEmpty();
    }

    @Test
    void should_create_an_audit() {
        // When
        deleteClusterUseCase.execute(new DeleteClusterUseCase.Input(existingCluster.getId(), AUDIT_INFO));

        // Then
        AuditEntity deletedAudit = AuditEntity.builder()
            .id("generated-id")
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .referenceType(AuditEntity.AuditReferenceType.ENVIRONMENT)
            .referenceId(ENV_ID)
            .user(USER_ID)
            .properties(Map.of(AuditProperties.CLUSTER.name(), existingCluster.getId()))
            .event(ClusterAuditEvent.CLUSTER_DELETED.name())
            .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
            .build();

        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .containsExactly(deletedAudit);
    }
}
