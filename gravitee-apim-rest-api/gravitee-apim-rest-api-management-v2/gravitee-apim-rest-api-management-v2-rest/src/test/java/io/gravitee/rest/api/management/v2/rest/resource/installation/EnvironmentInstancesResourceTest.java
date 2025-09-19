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
package io.gravitee.rest.api.management.v2.rest.resource.installation;

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import inmemory.InstanceQueryServiceInMemory;
import io.gravitee.apim.core.gateway.model.Instance;
import io.gravitee.rest.api.management.v2.rest.model.InstanceDetailResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EnvironmentInstancesResourceTest extends AbstractResourceTest {

    public static final String FAKE_ENVIRONMENT_ID = "fake-environment";
    public static final String ENVIRONMENT_ID = "env-id";

    @Inject
    InstanceQueryServiceInMemory instanceQueryService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT_ID + "/instances";
    }

    @BeforeEach
    public void init() {
        GraviteeContext.setCurrentEnvironment(FAKE_ENVIRONMENT_ID);

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT_ID).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT_ID)).thenReturn(environmentEntity);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        instanceQueryService.reset();
    }

    @Test
    public void should_return_instance_details() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_INSTANCE,
                ENVIRONMENT_ID,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        var instanceId = "instance-id";
        var hostname = "foo.example.com";
        var ip = "42.42.42.1";
        instanceQueryService.initWith(
            List.of(Instance.builder().id(instanceId).hostname(hostname).ip(ip).environments(Set.of(ENVIRONMENT_ID)).build())
        );

        final Response response = rootTarget().path(instanceId).request().get();

        MAPIAssertions.assertThat(response)
            .hasStatus(OK_200)
            .asEntity(InstanceDetailResponse.class)
            .satisfies(instanceDetail -> {
                assertThat(instanceDetail.getHostname()).isEqualTo(hostname);
                assertThat(instanceDetail.getIp()).isEqualTo(ip);
                assertThat(instanceDetail.getId()).isEqualTo(instanceId);
            });
    }

    @Test
    public void should_return_not_found_if_no_instance_found() {
        var instanceId = "instance-id";
        var hostname = "foo.example.com";
        var ip = "42.42.42.1";
        instanceQueryService.initWith(
            List.of(Instance.builder().id(instanceId).hostname(hostname).ip(ip).environments(Set.of(ENVIRONMENT_ID)).build())
        );

        final Response response = rootTarget().path("not-existing-id").request().get();

        MAPIAssertions.assertThat(response).hasStatus(NOT_FOUND_404);
    }
}
