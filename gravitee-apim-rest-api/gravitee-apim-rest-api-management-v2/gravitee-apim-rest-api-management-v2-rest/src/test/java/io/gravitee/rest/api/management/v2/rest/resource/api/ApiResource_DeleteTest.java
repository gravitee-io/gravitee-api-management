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

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

public class ApiResource_DeleteTest extends ApiResourceTest {

    private static final String UNKNOWN_API = "unknown";

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @Test
    public void shouldDeleteApi() {
        doNothing().when(apiServiceV4).delete(GraviteeContext.getExecutionContext(), API, false);
        final Response response = rootTarget(API).request().delete();

        assertEquals(NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void shouldNotDeleteApiBecauseNotfound() {
        doThrow(ApiNotFoundException.class).when(apiServiceV4).delete(GraviteeContext.getExecutionContext(), UNKNOWN_API, false);

        final Response response = rootTarget(UNKNOWN_API).request().delete();
        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotDeleteApiWithInsufficientRights() {
        when(permissionService.hasPermission(eq(GraviteeContext.getExecutionContext()), eq(RolePermission.API_DEFINITION), eq(API), any()))
            .thenReturn(false);
        final Response response = rootTarget(API).request().delete();
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }
}
