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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.ListenerHttp;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "v4/apis/";
    }

    @Test
    public void shouldNotCreateApiWithNoContent() {
        final Response response = envTarget().request().post(null);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotCreateApiWithoutName() {
        final NewApiEntity apiEntity = new NewApiEntity();
        apiEntity.setName("");
        apiEntity.setApiVersion("v1");
        apiEntity.setDescription("my description");

        ApiEntity returnedApi = new ApiEntity();
        returnedApi.setId("my-beautiful-api");
        doReturn(returnedApi)
            .when(apiServiceV4)
            .create(eq(GraviteeContext.getExecutionContext()), Mockito.any(NewApiEntity.class), Mockito.eq(USER_NAME));

        final Response response = envTarget().request().post(Entity.json(apiEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotCreateApiWithoutListeners() {
        final NewApiEntity apiEntity = new NewApiEntity();
        apiEntity.setName("My beautiful api");
        apiEntity.setApiVersion("v1");
        apiEntity.setDescription("my description");

        ApiEntity returnedApi = new ApiEntity();
        returnedApi.setId("my-beautiful-api");
        doReturn(returnedApi)
            .when(apiServiceV4)
            .create(eq(GraviteeContext.getExecutionContext()), Mockito.any(NewApiEntity.class), Mockito.eq(USER_NAME));

        final Response response = envTarget().request().post(Entity.json(apiEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotCreateApiWithoutEndpoints() {
        final NewApiEntity apiEntity = new NewApiEntity();
        apiEntity.setName("My beautiful api");
        apiEntity.setApiVersion("v1");
        apiEntity.setDescription("my description");
        ListenerHttp listenerHttp = new ListenerHttp();
        listenerHttp.setPaths(List.of(new Path("/context")));
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("sse");
        listenerHttp.setEntrypoints(List.of(entrypoint));
        apiEntity.setListeners(List.of(listenerHttp));

        ApiEntity returnedApi = new ApiEntity();
        returnedApi.setId("my-beautiful-api");
        doReturn(returnedApi)
            .when(apiServiceV4)
            .create(eq(GraviteeContext.getExecutionContext()), Mockito.any(NewApiEntity.class), Mockito.eq(USER_NAME));

        final Response response = envTarget().request().post(Entity.json(apiEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotCreateApiWhenServiceThrowException() {
        final NewApiEntity apiEntity = new NewApiEntity();
        apiEntity.setName("My beautiful api");
        apiEntity.setApiVersion("v1");
        apiEntity.setDescription("my description");
        ListenerHttp listenerHttp = new ListenerHttp();
        listenerHttp.setPaths(List.of(new Path("/context")));
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("sse");
        listenerHttp.setEntrypoints(List.of(entrypoint));
        apiEntity.setListeners(List.of(listenerHttp));

        EndpointGroup endpoint = new EndpointGroup();
        endpoint.setName("default");
        endpoint.setType("http");
        apiEntity.setEndpointGroups(List.of(endpoint));

        when(apiServiceV4.create(eq(GraviteeContext.getExecutionContext()), Mockito.any(NewApiEntity.class), Mockito.eq(USER_NAME)))
            .thenThrow(new TechnicalManagementException());
        final Response response = envTarget().request().post(Entity.json(apiEntity));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldCreateApi() {
        final NewApiEntity apiEntity = new NewApiEntity();
        apiEntity.setName("My beautiful api");
        apiEntity.setApiVersion("v1");
        apiEntity.setDescription("my description");
        apiEntity.setApiVersion("v1");
        apiEntity.setType(ApiType.SYNC);
        apiEntity.setDescription("Ma description");
        ListenerHttp listenerHttp = new ListenerHttp();
        listenerHttp.setPaths(List.of(new Path("/context")));
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("sse");
        listenerHttp.setEntrypoints(List.of(entrypoint));
        apiEntity.setListeners(List.of(listenerHttp));

        EndpointGroup endpoint = new EndpointGroup();
        endpoint.setName("default");
        endpoint.setType("http");
        apiEntity.setEndpointGroups(List.of(endpoint));

        ApiEntity returnedApi = new ApiEntity();
        returnedApi.setId("my-beautiful-api");
        doReturn(returnedApi)
            .when(apiServiceV4)
            .create(eq(GraviteeContext.getExecutionContext()), Mockito.any(NewApiEntity.class), Mockito.eq(USER_NAME));

        final Response response = envTarget().request().post(Entity.json(apiEntity));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(envTarget().path("my-beautiful-api").getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }
}
