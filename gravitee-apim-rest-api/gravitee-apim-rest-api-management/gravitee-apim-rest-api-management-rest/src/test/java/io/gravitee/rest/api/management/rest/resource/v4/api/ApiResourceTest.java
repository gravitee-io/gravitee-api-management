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

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import io.gravitee.definition.model.v4.listener.http.ListenerHttp;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import java.util.Date;
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        listenerHttp.setPaths(List.of(new Path("/test")));
        apiEntity.setListeners(List.of(listenerHttp));
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
    }

    @Test
    public void shouldNotGetApiBecauseNotFound() {
        final Response response = envTarget(UNKNOWN_API).request().get();

        assertEquals(NOT_FOUND_404, response.getStatus());
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
}
