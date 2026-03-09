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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import inmemory.AbstractUseCaseTest;
import inmemory.ClusterCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.domain_service.ClusterConfigurationSchemaService;
import io.gravitee.apim.core.cluster.domain_service.ValidateClusterService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterAuditEvent;
import io.gravitee.apim.core.cluster.model.UpdateCluster;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.infra.json.JsonSchemaCheckerImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.json.validation.JsonSchemaValidatorImpl;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.impl.JsonSchemaServiceImpl;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateClusterUseCaseTest extends AbstractUseCaseTest {

    private UpdateClusterUseCase updateClusterUseCase;
    private final ClusterCrudService clusterCrudService = new ClusterCrudServiceInMemory();
    private final PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);
    private Cluster existingCluster;

    @BeforeEach
    void setUp() {
        var jsonSchemaChecker = new JsonSchemaCheckerImpl(new JsonSchemaServiceImpl(new JsonSchemaValidatorImpl()));
        var clusterConfigurationSchemaService = new ClusterConfigurationSchemaService();
        var validateClusterService = new ValidateClusterService(jsonSchemaChecker, clusterConfigurationSchemaService, new ObjectMapper());
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        updateClusterUseCase = new UpdateClusterUseCase(clusterCrudService, validateClusterService, auditService, permissionDomainService);

        existingCluster = Cluster.builder()
            .id(GENERATED_UUID)
            .name("Cluster 1")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 1")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "PLAINTEXT")))
            .build();
        ((ClusterCrudServiceInMemory) clusterCrudService).initWith(List.of(existingCluster));

        lenient()
            .when(
                permissionDomainService.hasPermission(
                    ORG_ID,
                    USER_ID,
                    RolePermission.CLUSTER_CONFIGURATION,
                    GENERATED_UUID,
                    RolePermissionAction.UPDATE
                )
            )
            .thenReturn(true);
    }

    @Test
    void should_update_name() {
        // Given
        String name = "Cluster 1 - 2";
        var toUpdate = UpdateCluster.builder().name(name).build();

        // When
        var updatedCluster = updateClusterUseCase.execute(new UpdateClusterUseCase.Input(GENERATED_UUID, toUpdate, AUDIT_INFO));

        // Then
        var expected = existingCluster;
        expected.setName(name);
        expected.setUpdatedAt(INSTANT_NOW);

        assertThat(updatedCluster.cluster()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_update_description() {
        // Given
        String description = "The cluster no 1 - 2";
        var toUpdate = UpdateCluster.builder().description(description).build();

        // When
        var updatedCluster = updateClusterUseCase.execute(new UpdateClusterUseCase.Input(GENERATED_UUID, toUpdate, AUDIT_INFO));

        // Then
        var expected = existingCluster;
        expected.setDescription(description);
        expected.setUpdatedAt(INSTANT_NOW);

        assertThat(updatedCluster.cluster()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_update_configuration() {
        // Given
        Object configuration = Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "SSL"));
        var toUpdate = UpdateCluster.builder().configuration(configuration).build();

        // When
        var updatedCluster = updateClusterUseCase.execute(new UpdateClusterUseCase.Input(GENERATED_UUID, toUpdate, AUDIT_INFO));

        // Then
        var expected = existingCluster;
        expected.setConfiguration(configuration);
        expected.setUpdatedAt(INSTANT_NOW);

        assertThat(updatedCluster.cluster()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_create_an_audit() {
        // Given
        String name = "Cluster 1 - 2";
        var toUpdate = UpdateCluster.builder().name(name).build();
        updateClusterUseCase.execute(new UpdateClusterUseCase.Input(GENERATED_UUID, toUpdate, AUDIT_INFO));

        // Then
        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .containsExactly(
                AuditEntity.builder()
                    .id(GENERATED_UUID)
                    .organizationId(ORG_ID)
                    .environmentId(ENV_ID)
                    .referenceType(AuditEntity.AuditReferenceType.ENVIRONMENT)
                    .referenceId(ENV_ID)
                    .user(USER_ID)
                    .properties(Map.of(AuditProperties.CLUSTER.name(), GENERATED_UUID))
                    .event(ClusterAuditEvent.CLUSTER_UPDATED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_not_update_configuration_without_CLUSTER_CONFIGURATION_UPDATE_permission() {
        // Given
        when(
            permissionDomainService.hasPermission(
                ORG_ID,
                USER_ID,
                RolePermission.CLUSTER_CONFIGURATION,
                GENERATED_UUID,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(false);
        var toUpdate = UpdateCluster.builder()
            .configuration(Map.of("bootstrapServers", "localhost:9092", "security", Map.of("protocol", "SSL")))
            .build();

        // When
        var updatedCluster = updateClusterUseCase.execute(new UpdateClusterUseCase.Input(GENERATED_UUID, toUpdate, AUDIT_INFO));

        // Then
        assertThat(updatedCluster.cluster().getConfiguration()).isEqualTo(existingCluster.getConfiguration());
    }
}
