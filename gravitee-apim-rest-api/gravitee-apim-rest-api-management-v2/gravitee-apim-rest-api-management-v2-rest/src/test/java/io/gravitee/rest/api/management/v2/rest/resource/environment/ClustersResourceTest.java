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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.cluster.model.Cluster;
import io.gravitee.apim.core.cluster.use_case.CreateClusterUseCase;
import io.gravitee.apim.core.cluster.use_case.SearchClusterUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.management.v2.rest.model.CreateCluster;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ClustersResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";

    @Inject
    private CreateClusterUseCase createClusterUseCase;

    @Inject
    private SearchClusterUseCase searchClusterUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/clusters";
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
        reset(createClusterUseCase);
    }

    @Nested
    class CreateClusterTest {

        @Test
        void should_create_cluster() {
            CreateCluster createCluster = new CreateCluster();
            createCluster.setName("cluster-1");
            createCluster.setDescription("Cluster 1 description");
            createCluster.setConfiguration(Map.of("bootstrapServers", "localhost:9092"));

            Cluster output = Cluster
                .builder()
                .createdAt(Instant.now())
                .id("cl-id-1")
                .name(createCluster.getName())
                .environmentId(ENV_ID)
                .organizationId(ORGANIZATION)
                .description(createCluster.getDescription())
                .configuration(createCluster.getConfiguration())
                .build();

            when(createClusterUseCase.execute(any())).thenReturn(new CreateClusterUseCase.Output(output));

            final Response response = rootTarget().request().post(json(createCluster));

            assertThat(response.getStatus()).isEqualTo(CREATED_201);

            var createdCluster = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Cluster.class);

            assertAll(
                () -> assertThat(createdCluster.getId()).isEqualTo(output.getId()),
                () ->
                    assertThat(createdCluster.getCreatedAt())
                        .isEqualTo(output.getCreatedAt().atZone(TimeProvider.clock().getZone()).toOffsetDateTime()),
                () -> assertThat(createdCluster.getUpdatedAt()).isNull(),
                () -> assertThat(createdCluster.getName()).isEqualTo(createCluster.getName()),
                () -> assertThat(createdCluster.getDescription()).isEqualTo(createCluster.getDescription()),
                () -> assertThat(createdCluster.getConfiguration()).isEqualTo(createCluster.getConfiguration())
            );

            var captor = ArgumentCaptor.forClass(CreateClusterUseCase.Input.class);
            verify(createClusterUseCase).execute(captor.capture());
            SoftAssertions.assertSoftly(soft -> {
                var input = captor.getValue();
                soft.assertThat(input.createCluster().getName()).isEqualTo(createCluster.getName());
                soft.assertThat(input.auditInfo()).isInstanceOf(AuditInfo.class);
            });
        }

        @Test
        void should_return_400_if_execute_fails_with_invalid_data_exception() {
            CreateCluster createCluster = new CreateCluster();

            when(createClusterUseCase.execute(any())).thenThrow(new InvalidDataException("Name is required."));

            final Response response = rootTarget().request().post(json(createCluster));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_if_missing_body() {
            final Response response = rootTarget().request().post(json(null));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_if_name_not_specified() {
            final Response response = rootTarget().request().post(json(new CreateCluster()));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(
                RolePermission.ENVIRONMENT_CLUSTER,
                ENV_ID,
                RolePermissionAction.CREATE,
                () -> rootTarget().request().post(json(new CreateCluster()))
            );
        }
    }

    @Nested
    class SearchClustersTest {

        @Test
        void should_search_clusters() {
            List<Cluster> outputClusters = List.of(
                Cluster.builder().name("Cluster 1").build(),
                Cluster.builder().name("Cluster 2").build(),
                Cluster.builder().name("Cluster 3").build(),
                Cluster.builder().name("Cluster 4").build(),
                Cluster.builder().name("Cluster 5").build(),
                Cluster.builder().name("Cluster 6").build(),
                Cluster.builder().name("Cluster 7").build(),
                Cluster.builder().name("Cluster 8").build(),
                Cluster.builder().name("Cluster 9").build(),
                Cluster.builder().name("Cluster 10").build()
            );
            var output = new Page<>(outputClusters, 1, 10, 23);

            when(searchClusterUseCase.execute(any())).thenReturn(new SearchClusterUseCase.Output(output));

            final Response response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);

            var clustersResponse = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.ClustersResponse.class);

            assertAll(
                () ->
                    assertThat(
                        clustersResponse.getData().stream().map(io.gravitee.rest.api.management.v2.rest.model.Cluster::getName).toList()
                    )
                        .isEqualTo(outputClusters.stream().map(Cluster::getName).toList()),
                () -> assertThat(clustersResponse.getPagination().getPage()).isEqualTo(1),
                () -> assertThat(clustersResponse.getPagination().getPerPage()).isEqualTo(10),
                () -> assertThat(clustersResponse.getPagination().getPageCount()).isEqualTo(3),
                () -> assertThat(clustersResponse.getPagination().getTotalCount()).isEqualTo(23),
                () -> assertThat(clustersResponse.getLinks()).isNotNull()
            );
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.ENVIRONMENT_CLUSTER, ENV_ID, RolePermissionAction.READ, () -> rootTarget().request().get());
        }
    }
}
