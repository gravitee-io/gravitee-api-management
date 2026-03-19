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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiFixtures;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.use_case.UpdateApiDefinitionUseCase;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class ApiResource_UpdateApiWithDefinitionTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_import/definition";
    }

    @Test
    void should_return_403_when_no_definition_update_permission() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_DEFINITION,
                API,
                RolePermissionAction.UPDATE
            )
        ).thenReturn(false);

        Response response = rootTarget().request().put(Entity.json(new ExportApiV4()));

        assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);
    }

    @Test
    void should_return_404_when_api_not_found() {
        when(updateApiDefinitionUseCase.execute(any())).thenThrow(new ApiNotFoundException(API));

        var exportApiV4 = buildMinimalExportApiV4();
        Response response = rootTarget().request().put(Entity.entity(exportApiV4, MediaType.APPLICATION_JSON));

        assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
    }

    @Test
    void should_return_400_when_definition_version_not_supported() {
        when(updateApiDefinitionUseCase.execute(any())).thenThrow(
            new ApiDefinitionVersionNotSupportedException(DefinitionVersion.V2.getLabel())
        );

        var exportApiV4 = buildMinimalExportApiV4();
        Response response = rootTarget().request().put(Entity.entity(exportApiV4, MediaType.APPLICATION_JSON));

        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void should_return_200_and_updated_api_on_success() {
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API).environmentId(ENVIRONMENT).build();
        var apiWithFlows = new ApiWithFlows(existingApi, java.util.List.of());

        when(updateApiDefinitionUseCase.execute(any())).thenReturn(new UpdateApiDefinitionUseCase.Output(apiWithFlows));

        // mock getGenericApiEntityById used for isSynchronized check
        when(apiSearchServiceV4.findById(GraviteeContext.getExecutionContext(), API)).thenReturn(
            io.gravitee.rest.api.model.v4.api.ApiEntity.builder().id(API).name("Test API").apiVersion("1.0").build()
        );

        var exportApiV4 = buildMinimalExportApiV4();
        Response response = rootTarget().request().put(Entity.entity(exportApiV4, MediaType.APPLICATION_JSON));

        assertThat(response.getStatus()).isEqualTo(OK_200);
        var body = response.readEntity(ApiV4.class);
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(API);
    }

    @Test
    void should_return_400_when_invalid_paths_exception() {
        when(updateApiDefinitionUseCase.execute(any())).thenThrow(new InvalidPathsException("Path [/foo] already exists"));

        var exportApiV4 = buildMinimalExportApiV4();
        Response response = rootTarget().request().put(Entity.entity(exportApiV4, MediaType.APPLICATION_JSON));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_when_api_picture_is_invalid() {
        var exportApiV4 = buildMinimalExportApiV4();
        exportApiV4.setApiPicture("not-a-valid-image");

        Response response = rootTarget().request().put(Entity.entity(exportApiV4, MediaType.APPLICATION_JSON));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_when_api_background_is_invalid() {
        var exportApiV4 = buildMinimalExportApiV4();
        exportApiV4.setApiBackground("not-a-valid-image");

        Response response = rootTarget().request().put(Entity.entity(exportApiV4, MediaType.APPLICATION_JSON));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    private ExportApiV4 buildMinimalExportApiV4() {
        var apiV4 = new io.gravitee.rest.api.management.v2.rest.model.ApiV4();
        apiV4.setId(API);
        apiV4.setName("Test API");
        apiV4.setApiVersion("1.0");
        apiV4.setDefinitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4);
        apiV4.setType(io.gravitee.rest.api.management.v2.rest.model.ApiType.PROXY);

        var exportApiV4 = new ExportApiV4();
        exportApiV4.setApi(apiV4);
        return exportApiV4;
    }
}
