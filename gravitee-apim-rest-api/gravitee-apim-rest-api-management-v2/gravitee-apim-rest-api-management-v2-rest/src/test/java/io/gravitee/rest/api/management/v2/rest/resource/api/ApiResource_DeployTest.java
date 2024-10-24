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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static io.gravitee.common.http.HttpStatusCode.ACCEPTED_202;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.ApiFixtures;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class ApiResource_DeployTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @Test
    public void shouldDeployApi() {
        ApiDeploymentEntity deployEntity = new ApiDeploymentEntity();
        deployEntity.setDeploymentLabel("label");

        ApiEntity apiEntity = ApiFixtures.aModelApiV4().toBuilder().state(Lifecycle.State.STARTED).updatedAt(new Date()).build();
        when(apiStateServiceV4.deploy(eq(GraviteeContext.getExecutionContext()), any(String.class), any(), any())).thenReturn(apiEntity);

        final Response response = rootTarget(API + "/deployments").request().post(Entity.json(deployEntity));

        assertEquals(ACCEPTED_202, response.getStatus());

        assertEquals(apiEntity.getUpdatedAt().toString(), response.getLastModified().toString());
        assertEquals(new EntityTag(Long.toString(apiEntity.getUpdatedAt().getTime())), response.getEntityTag());

        verify(apiStateServiceV4, times(1)).deploy(eq(GraviteeContext.getExecutionContext()), any(String.class), any(), any());
    }

    @Test
    public void shouldNotDeployApi() {
        ApiDeploymentEntity deployEntity = new ApiDeploymentEntity();
        deployEntity.setDeploymentLabel("label_too_long_because_more_than_32_chars");

        final Response response = rootTarget(API + "/deployments").request().post(Entity.json(deployEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());

        verify(apiService, never()).deploy(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldDeployApiWithFailingLicenseCheck() {
        ApiDeploymentEntity deployEntity = new ApiDeploymentEntity();
        deployEntity.setDeploymentLabel("label");
        doThrow(new ForbiddenFeatureException("apim-en-endpoint-mqtt5")).when(apiLicenseService).checkLicense(any(), anyString());

        final Response response = rootTarget(API + "/deployments").request().post(Entity.json(deployEntity));

        assertEquals(FORBIDDEN_403, response.getStatus());

        var body = response.readEntity(Error.class);
        assertNotNull(body);
        assertEquals("Feature 'apim-en-endpoint-mqtt5' is not available with your license tier", body.getMessage());
    }

    @Test
    public void shouldNotDeployApiWithInvalidRole() {
        ApiDeploymentEntity deployEntity = new ApiDeploymentEntity();
        deployEntity.setDeploymentLabel("a nice label");

        when(permissionService.hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_DEFINITION), eq(API), any()))
            .thenReturn(false);

        final Response response = rootTarget(API + "/deployments").request().post(Entity.json(deployEntity));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        verify(apiService, never()).deploy(any(), any(), any(), any(), any());
    }
}
