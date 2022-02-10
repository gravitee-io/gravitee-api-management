/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.NewApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Date;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApisResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "apis";
    }

    @Test
    public void shouldNotCreateApi_noContent() {
        final Response response = envTarget().request().post(null);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotCreateApi_emptyName() {
        final NewApiEntity apiEntity = new NewApiEntity();
        apiEntity.setName("");
        apiEntity.setVersion("v1");
        apiEntity.setDescription("my description");

        ApiEntity returnedApi = new ApiEntity();
        returnedApi.setId("my-beautiful-api");
        doReturn(returnedApi).when(apiService).create(Mockito.any(NewApiEntity.class), Mockito.eq(USER_NAME));

        final Response response = envTarget().request().post(Entity.json(apiEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotCreateApi_withoutPath() {
        final NewApiEntity apiEntity = new NewApiEntity();
        apiEntity.setName("My beautiful api");
        apiEntity.setVersion("v1");
        apiEntity.setDescription("my description");

        ApiEntity returnedApi = new ApiEntity();
        returnedApi.setId("my-beautiful-api");
        doReturn(returnedApi).when(apiService).create(Mockito.any(NewApiEntity.class), Mockito.eq(USER_NAME));

        final Response response = envTarget().request().post(Entity.json(apiEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldCreateApi() {
        final NewApiEntity apiEntity = new NewApiEntity();
        apiEntity.setName("My beautiful api");
        apiEntity.setVersion("v1");
        apiEntity.setDescription("my description");
        apiEntity.setContextPath("/myapi");
        apiEntity.setEndpoint("http://localhost:9099/");

        ApiEntity returnedApi = new ApiEntity();
        returnedApi.setId("my-beautiful-api");
        doReturn(returnedApi).when(apiService).create(Mockito.any(NewApiEntity.class), Mockito.eq(USER_NAME));

        final Response response = envTarget().request().post(Entity.json(apiEntity));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(envTarget().path("my-beautiful-api").getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    public void shouldImportApiFromSwager() {
        reset(apiService, swaggerService);
        ImportSwaggerDescriptorEntity swaggerDescriptor = new ImportSwaggerDescriptorEntity();
        swaggerDescriptor.setPayload("my-payload");

        ApiEntity createdApi = new ApiEntity();
        createdApi.setId("my-beautiful-api");
        doReturn(createdApi).when(apiService).createFromSwagger(any(), any(), any());

        final Response response = envTarget().path("import").path("swagger").request().post(Entity.json(swaggerDescriptor));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(envTarget().path("my-beautiful-api").getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));

        verify(swaggerService)
            .createAPI(
                argThat(argument -> argument.getPayload().equalsIgnoreCase(swaggerDescriptor.getPayload())),
                eq(DefinitionVersion.valueOfLabel("1.0.0"))
            );
    }

    @Test
    public void shouldImportApiFromGraviteeIODefinitionV1() {
        reset(apiService, swaggerService);
        String apiDefinition = "{}";

        ApiEntity createdApi = new ApiEntity();
        createdApi.setGraviteeDefinitionVersion("1.0.0");
        createdApi.setId("my-beautiful-api");
        doReturn(createdApi)
            .when(apiDuplicatorService)
            .createWithImportedDefinition(any(), eq(GraviteeContext.getCurrentOrganization()), eq(GraviteeContext.getCurrentEnvironment()));

        final Response response = envTarget().path("import").request().post(Entity.json(apiDefinition));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        verify(apiService, times(0)).migrate(any());
    }

    @Test
    public void shouldImportApiWithoutMigrationFromGraviteeIODefinitionIfAlreadyV2() {
        reset(apiService, swaggerService);
        String apiDefinition = "{}";

        ApiEntity createdApi = new ApiEntity();
        createdApi.setGraviteeDefinitionVersion("2.0.0");
        createdApi.setId("my-beautiful-api");
        doReturn(createdApi)
            .when(apiDuplicatorService)
            .createWithImportedDefinition(any(), eq(GraviteeContext.getCurrentOrganization()), eq(GraviteeContext.getCurrentEnvironment()));

        final Response response = envTarget()
            .path("import")
            .queryParam("definitionVersion", "2.0.0")
            .request()
            .post(Entity.json(apiDefinition));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        verify(apiService, times(0)).migrate(any());
    }

    @Test
    public void shouldImportApiWithMigrationFromGraviteeIODefinitionIfAlreadyV2() {
        reset(apiService, swaggerService);
        String apiDefinition = "{}";

        ApiEntity createdApi = new ApiEntity();
        createdApi.setGraviteeDefinitionVersion("1.0.0");
        createdApi.setId("my-beautiful-api");
        doReturn(createdApi)
            .when(apiDuplicatorService)
            .createWithImportedDefinition(any(), eq(GraviteeContext.getCurrentOrganization()), eq(GraviteeContext.getCurrentEnvironment()));

        final Response response = envTarget()
            .path("import")
            .queryParam("definitionVersion", "2.0.0")
            .request()
            .post(Entity.json(apiDefinition));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        verify(apiService, times(1)).migrate(any());
    }

    @Test
    public void put_import_shouldImportApi_calling_apiDuplicator() {
        reset(apiDuplicatorService);
        String apiDefinition = "{ \"content\": \"this-is-a-test-api-definition\"}";

        ApiEntity updatedApi = new ApiEntity();
        updatedApi.setId("my-api-id");
        updatedApi.setUpdatedAt(new Date());

        doReturn(updatedApi)
            .when(apiDuplicatorService)
            .updateWithImportedDefinition(
                eq(apiDefinition),
                eq(GraviteeContext.getCurrentOrganization()),
                eq(GraviteeContext.getCurrentEnvironment())
            );

        final Response response = envTarget().path("import").request().put(Entity.json(apiDefinition));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertEquals(updatedApi, response.readEntity(ApiEntity.class));
    }
}
