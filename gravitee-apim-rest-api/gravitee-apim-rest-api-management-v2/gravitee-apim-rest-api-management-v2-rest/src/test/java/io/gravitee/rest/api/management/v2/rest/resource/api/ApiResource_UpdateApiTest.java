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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.ApiFixtures;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.ApiFederated;
import io.gravitee.rest.api.management.v2.rest.model.ApiV2;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.GenericApi;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiFederated;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiV2;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiV4;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

public class ApiResource_UpdateApiTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @Test
    public void should_return_404_if_not_found() {
        UpdateApiV4 updateApiV4 = ApiFixtures.anUpdateApiV4();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenThrow(new ApiNotFoundException(API));

        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Api [" + API + "] cannot be found.", error.getMessage());
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        UpdateApiV4 updateApiV4 = ApiFixtures.anUpdateApiV4();
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        )
            .thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_GATEWAY_DEFINITION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        )
            .thenReturn(false);
        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());
    }

    @Test
    public void should_update_v4_api() {
        ApiEntity apiEntity = ApiFixtures.aModelApiV4().toBuilder().id(API).build();
        UpdateApiV4 updateApiV4 = ApiFixtures.anUpdateApiV4();

        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(apiEntity);
        when(apiServiceV4.update(eq(GraviteeContext.getExecutionContext()), eq(API), any(UpdateApiEntity.class), eq(false), eq(USER_NAME)))
            .thenReturn(apiEntity);
        when(apiStateServiceV4.isSynchronized(eq(GraviteeContext.getExecutionContext()), eq(apiEntity))).thenReturn(true);

        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(OK_200, response.getStatus());

        final ApiV4 apiV4 = response.readEntity(ApiV4.class);
        assertEquals(API, apiV4.getId());
        assertEquals(GenericApi.DeploymentStateEnum.DEPLOYED, apiV4.getDeploymentState());

        verify(apiServiceV4)
            .update(
                eq(GraviteeContext.getExecutionContext()),
                eq(API),
                argThat(updateApiEntity -> {
                    assertEquals(updateApiV4.getName(), updateApiEntity.getName());
                    assertEquals(API, updateApiEntity.getId());
                    return true;
                }),
                eq(false),
                eq(USER_NAME)
            );
    }

    @Test
    public void should_return_bad_request_when_updating_v2_api_with_v4_definition() {
        final io.gravitee.rest.api.model.api.ApiEntity apiEntity = ApiFixtures.aModelApiV2().toBuilder().id(API).build();
        final UpdateApiV4 updateApiV4 = ApiFixtures.anUpdateApiV4();

        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(apiEntity);

        final Response response = rootTarget(API).request().put(Entity.json(updateApiV4));
        assertEquals(BAD_REQUEST_400, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(BAD_REQUEST_400, (int) error.getHttpStatus());
        assertEquals("Api [" + API + "] is not valid.", error.getMessage());
    }

    @Test
    public void should_update_v2_api() {
        io.gravitee.rest.api.model.api.ApiEntity apiEntity = ApiFixtures.aModelApiV2().toBuilder().id(API).build();
        UpdateApiV2 updateApiV2 = ApiFixtures.anUpdateApiV2();

        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(apiEntity);
        when(
            apiService.update(
                eq(GraviteeContext.getExecutionContext()),
                eq(API),
                any(io.gravitee.rest.api.model.api.UpdateApiEntity.class),
                eq(false)
            )
        )
            .thenReturn(apiEntity);
        when(apiStateServiceV4.isSynchronized(eq(GraviteeContext.getExecutionContext()), eq(apiEntity))).thenReturn(false);

        final Response response = rootTarget(API).request().put(Entity.json(updateApiV2));
        assertEquals(OK_200, response.getStatus());

        final ApiV2 apiV2 = response.readEntity(ApiV2.class);
        assertEquals(API, apiV2.getId());
        assertEquals(GenericApi.DeploymentStateEnum.NEED_REDEPLOY, apiV2.getDeploymentState());

        verify(apiService)
            .update(
                eq(GraviteeContext.getExecutionContext()),
                eq(API),
                argThat(updateApiEntity -> {
                    assertEquals(updateApiV2.getName(), updateApiEntity.getName());
                    assertEquals(updateApiEntity.getGraviteeDefinitionVersion(), DefinitionVersion.V2.getLabel());
                    return true;
                }),
                eq(false)
            );
    }

    @Test
    public void should_return_bad_request_when_updating_v4_api_with_v2_definition() {
        final ApiEntity apiEntity = ApiFixtures.aModelApiV4().toBuilder().id(API).build();
        final UpdateApiV2 updateApiV2 = ApiFixtures.anUpdateApiV2();

        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(apiEntity);

        final Response response = rootTarget(API).request().put(Entity.json(updateApiV2));
        assertEquals(BAD_REQUEST_400, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(BAD_REQUEST_400, (int) error.getHttpStatus());
        assertEquals("Api [" + API + "] is not valid.", error.getMessage());
    }

    @Test
    public void should_update_federated_api() {
        FederatedApiEntity federatedApiEntity = ApiFixtures.aModelFederatedApi().toBuilder().id(API).build();
        Api api = ApiFixtures.aCoreApi();
        UpdateApiFederated updateApiFederated = ApiFixtures.anUpdateApiFederated();

        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(federatedApiEntity);
        when(updateFederatedApiDomainService.update(any(Api.class), any(AuditInfo.class))).thenReturn(api);

        final Response response = rootTarget(API).request().put(Entity.json(updateApiFederated));
        assertEquals(OK_200, response.getStatus());

        final ApiFederated apiFederated = response.readEntity(ApiFederated.class);
        assertEquals(API, apiFederated.getId());

        verify(updateFederatedApiDomainService)
            .update(
                argThat(updateApi -> {
                    assertEquals(updateApi.getName(), updateApiFederated.getName());
                    assertEquals(API, updateApi.getId());
                    assertEquals(updateApi.getDefinitionVersion(), DefinitionVersion.FEDERATED);
                    return true;
                }),
                any(AuditInfo.class)
            );
    }
}
