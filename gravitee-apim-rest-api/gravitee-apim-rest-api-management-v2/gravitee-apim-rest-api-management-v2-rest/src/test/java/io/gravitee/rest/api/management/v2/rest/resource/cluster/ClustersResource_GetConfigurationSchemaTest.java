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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.cluster.domain_service.ClusterConfigurationSchemaService;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClustersResource_GetConfigurationSchemaTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";

    @Inject
    private ClusterConfigurationSchemaService clusterConfigurationSchemaService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/clusters/schema/configuration";
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
        reset(clusterConfigurationSchemaService);
    }

    @Test
    void should_return_configuration_schema() {
        String schema = "{\"type\":\"object\",\"properties\":{\"protocol\":{\"type\":\"string\"}}}";
        when(clusterConfigurationSchemaService.getConfigurationSchema()).thenReturn(schema);

        final Response response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.readEntity(String.class)).isEqualTo(schema);
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        shouldReturn403(RolePermission.ENVIRONMENT_CLUSTER, ENV_ID, RolePermissionAction.READ, () -> rootTarget().request().get());
    }
}
