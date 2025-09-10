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
package io.gravitee.rest.api.management.v2.rest.resource.cluster;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.use_case.DeleteClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.GetClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.UpdateClusterGroupsUseCase;
import io.gravitee.apim.core.cluster.use_case.UpdateClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.members.GetClusterPermissionsUseCase;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.management.v2.rest.model.UpdateCluster;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.ClusterPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

class ClusterResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String CLUSTER_ID = "my-cluster";

    @Inject
    private GetClusterUseCase getClusterUseCase;

    @Inject
    private UpdateClusterUseCase updateClusterUseCase;

    @Inject
    private DeleteClusterUseCase deleteClusterUseCase;

    @Inject
    private GetClusterPermissionsUseCase getClusterPermissionsUseCase;

    @Autowired
    private UpdateClusterGroupsUseCase updateClusterGroupsUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/clusters/" + CLUSTER_ID;
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(updateClusterUseCase, deleteClusterUseCase, getClusterPermissionsUseCase);
    }

    @Nested
    class GetClusterTest {

        @Test
        void should_get_cluster() {
            when(getClusterUseCase.execute(any())).thenReturn(new GetClusterUseCase.Output(Cluster.builder().id("cluster-1").build()));

            final Response response = rootTarget().request().get();

            var cluster = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Cluster.class);

            assertAll(() -> assertThat(response.getStatus()).isEqualTo(OK_200), () -> assertThat(cluster.getId()).isEqualTo("cluster-1"));
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.CLUSTER_DEFINITION, CLUSTER_ID, RolePermissionAction.READ, () -> rootTarget().request().get());
            /*when(
                    permissionService.hasPermission(
                            GraviteeContext.getExecutionContext(),
                            RolePermission.CLUSTER_DEFINITION,
                            CLUSTER_ID,
                            RolePermissionAction.READ
                    )
            )
                    .thenReturn(false);

            final Response response = rootTarget().request().get();

            MAPIAssertions
                    .assertThat(response)
                    .hasStatus(FORBIDDEN_403)
                    .asError()
                    .hasHttpStatus(FORBIDDEN_403)
                    .hasMessage("You do not have sufficient rights to access this resource");*/
        }
    }

    @Nested
    class UpdateClusterTest {

        @Test
        void should_update_cluster() {
            Cluster updatedCluster = Cluster
                .builder()
                .id(CLUSTER_ID)
                .name("Cluster 1")
                .createdAt(Instant.now().minus(30, ChronoUnit.MINUTES))
                .updatedAt(Instant.now())
                .organizationId(ORGANIZATION)
                .environmentId(ENV_ID)
                .description("Cluster 1 description")
                .configuration(Map.of("bootstrapServers", "localhost:9092"))
                .build();
            when(updateClusterUseCase.execute(any())).thenReturn(new UpdateClusterUseCase.Output(updatedCluster));

            var updateCluster = new UpdateCluster();
            updateCluster.setName(updatedCluster.getName());
            io.gravitee.rest.api.management.v2.rest.model.Cluster updatedClusterResponse;
            try (Response response = rootTarget().request().put(json(updateCluster))) {
                assertThat(response.getStatus()).isEqualTo(OK_200);
                updatedClusterResponse = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Cluster.class);
            }

            assertAll(
                () -> assertThat(updatedClusterResponse.getId()).isEqualTo(updatedCluster.getId()),
                () -> assertThat(updatedClusterResponse.getName()).isEqualTo(updatedCluster.getName()),
                () ->
                    assertThat(updatedClusterResponse.getCreatedAt())
                        .isEqualTo(updatedCluster.getCreatedAt().atZone(TimeProvider.clock().getZone()).toOffsetDateTime()),
                () ->
                    assertThat(updatedClusterResponse.getUpdatedAt())
                        .isEqualTo(updatedCluster.getUpdatedAt().atZone(TimeProvider.clock().getZone()).toOffsetDateTime()),
                () -> assertThat(updatedClusterResponse.getDescription()).isEqualTo(updatedCluster.getDescription()),
                () -> assertThat(updatedClusterResponse.getConfiguration()).isEqualTo(updatedCluster.getConfiguration())
            );

            var captor = ArgumentCaptor.forClass(UpdateClusterUseCase.Input.class);
            verify(updateClusterUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                var input = captor.getValue();
                soft.assertThat(input.updateCluster().getName()).isEqualTo(updatedClusterResponse.getName());
                soft.assertThat(input.auditInfo()).isInstanceOf(AuditInfo.class);
            });
        }

        @Test
        void should_return_400_if_execute_fails_with_invalid_data_exception() {
            when(updateClusterUseCase.execute(any())).thenThrow(new InvalidDataException("Name is required."));

            try (Response response = rootTarget().request().put(json(new UpdateCluster()))) {
                assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            }
        }

        @Test
        void should_return_400_if_missing_body() {
            try (Response response = rootTarget().request().put(json(""))) {
                assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            }
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(
                RolePermission.CLUSTER_DEFINITION,
                CLUSTER_ID,
                RolePermissionAction.UPDATE,
                () -> rootTarget().request().put(json(""))
            );
        }
    }

    @Nested
    class DeleteClusterTest {

        @Test
        void should_delete_cluster() {
            when(deleteClusterUseCase.execute(any())).thenReturn(new DeleteClusterUseCase.Output());

            // When
            var response = rootTarget().request().delete();

            // Then
            MAPIAssertions.assertThat(response).hasStatus(204);

            var captor = ArgumentCaptor.forClass(DeleteClusterUseCase.Input.class);
            verify(deleteClusterUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                var input = captor.getValue();
                soft.assertThat(input.clusterId()).isEqualTo(CLUSTER_ID);
                soft.assertThat(input.auditInfo()).isInstanceOf(AuditInfo.class);
            });
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(
                RolePermission.CLUSTER_DEFINITION,
                CLUSTER_ID,
                RolePermissionAction.DELETE,
                () -> rootTarget().request().delete()
            );
        }
    }

    @Nested
    class UpdateClusterGroupsTest {

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(
                RolePermission.CLUSTER_DEFINITION,
                CLUSTER_ID,
                RolePermissionAction.UPDATE,
                () -> rootTarget().path("groups").request().put(json(Map.of("groups", Set.of("G1", "G2"))))
            );
        }

        @Test
        void should_update_cluster_groups() {
            when(updateClusterGroupsUseCase.execute(any())).thenReturn(new UpdateClusterGroupsUseCase.Output(Set.of("G1", "G2")));

            try (Response response = rootTarget().path("groups").request().put(json(List.of("G1", "G2")))) {
                assertThat(response.getStatus()).isEqualTo(OK_200);
                List<String> groups = response.readEntity(List.class);
                assertThat(groups).containsExactlyInAnyOrder("G1", "G2");
            }
            var captor = ArgumentCaptor.forClass(UpdateClusterGroupsUseCase.Input.class);
            verify(updateClusterGroupsUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                var input = captor.getValue();
                soft.assertThat(input.clusterId()).isEqualTo(CLUSTER_ID);
                soft.assertThat(input.groups()).containsExactlyInAnyOrder("G1", "G2");
                soft.assertThat(input.auditInfo()).isInstanceOf(AuditInfo.class);
            });
        }
    }

    @Nested
    class GetClusterPermissionsTest {

        private WebTarget target;

        @BeforeEach
        void setUp() {
            target = rootTarget("permissions");
        }

        @Test
        void should_get_cluster_permissions() {
            var output = Map.of(
                ClusterPermission.MEMBER.getName(),
                new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() },
                ClusterPermission.DEFINITION.getName(),
                new char[] { READ.getId() }
            );
            when(getClusterPermissionsUseCase.execute(any())).thenReturn(new GetClusterPermissionsUseCase.Output(output));

            final Response response = target.request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);

            Map<String, String> permissionsResponse = (Map<String, String>) response.readEntity(Map.class);

            assertAll(
                () -> assertThat(permissionsResponse.size()).isEqualTo(2),
                () -> assertThat(permissionsResponse.get(ClusterPermission.MEMBER.getName())).isEqualTo("CRUD"),
                () -> assertThat(permissionsResponse.get(ClusterPermission.DEFINITION.getName())).isEqualTo("R")
            );
        }
    }
}
