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
package io.gravitee.rest.api.management.rest.resource.search;

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.IdentityService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SearchUsersResourceTest extends AbstractResourceTest {

    @Autowired
    private IdentityService identityService;

    @Override
    protected String contextPath() {
        return "search/users";
    }

    @BeforeEach
    void resetIdentityService() {
        reset(identityService);
        when(identityService.search(anyString())).thenReturn(List.of());
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                any(RolePermission.class),
                anyString(),
                any(RolePermissionAction[].class)
            )
        ).thenReturn(true);
    }

    @Test
    void should_return_403_when_caller_has_no_listed_permission() {
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                any(RolePermission.class),
                anyString(),
                any(RolePermissionAction[].class)
            )
        ).thenReturn(false);

        Response response = envTarget().queryParam("q", "alice").request().get();

        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    void should_return_200_with_environment_api_read() {
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                any(RolePermission.class),
                anyString(),
                any(RolePermissionAction[].class)
            )
        ).thenReturn(false);
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                eq(RolePermission.ENVIRONMENT_API),
                eq(GraviteeContext.getCurrentEnvironment()),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(true);

        when(identityService.search("alice")).thenReturn(List.of());

        Response response = envTarget().queryParam("q", "alice").request().get();

        assertEquals(OK_200, response.getStatus());
    }
}
