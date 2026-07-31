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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.ApiFixtures;
import inmemory.AbstractUseCaseTest;
import inmemory.ApiQueryServiceInMemory;
import inmemory.ClusterCrudServiceInMemory;
import inmemory.ClusterQueryServiceInMemory;
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.domain_service.ClusterConfigurationSchemaService;
import io.gravitee.apim.core.cluster.domain_service.ValidateClusterService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.model.ClusterAuditEvent;
import io.gravitee.apim.core.cluster.model.ClusterLifecycleState;
import io.gravitee.apim.core.cluster.model.UpdateCluster;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.infra.json.JsonSchemaCheckerImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.definition.model.cluster.ClusterType;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
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
    private final ClusterQueryServiceInMemory clusterQueryService = new ClusterQueryServiceInMemory();
    private final PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private final EventCrudInMemory eventCrudInMemory = new EventCrudInMemory();
    private final EventLatestCrudInMemory eventLatestCrudInMemory = new EventLatestCrudInMemory();
    private Cluster existingCluster;

    @BeforeEach
    void setUp() {
        var jsonSchemaChecker = new JsonSchemaCheckerImpl(new JsonSchemaServiceImpl(new JsonSchemaValidatorImpl()));
        var clusterConfigurationSchemaService = new ClusterConfigurationSchemaService();
        var validateClusterService = new ValidateClusterService(
            jsonSchemaChecker,
            clusterConfigurationSchemaService,
            objectMapper,
            clusterQueryService
        );
        var auditService = new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor());
        updateClusterUseCase = new UpdateClusterUseCase(
            clusterCrudService,
            validateClusterService,
            auditService,
            permissionDomainService,
            objectMapper
        );
        clusterQueryService.reset();
        apiQueryService.reset();

        existingCluster = Cluster.builder()
            .id(GENERATED_UUID)
            .crossId("cluster-1")
            .type(ClusterType.KAFKA_CLUSTER_STANDALONE)
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
    void should_preserve_crossId_on_update() {
        // Given
        var toUpdate = UpdateCluster.builder().name("New Name").build();

        // When
        var updatedCluster = updateClusterUseCase.execute(new UpdateClusterUseCase.Input(GENERATED_UUID, toUpdate, AUDIT_INFO));

        // Then - crossId remains unchanged
        assertThat(updatedCluster.cluster().getCrossId()).isEqualTo("cluster-1");
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

    @Test
    void should_flip_deployed_virtual_cluster_to_pending_when_removing_all_backends() {
        seedVirtualCluster(ClusterLifecycleState.DEPLOYED, backends("kafka-1", "conn-1"));

        var toUpdate = UpdateCluster.builder().configuration(Map.of("backends", List.of())).build();

        var output = updateClusterUseCase.execute(new UpdateClusterUseCase.Input(GENERATED_UUID, toUpdate, AUDIT_INFO));

        // Emptying the backends is a plain edit: no undeploy, the gateway keeps the previous
        // configuration and the emptied one is rejected at deploy time.
        assertThat(output.cluster().getLifecycleState()).isEqualTo(ClusterLifecycleState.PENDING);
        assertThat(eventCrudInMemory.storage()).isEmpty();
    }

    @Test
    void should_accept_removing_all_backends_even_with_started_bound_apis() {
        seedVirtualCluster(ClusterLifecycleState.DEPLOYED, backends("kafka-1", "conn-1"));
        apiQueryService.initWith(
            List.of(ApiFixtures.aNativeApiBoundToVirtualCluster("started-api", ENV_ID, Api.LifecycleState.STARTED, "mesh"))
        );

        var toUpdate = UpdateCluster.builder().configuration(Map.of("backends", List.of())).build();

        var output = updateClusterUseCase.execute(new UpdateClusterUseCase.Input(GENERATED_UUID, toUpdate, AUDIT_INFO));

        assertThat(output.cluster().getLifecycleState()).isEqualTo(ClusterLifecycleState.PENDING);
    }

    @Test
    void should_keep_pending_virtual_cluster_pending_when_removing_all_backends() {
        seedVirtualCluster(ClusterLifecycleState.PENDING, backends("kafka-1", "conn-1"));

        var toUpdate = UpdateCluster.builder().configuration(Map.of("backends", List.of())).build();

        var output = updateClusterUseCase.execute(new UpdateClusterUseCase.Input(GENERATED_UUID, toUpdate, AUDIT_INFO));

        assertThat(output.cluster().getLifecycleState()).isEqualTo(ClusterLifecycleState.PENDING);
    }

    @Test
    void should_keep_pending_flow_when_backends_are_kept() {
        seedVirtualCluster(ClusterLifecycleState.DEPLOYED, backends("kafka-1", "conn-1"));

        var toUpdate = UpdateCluster.builder().configuration(backends("kafka-2", "conn-2")).build();

        var output = updateClusterUseCase.execute(new UpdateClusterUseCase.Input(GENERATED_UUID, toUpdate, AUDIT_INFO));

        assertThat(output.cluster().getLifecycleState()).isEqualTo(ClusterLifecycleState.PENDING);
    }

    @Test
    void should_not_undeploy_already_undeployed_virtual_cluster_when_emptying_backends() {
        seedVirtualCluster(ClusterLifecycleState.UNDEPLOYED, backends("kafka-1", "conn-1"));

        var toUpdate = UpdateCluster.builder().configuration(Map.of("backends", List.of())).build();

        var output = updateClusterUseCase.execute(new UpdateClusterUseCase.Input(GENERATED_UUID, toUpdate, AUDIT_INFO));

        assertThat(output.cluster().getLifecycleState()).isEqualTo(ClusterLifecycleState.UNDEPLOYED);
        // The plain update path ran: no UNDEPLOY_CLUSTER event was emitted.
        assertThat(eventCrudInMemory.storage()).isEmpty();
    }

    private void seedVirtualCluster(ClusterLifecycleState lifecycleState, Object configuration) {
        Cluster virtualCluster = Cluster.builder()
            .id(GENERATED_UUID)
            .crossId("mesh")
            .type(ClusterType.KAFKA_VIRTUAL_CLUSTER)
            .name("My Virtual Cluster")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .lifecycleState(lifecycleState)
            .version(1)
            .configuration(configuration)
            .build();
        ((ClusterCrudServiceInMemory) clusterCrudService).initWith(List.of(virtualCluster));
    }

    private static Map<String, Object> backends(String clusterCrossId, String connectionCrossId) {
        return Map.of("backends", List.of(Map.of("clusterCrossId", clusterCrossId, "connectionCrossId", connectionCrossId)));
    }
}
