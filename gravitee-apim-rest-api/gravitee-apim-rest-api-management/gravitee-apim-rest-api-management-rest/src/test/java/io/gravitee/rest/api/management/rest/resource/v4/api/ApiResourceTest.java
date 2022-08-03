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
package io.gravitee.rest.api.management.rest.resource.v4.api;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static javax.ws.rs.client.Entity.entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.ListenerHttp;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String UNKNOWN_API = "unknown";
    private ApiEntity apiEntity;

    @Override
    protected String contextPath() {
        return "v4/apis/";
    }

    @Before
    public void init() {
        reset(apiServiceV4);
        GraviteeContext.cleanContext();

        apiEntity = new ApiEntity();
        apiEntity.setId(API);
        apiEntity.setName(API);
        ListenerHttp listenerHttp = new ListenerHttp();
        listenerHttp.setPaths(List.of(new Path("my.fake.host", "/test")));
        listenerHttp.setPathMappings(Set.of("/test"));
        apiEntity.setListeners(List.of(listenerHttp));
        apiEntity.setProperties(List.of(new Property()));
        apiEntity.setServices(new ApiServices());
        apiEntity.setResources(List.of(new Resource()));
        apiEntity.setResponseTemplates(Map.of("key", new HashMap<>()));
        apiEntity.setUpdatedAt(new Date());
        doReturn(apiEntity).when(apiServiceV4).findById(GraviteeContext.getExecutionContext(), API);
        doThrow(ApiNotFoundException.class).when(apiServiceV4).findById(GraviteeContext.getExecutionContext(), UNKNOWN_API);
        doThrow(ApiNotFoundException.class).when(apiServiceV4).delete(GraviteeContext.getExecutionContext(), UNKNOWN_API);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldGetApi() {
        final Response response = envTarget(API).request().get();

        assertEquals(OK_200, response.getStatus());

        final ApiEntity responseApi = response.readEntity(ApiEntity.class);
        assertNotNull(responseApi);
        assertEquals(API, responseApi.getName());
        assertNotNull(responseApi.getPictureUrl());
        assertNotNull(responseApi.getBackgroundUrl());
        assertNotNull(responseApi.getProperties());
        assertEquals(1, responseApi.getProperties().size());
        assertNotNull(responseApi.getServices());
        assertNotNull(responseApi.getResources());
        assertEquals(1, responseApi.getResources().size());
        assertNotNull(responseApi.getResponseTemplates());
        assertEquals(1, responseApi.getResponseTemplates().size());
        assertNotNull(responseApi.getListeners());
        assertNotNull(((ListenerHttp) responseApi.getListeners().get(0)).getPathMappings());
        assertNotNull(((ListenerHttp) responseApi.getListeners().get(0)).getPaths().get(0).getHost());
    }

    @Test
    public void shouldGetFilteredApi() {
        doReturn(false)
            .when(permissionService)
            .hasPermission(GraviteeContext.getExecutionContext(), RolePermission.API_DEFINITION, API, RolePermissionAction.READ);

        final Response response = envTarget(API).request().get();

        assertEquals(OK_200, response.getStatus());

        final ApiEntity responseApi = response.readEntity(ApiEntity.class);
        assertNotNull(responseApi);
        assertEquals(API, responseApi.getName());
        assertNull(responseApi.getPictureUrl());
        assertNull(responseApi.getBackgroundUrl());
        assertNotNull(responseApi.getProperties());
        assertEquals(0, responseApi.getProperties().size());
        assertNull(responseApi.getServices());
        assertNotNull(responseApi.getResources());
        assertEquals(0, responseApi.getResources().size());
        assertNotNull(responseApi.getResponseTemplates());
        assertEquals(0, responseApi.getResponseTemplates().size());
        assertNotNull(responseApi.getListeners());
        assertNull(((ListenerHttp) responseApi.getListeners().get(0)).getPathMappings());
        assertNull(((ListenerHttp) responseApi.getListeners().get(0)).getPaths().get(0).getHost());
    }

    @Test
    public void shouldNotGetApiBecauseNotFound() {
        final Response response = envTarget(UNKNOWN_API).request().get();

        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldUpdateApi() {
        UpdateApiEntity updateApiEntity = prepareValidUpdateApiEntity();

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setUpdatedAt(new Date());
        doReturn(updatedApiEntity).when(apiServiceV4).update(GraviteeContext.getExecutionContext(), API, updateApiEntity, true, USER_NAME);

        final Response response = envTarget(API).request().put(Entity.json(updateApiEntity));

        verify(apiServiceV4, times(1)).update(GraviteeContext.getExecutionContext(), API, updateApiEntity, true, USER_NAME);

        assertEquals(OK_200, response.getStatus());

        final ApiEntity responseApi = response.readEntity(ApiEntity.class);
        assertNull(responseApi.getPicture());
        assertNotNull(responseApi.getPictureUrl());
        assertNull(responseApi.getBackground());
        assertNotNull(responseApi.getBackgroundUrl());
    }

    @Test
    public void shouldNotUpdateApiBecauseWrongApiId() {
        UpdateApiEntity updateApiEntity = prepareValidUpdateApiEntity();
        updateApiEntity.setId(UNKNOWN_API);
        final Response response = envTarget(API).request().put(Entity.json(updateApiEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotUpdateApiBecausePictureInvalid() {
        UpdateApiEntity updateApiEntity = prepareValidUpdateApiEntity();
        updateApiEntity.setPicture("not-an-image");

        final Response response = envTarget(API).request().put(entity(updateApiEntity, MediaType.APPLICATION_JSON));
        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldDeleteApi() {
        final Response response = envTarget(UNKNOWN_API).request().delete();

        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotDeleteApiBecauseNotfound() {
        final Response response = envTarget(API).request().delete();

        assertEquals(NO_CONTENT_204, response.getStatus());
    }

    @NotNull
    private UpdateApiEntity prepareValidUpdateApiEntity() {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setId(API);
        updateApiEntity.setApiVersion("v1");
        updateApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        updateApiEntity.setType(ApiType.ASYNC);
        updateApiEntity.setName("api-name");
        updateApiEntity.setDescription("api-description");
        updateApiEntity.setVisibility(Visibility.PUBLIC);

        ListenerHttp listenerHttp = new ListenerHttp();
        listenerHttp.setEntrypoints(List.of(new Entrypoint()));
        listenerHttp.setPaths(List.of(new Path()));
        updateApiEntity.setListeners(List.of(listenerHttp));

        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpointName");
        endpoint.setType("http");
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("endpointGroupName");
        endpointGroup.setType("http");
        endpointGroup.setEndpoints(List.of(endpoint));
        updateApiEntity.setEndpointGroups(List.of(endpointGroup));
        return updateApiEntity;
    }
}
