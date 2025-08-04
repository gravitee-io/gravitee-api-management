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
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
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
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.management.v2.rest.model.CreateCluster;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ClustersResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";

    @Inject
    private CreateClusterUseCase createClusterUseCase;

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
    // TODO add the permissions
    /*@Test
    public void should_return_403_if_incorrect_permissions() {
        when(
                permissionService.hasPermission(
                        eq(GraviteeContext.getExecutionContext()),
                        eq(RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP),
                        eq(ENV_ID),
                        eq(RolePermissionAction.CREATE)
                )
        )
                .thenReturn(false);

        final Response response = rootTarget().request().post(json(SharedPolicyGroupFixtures.aCreateSharedPolicyGroup()));

        MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
    }*/
}
