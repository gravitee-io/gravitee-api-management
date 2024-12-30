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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.ApiFixtures;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

public class ApiResource_StopTest extends ApiResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_stop";
    }

    @Test
    public void should_not_stop_api_with_insufficient_rights() {
        when(permissionService.hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_DEFINITION), eq(API), any()))
            .thenReturn(false);
        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void should_not_stop_api_with_incorrect_if_match() {
        var apiEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).build();
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(apiEntity);

        final Response response = rootTarget().request().header(HttpHeaders.IF_MATCH, "\"000\"").post(Entity.json(""));
        assertEquals(HttpStatusCode.PRECONDITION_FAILED_412, response.getStatus());

        var body = response.readEntity(String.class);
        assert (body.isEmpty());

        verify(apiStateServiceV4, never()).stop(any(), any(), any());
    }

    @Test
    public void should_not_stop_api_if_not_found() {
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API)))
            .thenThrow(new ApiNotFoundException(API));

        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());

        var body = response.readEntity(Error.class);
        assertNotNull(body);

        verify(apiStateServiceV4, never()).stop(any(), any(), any());
    }

    @Test
    public void should_not_stop_api_if_archived() {
        var archivedEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).lifecycleState(ApiLifecycleState.ARCHIVED).build();

        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(archivedEntity);

        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        var body = response.readEntity(Error.class);
        assertNotNull(body);
        assertEquals("Deleted API cannot be stopped", body.getMessage());

        verify(apiStateServiceV4, never()).stop(any(), any(), any());
    }

    @Test
    public void should_not_stop_api_if_already_stopped() {
        var stoppedEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).state(Lifecycle.State.STOPPED).build();

        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API))).thenReturn(stoppedEntity);

        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        var body = response.readEntity(Error.class);
        assertNotNull(body);
        assertEquals("API is already stopped", body.getMessage());

        verify(apiStateServiceV4, never()).stop(any(), any(), any());
    }

    @Test
    public void should_stop_api() {
        var updatedEntity = ApiFixtures.aModelHttpApiV4().toBuilder().id(API).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(updatedEntity);

        when(apiStateServiceV4.stop(eq(GraviteeContext.getExecutionContext()), eq(API), eq(USER_NAME))).thenReturn(updatedEntity);

        final Response response = rootTarget().request().post(Entity.json(""));
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        assertEquals("\"" + updatedEntity.getUpdatedAt().getTime() + "\"", response.getHeaders().get("Etag").get(0));

        verify(apiStateServiceV4, times(1)).stop(eq(GraviteeContext.getExecutionContext()), eq(API), eq(USER_NAME));
    }
}
