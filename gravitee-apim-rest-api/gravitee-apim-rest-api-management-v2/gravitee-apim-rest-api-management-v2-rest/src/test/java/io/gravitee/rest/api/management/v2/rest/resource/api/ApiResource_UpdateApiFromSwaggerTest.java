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
import io.gravitee.apim.core.api.exception.InvalidApiDefinitionException;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.use_case.OAIToUpdateApiUseCase;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ImportSwaggerDescriptor;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class ApiResource_UpdateApiFromSwaggerTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_import/swagger";
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

        Response response = rootTarget().request().put(Entity.json(new ImportSwaggerDescriptor()));

        assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);
    }

    @Test
    void should_return_404_when_api_not_found() {
        when(oaiToUpdateApiUseCase.execute(any())).thenThrow(new ApiNotFoundException(API));

        Response response = rootTarget().request().put(Entity.entity(buildDescriptor("{}"), MediaType.APPLICATION_JSON));

        assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
    }

    @Test
    void should_return_200_and_updated_api_on_success() {
        var existingApi = ApiFixtures.aProxyApiV4().toBuilder().id(API).environmentId(ENVIRONMENT).build();
        var apiWithFlows = new ApiWithFlows(existingApi, java.util.List.of());

        when(oaiToUpdateApiUseCase.execute(any())).thenReturn(new OAIToUpdateApiUseCase.Output(apiWithFlows));
        when(apiSearchServiceV4.findById(GraviteeContext.getExecutionContext(), API)).thenReturn(
            io.gravitee.rest.api.model.v4.api.ApiEntity.builder().id(API).name("Test API").apiVersion("1.0").build()
        );

        Response response = rootTarget().request().put(Entity.entity(buildDescriptor("{}"), MediaType.APPLICATION_JSON));

        assertThat(response.getStatus()).isEqualTo(OK_200);
        var body = response.readEntity(ApiV4.class);
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(API);
    }

    @Test
    void should_return_400_when_swagger_cannot_be_read() {
        when(oaiToUpdateApiUseCase.execute(any())).thenThrow(new InvalidApiDefinitionException("Unable to read the swagger specification"));

        Response response = rootTarget().request().put(Entity.entity(buildDescriptor("{}"), MediaType.APPLICATION_JSON));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_when_invalid_paths_exception() {
        when(oaiToUpdateApiUseCase.execute(any())).thenThrow(new InvalidPathsException("Path [/foo] already exists"));

        Response response = rootTarget().request().put(Entity.entity(buildDescriptor("{}"), MediaType.APPLICATION_JSON));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    private ImportSwaggerDescriptor buildDescriptor(String payload) {
        var descriptor = new ImportSwaggerDescriptor();
        descriptor.setPayload(payload);
        return descriptor;
    }
}
