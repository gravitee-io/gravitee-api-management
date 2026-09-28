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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import inmemory.AbstractUseCaseTest;
import inmemory.ClusterCrudServiceInMemory;
import io.gravitee.apim.core.cluster.crud_service.ClusterCrudService;
import io.gravitee.apim.core.cluster.domain_service.ClusterConfigurationAccessDomainService;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.exception.DbEntityNotFoundException;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetClusterUseCaseTest extends AbstractUseCaseTest {

    private final ClusterCrudService clusterCrudService = new ClusterCrudServiceInMemory();
    private final PermissionDomainService permissionDomainService = mock(PermissionDomainService.class);
    private GetClusterUseCase getClusterUseCase;
    private Cluster existingCluster;

    @BeforeEach
    void setUp() {
        getClusterUseCase = new GetClusterUseCase(clusterCrudService, new ClusterConfigurationAccessDomainService(permissionDomainService));

        existingCluster = Cluster.builder()
            .id(GENERATED_UUID)
            .name("Cluster 1")
            .createdAt(INSTANT_NOW)
            .description("The cluster no 1")
            .environmentId(ENV_ID)
            .organizationId(ORG_ID)
            .configuration(Map.of("bootstrapServers", "localhost:9092"))
            .groups(Set.of("group-1", "group-2"))
            .build();
        ((ClusterCrudServiceInMemory) clusterCrudService).initWith(List.of(existingCluster));
    }

    @Test
    void should_return_cluster() {
        // Given
        grant(RolePermissionAction.UPDATE);
        // When
        var result = getClusterUseCase.execute(new GetClusterUseCase.Input(existingCluster.getId(), ENV_ID, ORG_ID, USER_ID));
        // Then
        assertThat(result.cluster()).usingRecursiveComparison().isEqualTo(existingCluster);
    }

    @Test
    void should_throw_exception_when_shared_policy_group_not_found() {
        // When
        var throwable = assertThrows(DbEntityNotFoundException.class, () ->
            getClusterUseCase.execute(new GetClusterUseCase.Input("unknown", ENV_ID, ORG_ID, USER_ID))
        );
        // Then
        assertThat(throwable.getMessage()).isEqualTo("Cluster with id unknown cannot be found");
    }

    @Test
    void should_throw_exception_when_shared_policy_group_not_found_in_environment() {
        // When
        var throwable = assertThrows(DbEntityNotFoundException.class, () ->
            getClusterUseCase.execute(new GetClusterUseCase.Input(existingCluster.getId(), "unknown", ORG_ID, USER_ID))
        );
        // Then
        assertThat(throwable.getMessage()).isEqualTo("Cluster with id " + GENERATED_UUID + " cannot be found");
    }

    @Test
    void should_hide_credentials_from_a_configuration_reader() {
        // Given
        givenClusterWithCredentials();
        grant(RolePermissionAction.READ);
        // When
        var result = getClusterUseCase.execute(new GetClusterUseCase.Input(existingCluster.getId(), ENV_ID, ORG_ID, USER_ID));
        // Then
        assertThat(result.cluster().getConfiguration()).isEqualTo(
            Map.of("bootstrapServers", "broker:9093", "security", Map.of("protocol", "SASL_SSL"))
        );
    }

    @Test
    void should_hide_configuration_from_a_member_without_configuration_permission() {
        // Given
        givenClusterWithCredentials();
        // When
        var result = getClusterUseCase.execute(new GetClusterUseCase.Input(existingCluster.getId(), ENV_ID, ORG_ID, USER_ID));
        // Then
        assertThat(result.cluster().getConfiguration()).isNull();
        assertThat(result.cluster().getId()).isEqualTo(existingCluster.getId());
    }

    private void givenClusterWithCredentials() {
        existingCluster.setConfiguration(
            Map.of("bootstrapServers", "broker:9093", "security", Map.of("protocol", "SASL_SSL", "sasl", Map.of("password", "secret")))
        );
        ((ClusterCrudServiceInMemory) clusterCrudService).initWith(List.of(existingCluster));
    }

    private void grant(RolePermissionAction action) {
        when(
            permissionDomainService.hasPermission(ORG_ID, USER_ID, RolePermission.CLUSTER_CONFIGURATION, existingCluster.getId(), action)
        ).thenReturn(true);
    }
}
